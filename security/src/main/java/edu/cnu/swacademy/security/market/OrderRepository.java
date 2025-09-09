package edu.cnu.swacademy.security.market;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findByUserId(Long userId);

  List<Order> findByStockId(Long stockId);

  List<Order> findByUserIdAndStockId(Long userId, Long stockId);

  @Query("SELECT o FROM Order o WHERE o.stock.id = :stockId AND o.side = :side AND o.unfilledAmount > 0 ORDER BY o.price ASC")
  List<Order> findActiveBuyOrdersByStockOrderByPriceAsc(@Param("stockId") Long stockId, @Param("side") String side);

  @Query("SELECT o FROM Order o WHERE o.stock.id = :stockId AND o.side = :side AND o.unfilledAmount > 0 ORDER BY o.price DESC")
  List<Order> findActiveSellOrdersByStockOrderByPriceDesc(@Param("stockId") Long stockId, @Param("side") String side);
}
