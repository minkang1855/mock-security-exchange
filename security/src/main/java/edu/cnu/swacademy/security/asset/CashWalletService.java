package edu.cnu.swacademy.security.asset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.asset.dto.CashDepositRequest;
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

    log.info("Cash deposit completed: userid(={}), amount(={}), new-balance(={})",
        userId, request.amount(), cashWallet.getReserve());
  }
}
