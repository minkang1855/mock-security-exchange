package edu.cnu.swacademy.security.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findByUserId(Long userId);

  List<Order> findByStockId(Long stockId);

  List<Order> findByUserIdAndStockId(Long userId, Long stockId);
}
