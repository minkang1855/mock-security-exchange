package edu.cnu.swacademy.security.asset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CashWalletRepository extends JpaRepository<CashWallet, Long> {

  Optional<CashWallet> findByAccountNumber(String accountNumber);

  Optional<CashWallet> findByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT cw FROM CashWallet cw WHERE cw.user.id = :userId")
  Optional<CashWallet> findByUserIdWithLock(Long userId);

  boolean existsByAccountNumber(String accountNumber);
  
  boolean existsByUserId(Long userId);
}
