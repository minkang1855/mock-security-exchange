package edu.cnu.swacademy.security.stock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StockWalletRepository extends JpaRepository<StockWallet, Integer> {

  Optional<StockWallet> findByUserIdAndStockId(int userId, int stockId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT sw FROM StockWallet sw WHERE sw.user.id = :userId AND sw.stock.id = :stockId")
  Optional<StockWallet> findByUserIdAndStockIdWithLock(int userId, int stockId);

  boolean existsByUserIdAndStockId(int userId, int stockId);
}
