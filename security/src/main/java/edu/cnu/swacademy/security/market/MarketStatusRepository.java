package edu.cnu.swacademy.security.market;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketStatusRepository extends JpaRepository<MarketStatus, Long> {

  List<MarketStatus> findByStockId(Long stockId);

  List<MarketStatus> findByStockIdOrderByTradingDateDesc(Long stockId);

  Optional<MarketStatus> findByStockIdAndTradingDate(Long stockId, LocalDate tradingDate);

  List<MarketStatus> findByTradingDate(LocalDate tradingDate);
}
