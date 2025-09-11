package edu.cnu.swacademy.security.order;

import edu.cnu.swacademy.security.asset.CashWallet;
import edu.cnu.swacademy.security.asset.CashWalletRepository;
import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.market.MarketStatusRepository;
import edu.cnu.swacademy.security.market.TickSizeUtil;
import edu.cnu.swacademy.security.order.dto.OrderSubmitRequest;
import edu.cnu.swacademy.security.order.dto.OrderSubmitResponse;
import edu.cnu.swacademy.security.order.dto.UnfilledOrderResponse;
import edu.cnu.swacademy.security.order.dto.UnfilledOrdersResponse;
import edu.cnu.swacademy.security.stock.Stock;
import edu.cnu.swacademy.security.stock.StockRepository;
import edu.cnu.swacademy.security.stock.StockWallet;
import edu.cnu.swacademy.security.stock.StockWalletRepository;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 주문 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final UserRepository userRepository;
  private final StockRepository stockRepository;
  private final CashWalletRepository cashWalletRepository;
  private final StockWalletRepository stockWalletRepository;
  private final MarketStatusRepository marketStatusRepository;

  /**
   * 주문 접수
   * 사용자가 특정 종목에 대해 매수/매도 주문을 접수합니다.
   * 
   * @param userId 사용자 ID
   * @param request 주문 접수 요청
   * @return 주문 접수 응답
   * @throws SecurityException 주문 접수 실패 시 발생
   */
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
  public OrderSubmitResponse submitOrder(int userId, OrderSubmitRequest request) throws SecurityException {
    log.info("Submitting order for user-id(={}), stock-id(={}), side(={})", 
        userId, request.stockId(), request.side());

    // 1. 사용자 존재 여부 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
        log.info("User not found for id(={})", userId);
        return new SecurityException(ErrorCode.USER_NOT_FOUND);
        });

    // 2. 종목 존재 여부 확인
    Stock stock = stockRepository.findById(request.stockId())
        .orElseThrow(() -> {
        log.info("Stock not found for id(={})", request.stockId());
        return new SecurityException(ErrorCode.STOCK_NOT_FOUND);
        });

    // 3. 주문 방향 검증
    OrderSide orderSide;
    try {
        orderSide = OrderSide.valueOf(request.side().toUpperCase());
    } catch (Exception e) {
        log.info("Invalid order side: {}", request.side());
        return OrderSubmitResponse.rejected("유효하지 않은 주문 방향입니다.");
    }

    // 4. 지갑 정지 여부 검증
    validateWalletStatus(userId, request.stockId());

    // 5. 가격 틱 사이즈 검증
    validateTickSize(request.price());

    // 6. 상하한가 검증
    validatePriceLimits(request.stockId(), request.price());

    // 7. 현금/종목 게좌 내 자산 부족 검증 및 업데이트
    validateAndUpdateWallet(userId, request.stockId(), orderSide, request.price(), request.quantity());

    // 8. 주문 생성
    Order order = orderRepository.save(
        new Order(
            user, stock, orderSide, request.price(), request.quantity(), request.quantity()
        )
    );

    // 9. Exchange 서버로 주문 전송

    log.info("Order submitted successfully: order-id(={})", order.getId());

    // 10. Exchange 서버로부터 전달 받은 값 반환
    return OrderSubmitResponse.success();
  }

  /**
   * 지갑 정지 여부 검증
   */
  private void validateWalletStatus(int userId, int stockId) throws SecurityException {
    // 현금 지갑 정지 여부 확인
    CashWallet cashWallet = cashWalletRepository.findByUserId(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 종목 지갑 정지 여부 확인
    StockWallet stockWallet = stockWalletRepository.findByUserIdAndStockId(userId, stockId)
        .orElseThrow(() -> {
          log.info("Stock wallet not found for user-id(={}), stock-id(={})", userId, stockId);
          return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
        });

    if (stockWallet.isBlocked()) {
      log.info("Stock wallet is blocked for stock-id(={})", stockId);
      throw new SecurityException(ErrorCode.STOCK_WALLET_BLOCKED);
    }
  }

  /**
   * 가격 틱 사이즈 검증
   */
  private void validateTickSize(int price) throws SecurityException {
    BigDecimal priceDecimal = BigDecimal.valueOf(price);
    if (!TickSizeUtil.isValidTickSize(priceDecimal)) {
      log.info("Invalid tick size for price");
      throw new SecurityException(ErrorCode.INVALID_TICK_SIZE);
    }
  }

  /**
   * 상하한가 검증
   */
  private void validatePriceLimits(int stockId, int price) throws SecurityException {
    // 당일 MarketStatus 조회
    LocalDate today = LocalDate.now();
    var marketStatusOpt = marketStatusRepository.findByStockIdAndTradingDate(stockId, today);
    
    if (marketStatusOpt.isEmpty()) {
      log.error("No market status found for stock-id(={}) on date(={})", stockId, today);
      throw new SecurityException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    var marketStatus = marketStatusOpt.get();
    
    if (price < marketStatus.getLowerLimitPrice() || price > marketStatus.getUpperLimitPrice()) {
      log.info("Price {} is out of limits for stock-id(={}). Lower: {}, Upper: {}", 
          price, stockId, marketStatus.getLowerLimitPrice(), marketStatus.getUpperLimitPrice());
      throw new SecurityException(ErrorCode.PRICE_OUT_OF_LIMITS);
    }
  }

  /**
   * 현금/종목 게좌 내 자산 부족 검증 및 업데이트
   */
  private void validateAndUpdateWallet(int userId, int stockId, OrderSide orderSide, int price, int quantity) throws SecurityException {
    if (orderSide == OrderSide.BUY) {
      // 매수 주문: 현금 지갑 잔액 확인
      CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
          .orElseThrow(() -> {
            log.info("Cash wallet not found for user-id(={})", userId);
            return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
          });

      int requestAmount = price * quantity;
      if (requestAmount > cashWallet.getAvailable()) {
        log.info("Insufficient cash balance for user-id(={}). request-amount: {}",
            userId, requestAmount);
        throw new SecurityException(ErrorCode.INSUFFICIENT_BALANCE);
      }

      cashWallet.updateBuyOrder(requestAmount);
      log.info("Buy order: Cash wallet tied amount increased.");
    } else {
      // 매도 주문: 종목 지갑 잔액 확인
      StockWallet stockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(userId, stockId)
          .orElseThrow(() -> {
            log.info("Stock wallet not found for user-id(={}), stock-id(={})", userId, stockId);
            return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
          });

      if (quantity > stockWallet.getAvailable()) {
        log.info("Insufficient stock balance for user-id(={}), stock-id(={}). Required: {}",
            userId, stockId, quantity);
        throw new SecurityException(ErrorCode.INSUFFICIENT_BALANCE);
      }

      stockWallet.updateSellOrder(quantity);
      log.info("Sell order: Stock wallet tied amount increased.");
    }
  }

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
