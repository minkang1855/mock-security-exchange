package edu.cnu.swacademy.security.asset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.common.AccountNumberGenerator;
import edu.cnu.swacademy.security.common.AesUtil;
import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
@Service
public class CashWalletService {

  private static final int MAX_RETRY = 3;

  private final CashWalletRepository cashWalletRepository;
  private final UserRepository userRepository;
  private final AesUtil aesUtil;

  @Transactional
  public void createCashWallet(Long userId) throws SecurityException {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new SecurityException(ErrorCode.USER_NOT_FOUND));
    log.info("Got user id (={})", userId);

    if (cashWalletRepository.existsByUserId(userId)) {
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
      log.warn("Account number collision, attempt: {}", attempt);
    }
    
    log.error("Failed to generate unique account number after {} attempts", MAX_RETRY);
    throw new SecurityException(ErrorCode.ACCOUNT_NUMBER_GENERATION_FAILED);
  }
}
