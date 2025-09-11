package edu.cnu.swacademy.security.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Order 엔티티를 위한 JPA Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

  /**
   * 사용자의 미체결 주문 조회 (unfilledQuantity > 0)
   * 
   * @param userId 사용자 ID
   * @param pageable 페이징 정보
   * @return 미체결 주문 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.unfilledAmount > 0")
  Page<Order> findUnfilledOrdersByUserId(int userId, Pageable pageable);

  /**
   * 사용자의 미체결 주문 조회 (종목 필터링)
   * 
   * @param userId 사용자 ID
   * @param stockId 종목 ID
   * @param pageable 페이징 정보
   * @return 미체결 주문 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.stock.id = :stockId AND o.unfilledAmount > 0")
  Page<Order> findUnfilledOrdersByUserIdAndStockId(int userId, int stockId, Pageable pageable);

  /**
   * 사용자의 미체결 주문 조회 (방향 필터링)
   * 
   * @param userId 사용자 ID
   * @param side 주문 방향
   * @param pageable 페이징 정보
   * @return 미체결 주문 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.side = :side AND o.unfilledAmount > 0")
  Page<Order> findUnfilledOrdersByUserIdAndSide(int userId, OrderSide side, Pageable pageable);

  /**
   * 사용자의 미체결 주문 조회 (종목 + 방향 필터링)
   * 
   * @param userId 사용자 ID
   * @param stockId 종목 ID
   * @param side 주문 방향
   * @param pageable 페이징 정보
   * @return 미체결 주문 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.stock.id = :stockId AND o.side = :side AND o.unfilledAmount > 0")
  Page<Order> findUnfilledOrdersByUserIdAndStockIdAndSide(int userId, int stockId, OrderSide side, Pageable pageable);

  /**
   * 사용자의 체결 내역 조회 (전체)
   *
   * @param userId 사용자 ID
   * @param pageable 페이징 정보
   * @return 체결 내역 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND (o.amount != o.unfilledAmount OR o.canceledAmount != 0)")
  Page<Order> findMatchedByUserId(int userId, Pageable pageable);

  /**
   * 사용자의 체결 내역 조회 (종목 필터링)
   *
   * @param userId 사용자 ID
   * @param stockId 종목 ID
   * @param pageable 페이징 정보
   * @return 체결 내역 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.stock.id = :stockId AND (o.amount != o.unfilledAmount OR o.canceledAmount != 0)")
  Page<Order> findMatchedByUserIdAndStockId(int userId, int stockId, Pageable pageable);

  /**
   * 사용자의 체결 내역 조회 (방향 필터링)
   *
   * @param userId 사용자 ID
   * @param side 주문 방향
   * @param pageable 페이징 정보
   * @return 체결 내역 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.side = :side AND (o.amount != o.unfilledAmount OR o.canceledAmount != 0)")
  Page<Order> findMatchedByUserIdAndSide(int userId, OrderSide side, Pageable pageable);

  /**
   * 사용자의 체결 내역 조회 (종목 + 방향 필터링)
   *
   * @param userId 사용자 ID
   * @param stockId 종목 ID
   * @param side 주문 방향
   * @param pageable 페이징 정보
   * @return 체결 내역 페이지
   */
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.stock.id = :stockId AND o.side = :side AND (o.amount != o.unfilledAmount OR o.canceledAmount != 0)")
  Page<Order> findMatchedByUserIdAndStockIdAndSide(int userId, int stockId, OrderSide side, Pageable pageable);
}