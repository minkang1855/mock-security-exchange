package edu.cnu.swacademy.security.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cnu.swacademy.security.order.dto.UnfilledOrdersResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  /**
   * 당일 미체결 주문 내역 조회
   * 사용자가 거래소에 제출한 미체결 주문 내역을 조회합니다.
   * Order 엔티티의 unfilledQuantity가 1 이상인 주문들만 가져옵니다.
   * 
   * @param request HTTP 요청 (JWT 토큰에서 사용자 ID 추출)
   * @param stockId 종목 ID (선택사항)
   * @param side 매수/매도 방향 (선택사항)
   * @param page 페이지 번호 (기본값: 0)
   * @param size 페이지 크기 (기본값: 10)
   * @param sort 정렬 방향 (기본값: desc)
   * @return 미체결 주문 목록
   */
  @GetMapping("/unfilled")
  public UnfilledOrdersResponse getUnfilledOrders(
      HttpServletRequest request,
      @RequestParam(required = false) Integer stockId,
      @RequestParam(required = false) String side,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "desc") String sort
  ) {
    // JWT에서 사용자 ID 추출
    int userId = (int) request.getAttribute("user_id");

    // 정렬 방향 검증 및 설정
    Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size,  Sort.by(direction, "createdAt"));

    return orderService.getUnfilledOrders(userId, stockId, side, pageable);
  }
}
