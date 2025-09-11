package edu.cnu.swacademy.security.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * MarketStatus 엔티티를 위한 JPA Repository
 */
@Repository
public interface MarketStatusRepository extends JpaRepository<MarketStatus, Integer> {

  Optional<MarketStatus> findByStockIdAndTradingDate(int stockId, LocalDate tradingDate);

  boolean existsByStockIdAndTradingDate(int stockId, LocalDate tradingDate);
}