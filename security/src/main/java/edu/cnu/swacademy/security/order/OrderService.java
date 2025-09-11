package edu.cnu.swacademy.security.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.cnu.swacademy.security.order.dto.UnfilledOrderResponse;
import edu.cnu.swacademy.security.order.dto.UnfilledOrdersResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;

  /**
   * 사용자의 미체결 주문 목록을 조회합니다.
   * 
   * @param userId 사용자 ID
   * @param stockId 종목 ID (선택사항)
   * @param side 주문 방향 (선택사항)
   * @param pageable 페이징 정보
   * @return 미체결 주문 목록
   */
  public UnfilledOrdersResponse getUnfilledOrders(int userId, Integer stockId, String side, Pageable pageable) {
    // 조건에 따른 조회
    Page<Order> orderPage = getOrdersByConditions(userId, stockId, side, pageable);

    // DTO 변환
    List<UnfilledOrderResponse> orderResponses = orderPage.getContent().stream()
        .map(this::convertToUnfilledOrderResponse)
        .toList();

    log.info("Found {} unfilled orders for user-id(={})", orderPage.getTotalElements(), userId);
    return new UnfilledOrdersResponse(orderPage.getTotalElements(), orderResponses);
  }

  /**
   * 조건에 따른 주문 조회
   * 
   * @param userId 사용자 ID
   * @param stockId 종목 ID
   * @param side 주문 방향
   * @param pageable 페이징 정보
   * @return 주문 페이지
   */
  private Page<Order> getOrdersByConditions(int userId, Integer stockId, String side, Pageable pageable) {
    if (stockId != null && side != null) {
      // 종목 + 방향 필터링
      OrderSide orderSide = OrderSide.valueOf(side.toUpperCase());
      return orderRepository.findUnfilledOrdersByUserIdAndStockIdAndSide(userId, stockId, orderSide, pageable);
    } else if (stockId != null) {
      // 종목 필터링만
      return orderRepository.findUnfilledOrdersByUserIdAndStockId(userId, stockId, pageable);
    } else if (side != null) {
      // 방향 필터링만
      OrderSide orderSide = OrderSide.valueOf(side.toUpperCase());
      return orderRepository.findUnfilledOrdersByUserIdAndSide(userId, orderSide, pageable);
    } else {
      // 필터링 없음
      return orderRepository.findUnfilledOrdersByUserId(userId, pageable);
    }
  }

  /**
   * Order 엔티티를 UnfilledOrderResponse로 변환
   * 
   * @param order 주문 엔티티
   * @return 미체결 주문 응답 DTO
   */
  private UnfilledOrderResponse convertToUnfilledOrderResponse(Order order) {
    return new UnfilledOrderResponse(
        order.getStock().getId(),
        order.getId(),
        order.getSide(),
        order.getPrice(),
        order.getAmount(),
        order.getUnfilledAmount(),
        order.getCanceledAmount(),
        order.getCreatedAt()
    );
  }
}
