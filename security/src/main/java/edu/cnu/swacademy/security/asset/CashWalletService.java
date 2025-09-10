package edu.cnu.swacademy.security.asset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.asset.dto.CashDepositRequest;
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
  public void createCashWallet(Long userId) throws SecurityException {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.info("User not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.USER_NOT_FOUND);
        });
    log.info("Got user id (={})", userId);

    if (cashWalletRepository.existsByUserId(userId)) {
      log.info("Cash wallet already exists for user-id(={})", userId);
      throw new SecurityException(ErrorCode.WALLET_ALREADY_EXISTS);
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
  public void deposit(Long userId, CashDepositRequest request) throws SecurityException {
    // 1. 사용자의 현금 계좌 조회 (비관적 락 적용)
    CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not fou nd for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 지갑 정지 상태 확인
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 3. 입금 처리
    cashWallet.deposit(request.amount());

    // 4. 거래 내역 생성
    CashWalletHistory history = new CashWalletHistory(
        cashWallet,
        TransactionType.DEPOSIT,
        request.amount(),
        "현금 입금",
        cashWallet.getDeposit()
    );
    cashWalletHistoryRepository.save(history);

    log.info("Cash deposit completed: user-id(={}), amount(={}), new-balance(={})",
        userId, request.amount(), cashWallet.getReserve());
  }

  /**
   * 현금 출금 처리
   * 사용자가 요청한 금액만큼 현금 지갑의 예치금을 차감하고 출금 내역을 기록합니다.
   * 동시성 이슈를 방지하기 위해 비관적 락을 사용하여 계좌를 조회합니다.
   * 
   * @param userId 사용자 ID (JWT에서 추출된 값)
   * @param request 출금 요청 정보 (출금 금액)
   * @throws SecurityException 출금 실패 시 발생 (계좌 없음, 정지 상태, 잔액 부족 등)
   */
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
  public void withdrawal(Long userId, CashWithdrawalRequest request) throws SecurityException {
    // 1. 사용자의 현금 계좌 조회 (비관적 락 적용으로 동시성 이슈 방지)
    CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 2. 지갑 정지 상태 확인 (정지된 계좌는 출금 불가)
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 3. 출금 가능 금액 확인 (예치금 - 대기중인 매수 주문으로 묶인 금액)
    int withdrawalAmount = request.amount();
    long availableBalance = cashWallet.getReserve() - cashWallet.getDeposit(); // 현재는 대기중인 주문이 없으므로 예치금이 출금 가능 금액
    
    if (availableBalance < withdrawalAmount) {
      log.info("Insufficient balance for user-id(={}), requested-amount(={}), available-balance(={})", 
          userId, withdrawalAmount, availableBalance);
      throw new SecurityException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    // 4. 출금 처리 (예치금 차감)
    cashWallet.withdrawal(withdrawalAmount);

    // 5. 출금 내역 생성 (거래 내역 기록)
    CashWalletHistory history = new CashWalletHistory(
        cashWallet,
        TransactionType.WITHDRAWAL,
        withdrawalAmount,
        "현금 출금",
        cashWallet.getReserve()
    );
    cashWalletHistoryRepository.save(history);

    log.info("Cash withdrawal completed: user-id(={}), amount(={}), new-balance(={})",
        userId, withdrawalAmount, cashWallet.getReserve());
  }
}
