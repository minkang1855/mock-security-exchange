package edu.cnu.swacademy.security.asset;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashWalletRepository extends JpaRepository<CashWallet, Long> {

  Optional<CashWallet> findByAccountNumber(String accountNumber);

  List<CashWallet> findByUserId(Long userId);

  Optional<CashWallet> findByUserIdAndAccountNumber(Long userId, String accountNumber);

  boolean existsByAccountNumber(String accountNumber);
}
