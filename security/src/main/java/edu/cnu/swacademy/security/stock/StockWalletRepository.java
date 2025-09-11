package edu.cnu.swacademy.security.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockWalletRepository extends JpaRepository<StockWallet, Integer> {

  Optional<StockWallet> findByUserIdAndStockId(int userId, int stockId);

  boolean existsByUserIdAndStockId(int userId, int stockId);
}
