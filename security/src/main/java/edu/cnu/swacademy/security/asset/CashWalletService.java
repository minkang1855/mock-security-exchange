package edu.cnu.swacademy.security.asset;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.asset.dto.CashBalanceResponse;
import edu.cnu.swacademy.security.asset.dto.CashDepositRequest;
import edu.cnu.swacademy.security.asset.dto.CashWalletHistoriesResponse;
import edu.cnu.swacademy.security.asset.dto.CashWalletHistoryResponse;
import edu.cnu.swacademy.security.asset.dto.CashWithdrawalRequest;
import edu.cnu.swacademy.security.common.AccountNumberGenerator;
import edu.cnu.swacademy.security.common.AesUtil;
import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class CashWalletService {

  private static final int MAX_RETRY = 3;

  private final CashWalletRepository cashWalletRepository;
  private final CashWalletHistoryRepository cashWalletHistoryRepository;
  private final UserRepository userRepository;
  private final AesUtil aesUtil;

  @Transactional(rollbackFor = Exception.class)
  public void createCashWallet(int userId) throws SecurityException {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.info("User not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.USER_NOT_FOUND);
        });
    log.info("Got user id (={})", userId);

    if (cashWalletRepository.existsByUserId(userId)) {
      log.info("Cash wallet already exists for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_ALREADY_EXISTS);
    }
    
    String uniqueAccountNumber = generateUniqueAccountNumber();

    cashWalletRepository.save(
        new CashWallet(
            user,
            aesUtil.encrypt(uniqueAccountNumber)
        )
    );
  }

  private String generateUniqueAccountNumber() throws SecurityException {
    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      String accountNumber = AccountNumberGenerator.generateAccountNumber();
      if (!cashWalletRepository.existsByAccountNumber(aesUtil.encrypt(accountNumber))) {
        return accountNumber;
      }
      log.warn("Account number collision, attempt(={})", attempt);
    }
    
    log.error("Failed to generate unique account number after {} attempts", MAX_RETRY);
    throw new SecurityException(ErrorCode.ACCOUNT_NUMBER_GENERATION_FAILED);
  }

  @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
  public void deposit(int userId, CashDepositRequest request) throws SecurityException {
    // 1. 사용자의 현금 계좌 조회 (비관적 락 적용)
    CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not fou nd for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 현금 계좌 정지 상태 확인
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 3. 입금 처리
    cashWallet.deposit(request.amount());

    // 4. 거래 내역 생성
    CashWalletHistory history = new CashWalletHistory(
        cashWallet,
        CashWalletTransactionType.DEPOSIT,
        request.amount(),
        "현금 입금",
        cashWallet.getReserve()
    );
    cashWalletHistoryRepository.save(history);

    log.info("Cash deposit completed: user-id(={}), cash-wallet-id(={})", userId, cashWallet.getId());
  }

  /**
   * 현금 출금 처리
   * 사용자가 요청한 금액만큼 현금 계좌의 예치금을 차감하고 출금 내역을 기록합니다.
   * 동시성 이슈를 방지하기 위해 비관적 락을 사용하여 계좌를 조회합니다.
   * 
   * @param userId 사용자 ID (JWT에서 추출된 값)
   * @param request 출금 요청 정보 (출금 금액)
   * @throws SecurityException 출금 실패 시 발생 (계좌 없음, 정지 상태, 잔액 부족 등)
   */
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
  public void withdrawal(int userId, CashWithdrawalRequest request) throws SecurityException {
    // 1. 사용자의 현금 계좌 조회 (비관적 락 적용으로 동시성 이슈 방지)
    CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 현금 계좌 정지 상태 확인 (정지된 계좌는 출금 불가)
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 3. 출금 가능 금액 확인 (예치금 - 대기중인 매수 주문으로 묶인 금액)
    int withdrawalAmount = request.amount();
    long availableBalance = cashWallet.getReserve() - cashWallet.getDeposit(); // 현재는 대기중인 주문이 없으므로 예치금이 출금 가능 금액
    
    if (availableBalance < withdrawalAmount) {
      log.error("Insufficient balance for cash-wallet-id(={}),", cashWallet.getId());
      throw new SecurityException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    // 4. 출금 처리 (예치금 차감)
    cashWallet.withdrawal(withdrawalAmount);

    // 5. 출금 내역 생성 (거래 내역 기록)
    CashWalletHistory history = new CashWalletHistory(
        cashWallet,
        CashWalletTransactionType.WITHDRAWAL,
        withdrawalAmount,
        "현금 출금",
        cashWallet.getReserve()
    );
    cashWalletHistoryRepository.save(history);

    log.info("Cash withdrawal completed: user-id(={}), cash-wallet-id(={})", userId, cashWallet.getId());
  }

  /**
   * 현금 잔액 조회
   * 사용자의 현금 계좌 상태를 확인하여 예치금, 대기중인 매수 주문으로 묶인 금액,
   * 그리고 실제 출금 가능 금액을 조회합니다.
   * 
   * @param userId 사용자 ID (JWT에서 추출된 값)
   * @return 현금 잔액 정보 (현금 계좌 ID, 예치금, 묶인 금액, 출금 가능 금액)
   * @throws SecurityException 조회 실패 시 발생 (계좌 없음, 정지 상태 등)
   */
  public CashBalanceResponse getBalance(int userId) throws SecurityException {
    // 1. 사용자의 현금 계좌 조회
    CashWallet cashWallet = cashWalletRepository.findByUserId(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 계좌 정지 상태 확인 (정지된 계좌는 조회 불가)
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for id(={})", cashWallet.getId());
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 3. 잔액 정보 계산
    int cashWalletId = cashWallet.getId();
    int savings = cashWallet.getReserve(); // 예치금
    int tiedSavings = cashWallet.getDeposit(); // 대기중인 매수 주문으로 묶인 금액
    int available = savings - tiedSavings; // 출금 가능 금액 (예치금 - 묶인 금액)

    log.info("Cash balance retrieved: user-id(={}), cash-wallet-id(={})",userId, cashWalletId);

    return new CashBalanceResponse(cashWalletId, savings, tiedSavings, available);
  }

  /**
   * 현금 입출금 내역 조회
   * 사용자의 현금 계좌 내역을 페이지 단위로 조회합니다.
   * 
   * @param userId 사용자 ID (JWT에서 추출된 값)
   * @param pageable 페이지네이션 정보 (기본값: size=10, page=0, sort=createdAt DESC)
   * @return 현금 입출금 내역 목록 (페이지 정보 포함)
   * @throws SecurityException 조회 실패 시 발생 (계좌 정지 상태)
   */
  public CashWalletHistoriesResponse getHistories(int userId, Pageable pageable) throws SecurityException {
    // 1. 현금 계좌 내역 조회 (입금/출금만 조회, 정지되지 않은 계좌만)
    List<CashWalletTransactionType> depositAndWithdrawalTypes = Arrays.asList(CashWalletTransactionType.DEPOSIT, CashWalletTransactionType.WITHDRAWAL);
    Page<CashWalletHistory> historyPage = cashWalletHistoryRepository.findByUserIdAndTxTypeInAndWalletNotBlocked(
        userId, depositAndWithdrawalTypes, pageable);

    // 4. 응답 DTO 변환
    List<CashWalletHistoryResponse> historyResponses = historyPage.getContent().stream()
        .map(history -> new CashWalletHistoryResponse(
            history.getId(),
            history.getTxType().getDescription(), // TransactionType enum의 description 사용
            history.getTxAmount(),
            history.getTxNote(),
            history.getReserve(),
            history.getCreatedAt() // LocalDateTime을 직접 전달하여 DTO 내부에서 포맷팅
        ))
        .toList();

    log.info("Cash wallet histories retrieved: user-id(={}), total-elements(={}), page(={}), size(={}), sort(={})",
        userId, historyPage.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

    return new CashWalletHistoriesResponse(
        (int) historyPage.getTotalElements(),
        historyResponses
    );
  }

  /**
   * 현금 계좌 정지
   * 사용자의 현금 계좌을 정지하여 모든 현금 관련 거래를 차단합니다.
   * 
   * @param cashWalletId 현금 계좌 ID (PathVariable로 받은 값)
   * @throws SecurityException 정지 실패 시 발생 (현금 계좌 없음, 이미 정지됨)
   */
  @Transactional(rollbackFor = Exception.class)
  public void blockCashWallet(int cashWalletId) throws SecurityException {
    // 1. 사용자의 현금 계좌 조회
    CashWallet cashWallet = cashWalletRepository.findById(cashWalletId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for id(={})", cashWalletId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 이미 정지된 계좌인지 확인
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is already blocked for id(={})", cashWalletId);
      throw new SecurityException(ErrorCode.CASH_WALLET_ALREADY_BLOCKED);
    }

    // 3. 계좌 정지 처리
    cashWallet.block();

    // 4. 계좌 정지 내역 생성 (변경 내역 기록)
    CashWalletHistory history = new CashWalletHistory(
        cashWallet,
        CashWalletTransactionType.ACCOUNT_BLOCKED,
        0, // 정지 시에는 금액 변동 없음
        "계좌 정지",
        cashWallet.getReserve()
    );
    cashWalletHistoryRepository.save(history);

    log.info("Cash wallet blocked successfully: cash-wallet-id(={})", cashWallet.getId());
  }

  /**
   * 현금 계좌 정지 해제
   * 정지 상태로 잠겨 있던 현금 계좌를 정상 상태로 되돌려, 다시 입금/출금 및 주문 접수가 가능하도록 허용합니다.
   * 
   * @param cashWalletId 현금 계좌 ID
   * @throws SecurityException 해제 실패 시 발생 (계좌 없음, 이미 해제됨 등)
   */
  @Transactional(rollbackFor = Exception.class)
  public void unblockCashWallet(int cashWalletId) throws SecurityException {
    // 1. 현금 계좌 조회
    CashWallet cashWallet = cashWalletRepository.findById(cashWalletId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for id(={})", cashWalletId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 이미 정지 해제된 계좌인지 확인
    if (!cashWallet.isBlocked()) {
      log.info("Cash wallet is already unblocked for id(={})", cashWalletId);
      throw new SecurityException(ErrorCode.CASH_WALLET_ALREADY_UNBLOCKED);
    }

    // 3. 계좌 정지 해제 처리
    cashWallet.unblock();

    // 4. 계좌 정지 해제 내역 생성 (변경 내역 기록)
    CashWalletHistory history = new CashWalletHistory(
        cashWallet,
        CashWalletTransactionType.ACCOUNT_UNBLOCKED,
        0, // 해제 시에는 금액 변동 없음
        "계좌 정지 해제",
        cashWallet.getReserve()
    );
    cashWalletHistoryRepository.save(history);

    log.info("Cash wallet unblocked successfully: cash-wallet-id(={})", cashWallet.getId());
  }
}
