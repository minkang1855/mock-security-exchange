package edu.cnu.swacademy.security.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

  List<Match> findByStockId(Long stockId);

  List<Match> findByMakerOrderId(Long makerOrderId);

  List<Match> findByTakerOrderId(Long takerOrderId);
}
