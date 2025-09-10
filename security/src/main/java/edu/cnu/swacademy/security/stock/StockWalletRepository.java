package edu.cnu.swacademy.security.stock;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockWalletRepository extends JpaRepository<StockWallet, Integer> {

  List<StockWallet> findByUserId(int userId);

  Optional<StockWallet> findByUserIdAndStockId(int userId, int stockId);

  List<StockWallet> findByStockId(int stockId);

  boolean existsByUserIdAndStockId(int userId, int stockId);
}
