package edu.cnu.swacademy.security.stock;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockWalletRepository extends JpaRepository<StockWallet, Long> {

  List<StockWallet> findByUserId(Long userId);

  Optional<StockWallet> findByUserIdAndStockId(Long userId, Long stockId);

  List<StockWallet> findByStockId(Long stockId);
}
