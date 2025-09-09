package edu.cnu.swacademy.security.stock;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockWalletHistoryRepository extends JpaRepository<StockWalletHistory, Long> {

  List<StockWalletHistory> findByStockWalletId(Long stockWalletId);
}
