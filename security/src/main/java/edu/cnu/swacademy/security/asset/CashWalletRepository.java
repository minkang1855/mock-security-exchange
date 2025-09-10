package edu.cnu.swacademy.security.asset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CashWalletRepository extends JpaRepository<CashWallet, Long> {

  Optional<CashWallet> findByAccountNumber(String accountNumber);

  Optional<CashWallet> findByUserId(Long userId);  

  boolean existsByAccountNumber(String accountNumber);
  
  boolean existsByUserId(Long userId);
}
