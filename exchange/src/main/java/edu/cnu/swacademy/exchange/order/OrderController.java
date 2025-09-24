package edu.cnu.swacademy.exchange.order;

import edu.cnu.swacademy.exchange.order.dto.OrderCancelRequest;
import edu.cnu.swacademy.exchange.order.dto.OrderCancelResponse;
import edu.cnu.swacademy.exchange.order.dto.OrderProcessRequest;
import edu.cnu.swacademy.exchange.order.dto.OrderProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 처리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class OrderController {

    private final OrderBookService orderBookService;

  /**
   * 주문 처리
   * 증권사 서버로부터 주문을 받아 오더북에 추가하고 매칭을 시도합니다.
   *
   * @param request 주문 처리 요청
   * @return 주문 처리 결과
   */
  @PostMapping("/order")
  public OrderProcessResponse processOrder(@RequestBody OrderProcessRequest request) {
    log.info("Received order processing request: orderId={}, stockId={}, side={}",
        request.orderId(), request.stockId(), request.side());

    try {
        OrderProcessResponse response = orderBookService.processOrder(request);

        log.info("Order processing completed: orderId={}, matchResult={}",
            request.orderId(), response.matchResult());

        return response;
    } catch (Exception e) {
        log.error("Failed to process order: orderId={}, e={}, msg={}",
            request.orderId(), e.getClass(), e.getMessage());

        return OrderProcessResponse.rejected("거래소 서버 오류");
    }
  }

  /**
   * 주문 취소
   * 오더북에 등록된 미체결 주문을 취소 처리합니다.
   *
   * @param request 주문 취소 요청
   * @return 주문 취소 결과
   */
  @DeleteMapping("/order")
  public OrderCancelResponse cancelOrder(@RequestBody OrderCancelRequest request) {
    log.info("Received order cancellation request: orderId={}, stockId={}, side={}, price={}",
        request.orderId(), request.stockId(), request.side(), request.price());
    try {
        OrderCancelResponse response = orderBookService.cancelOrder(request);

        log.info("Order cancellation completed: orderId={}, matchResult={}",
            request.orderId(), response.matchResult());

        return response;
    } catch (Exception e) {
        log.error("Failed to cancel order: orderId={}, e={}, msg={}",
            request.orderId(), e.getClass(), e.getMessage());
        return OrderCancelResponse.rejected();
    }
  }
}
