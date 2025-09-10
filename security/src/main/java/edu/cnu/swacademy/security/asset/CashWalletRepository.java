package edu.cnu.swacademy.security.asset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface CashWalletRepository extends JpaRepository<CashWallet, Integer> {

  Optional<CashWallet> findByAccountNumber(String accountNumber);

  Optional<CashWallet> findByUserId(int userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT cw FROM CashWallet cw WHERE cw.user.id = :userId")
  Optional<CashWallet> findByUserIdWithLock(int userId);

  boolean existsByAccountNumber(String accountNumber);
  
  boolean existsByUserId(int userId);
}
