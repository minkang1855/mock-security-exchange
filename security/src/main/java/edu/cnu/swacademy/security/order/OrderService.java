package edu.cnu.swacademy.security.order;

import edu.cnu.swacademy.security.asset.CashWallet;
import edu.cnu.swacademy.security.asset.CashWalletHistory;
import edu.cnu.swacademy.security.asset.CashWalletHistoryRepository;
import edu.cnu.swacademy.security.asset.CashWalletRepository;
import edu.cnu.swacademy.security.asset.CashWalletTransactionType;
import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.market.MarketStatus;
import edu.cnu.swacademy.security.market.MarketStatusRepository;
import edu.cnu.swacademy.security.market.TickSizeUtil;
import edu.cnu.swacademy.security.order.dto.ExchangeOrderResponse;
import edu.cnu.swacademy.security.order.dto.MakerOrderResponse;
import edu.cnu.swacademy.security.order.dto.ExchangeOrderCancelResponse;
import edu.cnu.swacademy.security.order.dto.OrderCancelResponse;
import edu.cnu.swacademy.security.order.dto.OrderSubmitRequest;
import edu.cnu.swacademy.security.order.dto.OrderSubmitResponse;
import edu.cnu.swacademy.security.order.dto.UnfilledOrderResponse;
import edu.cnu.swacademy.security.order.dto.UnfilledOrdersResponse;
import edu.cnu.swacademy.security.stock.Stock;
import edu.cnu.swacademy.security.stock.StockRepository;
import edu.cnu.swacademy.security.stock.StockWallet;
import edu.cnu.swacademy.security.stock.StockWalletHistory;
import edu.cnu.swacademy.security.stock.StockWalletHistoryRepository;
import edu.cnu.swacademy.security.stock.StockWalletRepository;
import edu.cnu.swacademy.security.stock.StockWalletTransactionType;
import edu.cnu.swacademy.security.user.User;
import edu.cnu.swacademy.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final SendOrderService sendOrderService;
  private final CashWalletHistoryRepository cashWalletHistoryRepository;
  private final StockWalletHistoryRepository stockWalletHistoryRepository;
  private final MatchRepository matchRepository;

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
          throw new SecurityException(ErrorCode.INVALID_ORDER_SIDE);
    }

    // 4. 가격 틱 사이즈 검증
    validateTickSize(request.price());

    // 5. 상하한가 검증
    validatePriceLimits(request.stockId(), request.price());

    // 6. 지갑 정지 및 자산 검증 이후 업데이트
    validateWalletStatusAndUpdate(userId, request.stockId(), orderSide, request.price(), request.quantity());

    // 7. 주문 생성
    Order order = orderRepository.save(
        new Order(
            user, stock, orderSide, request.price(), request.quantity(), request.quantity()
        )
    );

    // 8. Exchange 서버로 주문 전송
    ExchangeOrderResponse exchangeResponse = sendOrderService.sendOrderToExchange(
        order.getId(),
        request.stockId(),
        request.price(),
        request.quantity(),
        orderSide.getValue(),
        order.getCreatedAt()
    );

    log.info("Order submitted successfully: order-id(={}), match-result(={})",
        order.getId(), exchangeResponse.matchResult());

    // 9. Exchange 서버 응답에 따른 후속 처리
    return processExchangeResponse(order, exchangeResponse, request, orderSide);
  }

  /**
   * 지갑 정지 여부 검증 및 자산 부족 검증 및 업데이트
   */
  private void validateWalletStatusAndUpdate(int userId, int stockId, OrderSide orderSide, int price, int quantity) throws SecurityException {
    if (orderSide == OrderSide.BUY) {
      // 매수 주문: 현금 지갑만 검증 및 업데이트
      validateAndUpdateCashWallet(userId, price, quantity);
    } else if (orderSide == OrderSide.SELL) {
      // 매도 주문: 종목 지갑만 검증 및 업데이트
      validateAndUpdateStockWallet(userId, stockId, quantity);
    }
  }

  /**
   * 현금 지갑 검증 및 업데이트 (매수 주문용)
   */
  private void validateAndUpdateCashWallet(int userId, int price, int quantity) throws SecurityException {
    // 현금 지갑 조회
    CashWallet cashWallet = cashWalletRepository.findByUserId(userId)
        .orElseThrow(() -> {
          log.info("Cash wallet not found for user-id(={})", userId);
          return new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND);
        });

    // 현금 지갑 정지 여부 확인
    if (cashWallet.isBlocked()) {
      log.info("Cash wallet is blocked for user-id(={})", userId);
      throw new SecurityException(ErrorCode.CASH_WALLET_BLOCKED);
    }

    // 현금 지갑 잔액 확인 및 묶인 금액 업데이트
    int requestAmount = price * quantity;
    if (requestAmount > cashWallet.getAvailable()) {
      log.info("Insufficient cash wallet balance for user-id(={}). request-amount: {}",
          userId, requestAmount);
      throw new SecurityException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    cashWallet.updateBuyOrder(requestAmount);
    log.info("Buy order: Cash wallet tied amount increased by {}", requestAmount);
  }

  /**
   * 종목 지갑 검증 및 업데이트 (매도 주문용)
   */
  private void validateAndUpdateStockWallet(int userId, int stockId, int quantity) throws SecurityException {
    // 종목 지갑 조회
    StockWallet stockWallet = stockWalletRepository.findByUserIdAndStockId(userId, stockId)
        .orElseThrow(() -> {
          log.info("Stock wallet not found for user-id(={}), stock-id(={})", userId, stockId);
          return new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND);
        });

    // 종목 지갑 정지 여부 확인
    if (stockWallet.isBlocked()) {
      log.info("Stock wallet is blocked for stock-id(={})", stockId);
      throw new SecurityException(ErrorCode.STOCK_WALLET_BLOCKED);
    }

    // 종목 지갑 잔액 확인 및 묶인 수량 업데이트
    if (quantity > stockWallet.getAvailable()) {
      log.info("Insufficient stock balance for user-id(={}), stock-id(={}). Required: {}",
          userId, stockId, quantity);
      throw new SecurityException(ErrorCode.INSUFFICIENT_BALANCE);
    }

    stockWallet.updateSellOrder(quantity);
    log.info("Sell order: Stock wallet tied amount increased by {}", quantity);
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
    MarketStatus marketStatus = marketStatusRepository.findByStockIdAndTradingDate(stockId, today)
        .orElseThrow(() -> {
          log.error("No market status found for stock-id(={}) on date(={})", stockId, today);
          return new SecurityException(ErrorCode.INTERNAL_SERVER_ERROR);
        });
    
    if (price < marketStatus.getLowerLimitPrice() || price > marketStatus.getUpperLimitPrice()) {
      log.info("Price {} is out of limits for stock-id(={}). Lower: {}, Upper: {}", 
          price, stockId, marketStatus.getLowerLimitPrice(), marketStatus.getUpperLimitPrice());
      throw new SecurityException(ErrorCode.PRICE_OUT_OF_LIMITS);
    }
  }

  /**
   * Exchange 서버 응답에 따른 후속 처리
   */
  private OrderSubmitResponse processExchangeResponse(Order order, ExchangeOrderResponse exchangeResponse, OrderSubmitRequest request, OrderSide orderSide) throws SecurityException {
    return switch (exchangeResponse.matchResult()) {
      case "Unmatched" ->
        // 미체결: 후속 조치 불필요
          OrderSubmitResponse.success();
      case "Matched" -> {
        // 체결 완료: 지갑, 체결 내역, 장 상태 업데이트
        processMatchedOrder(exchangeResponse, request, orderSide);
        yield OrderSubmitResponse.success();
      }
      case "Rejected" -> {
        // 거절 처리
        processRejectedOrder(order, request, orderSide);
        yield OrderSubmitResponse.rejected(exchangeResponse.reason());
      }
      default -> throw new SecurityException(ErrorCode.EXCHANGE_SERVER_COMMUNICATION_FAILED);
    };
  }

  /**
  * 체결 완료 주문 처리
  */
  private void processMatchedOrder(ExchangeOrderResponse exchangeResponse, OrderSubmitRequest request, OrderSide orderSide) throws SecurityException {
    List<MakerOrderResponse> makers = exchangeResponse.makers();
    for (MakerOrderResponse maker : makers) {
      int matchedAmount = maker.matchedAmount();
      int matchedPrice = request.price();

      // maker가 매수자인지 매도자인지 판단
      OrderSide makerSide = (orderSide == OrderSide.BUY) ? OrderSide.SELL : OrderSide.BUY;

      if (makerSide == OrderSide.BUY) {
        // maker가 매수자, taker가 매도자
        processMakerBuyTakerSell(exchangeResponse, maker, matchedAmount, matchedPrice, request.stockId());
      } else {
        // maker가 매도자, taker가 매수자
        processMakerSellTakerBuy(exchangeResponse, maker, matchedAmount, matchedPrice, request.stockId());
      }
    }

    // 장 상태 업데이트
    updateMarketStatus(request.stockId(), exchangeResponse);
  }

  /**
  * Maker가 매수자, Taker가 매도자인 경우 처리
  */
  private void processMakerBuyTakerSell(ExchangeOrderResponse exchangeResponse, MakerOrderResponse maker, int matchedAmount, int matchedPrice, int stockId) throws SecurityException {
    Order makerOrder = orderRepository.findById(maker.orderId())
        .orElseThrow(() -> new SecurityException(ErrorCode.ORDER_NOT_FOUND));
    Order takerOrder = orderRepository.findById(exchangeResponse.takerOrderId())
        .orElseThrow(() -> new SecurityException(ErrorCode.ORDER_NOT_FOUND));

    // 1. Maker(매수자) 현금 계좌 처리
    CashWallet makerCashWallet = cashWalletRepository.findByUserIdWithLock(makerOrder.getUser().getId())
      .orElseThrow(() -> new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND));

    int matchedAmountInCash = matchedPrice * matchedAmount;
    makerCashWallet.updateBuyOrder(matchedAmountInCash);
    cashWalletRepository.save(makerCashWallet);

    // 현금 계좌 내역 생성
    createCashWalletHistory(makerCashWallet, CashWalletTransactionType.TRADE_RECEIPT, matchedAmountInCash);

    // 2. Taker(매도자) 종목 계좌 처리
    StockWallet takerStockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(takerOrder.getUser().getId(), stockId)
      .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND));

    takerStockWallet.updateSellOrder(matchedAmount);

    // 3. Maker(매수자) 종목 계좌 처리
    StockWallet makerStockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(makerOrder.getUser().getId(), stockId)
      .orElse(null);

    if (makerStockWallet == null) {
      // 종목 계좌가 없으면 새로 생성
      User makerUser = userRepository.findById(makerOrder.getUser().getId())
          .orElseThrow(() -> new SecurityException(ErrorCode.USER_NOT_FOUND));
      Stock stock = stockRepository.findById(stockId)
          .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_NOT_FOUND));

      makerStockWallet = new StockWallet(makerUser, stock);
      makerStockWallet.deposit(matchedAmount); // 매수한 수량만큼 초기화
    } else {
      makerStockWallet.deposit(matchedAmount); // 매수한 수량 추가
    }
    stockWalletRepository.save(makerStockWallet);

    // 종목 계좌 내역 생성
    createStockWalletHistory(takerStockWallet, StockWalletTransactionType.SELL_ORDER_EXECUTED, matchedAmount);
    createStockWalletHistory(makerStockWallet, StockWalletTransactionType.BUY_ORDER_EXECUTED, matchedAmount);

    // 4. 체결 내역 생성
    createMatch(stockId, takerOrder, maker, matchedAmount, matchedPrice);
  }

  /**
  * Maker가 매도자, Taker가 매수자인 경우 처리
  */
  private void processMakerSellTakerBuy(ExchangeOrderResponse exchangeResponse, MakerOrderResponse maker, int matchedAmount, int matchedPrice, int stockId) throws SecurityException {
    Order makerOrder = orderRepository.findById(maker.orderId())
        .orElseThrow(() -> new SecurityException(ErrorCode.ORDER_NOT_FOUND));
    Order takerOrder = orderRepository.findById(exchangeResponse.takerOrderId())
        .orElseThrow(() -> new SecurityException(ErrorCode.ORDER_NOT_FOUND));

    // 1. Maker(매도자) 종목 계좌 처리
    StockWallet makerStockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(makerOrder.getUser().getId(), stockId)
      .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND));

    makerStockWallet.updateSellOrder(matchedAmount); // 매도 수량 차감

    // 2. Taker(매수자) 현금 계좌 처리
    CashWallet takerCashWallet = cashWalletRepository.findByUserIdWithLock(takerOrder.getUser().getId())
      .orElseThrow(() -> new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND));

    int matchedAmountInCash = matchedPrice * matchedAmount;
    takerCashWallet.updateBuyOrder(matchedAmountInCash);
    cashWalletRepository.save(takerCashWallet);

    createCashWalletHistory(takerCashWallet, CashWalletTransactionType.TRADE_RECEIPT, matchedAmountInCash);

    // 3. Taker(매수자) 종목 계좌 처리
    StockWallet takerStockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(takerOrder.getUser().getId(), stockId)
      .orElse(null);

    if (takerStockWallet == null) {
      // 종목 계좌가 없으면 새로 생성
      User takerUser = takerOrder.getUser();
      Stock stock = stockRepository.findById(stockId)
          .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_NOT_FOUND));

      takerStockWallet = new StockWallet(takerUser, stock);
      takerStockWallet.deposit(matchedAmount); // 매수한 수량만큼 초기화
    } else {
      takerStockWallet.deposit(matchedAmount); // 매수한 수량 추가
    }
    stockWalletRepository.save(takerStockWallet);

    // 내역 생성
    createStockWalletHistory(makerStockWallet, StockWalletTransactionType.SELL_ORDER_EXECUTED, -matchedAmount);
    createStockWalletHistory(takerStockWallet, StockWalletTransactionType.BUY_ORDER_EXECUTED, matchedAmount);

    // 4. 체결 내역 생성
    createMatch(stockId, takerOrder, maker, matchedAmount, matchedPrice);
  }

  /**
  * 거절된 주문 처리
  */
  private void processRejectedOrder(Order order, OrderSubmitRequest request, OrderSide orderSide) throws SecurityException {
    int userId = order.getUser().getId();
    int rejectedAmount = request.quantity();
    int rejectedPrice = request.price();

    order.cancel();
    orderRepository.save(order);
    if (orderSide == OrderSide.BUY) {
      // 현금 지갑 롤백
      CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
          .orElseThrow(() -> new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND));

      int rejectedAmountInCash = rejectedPrice * rejectedAmount;
      cashWallet.updateOrderCancel(rejectedAmountInCash);

      // 현금 지갑 내역 생성
      createCashWalletHistory(cashWallet, CashWalletTransactionType.TRADE_REFUND, rejectedAmountInCash);
    } else {
      // 종목 지갑 롤백
      StockWallet stockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(userId, request.stockId())
          .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND));

      stockWallet.updateOrderCancel(rejectedAmount); // 매도 수량 차감

      // 종목 지갑 내역 생성
      createStockWalletHistory(stockWallet, StockWalletTransactionType.SELL_ORDER_CANCEL, rejectedAmount);
    }
  }

  /**
  * 체결 내역 생성
  */
  private void createMatch(int stockId, Order takerOrder, MakerOrderResponse maker, int amount, int price) throws SecurityException {
      // Match 엔티티 생성 및 저장
      Stock stock = stockRepository.findById(stockId)
          .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_NOT_FOUND));

      Order makerOrder = orderRepository.findById(maker.orderId())
          .orElseThrow(() -> new SecurityException(ErrorCode.ORDER_NOT_FOUND));

      Match match = new Match(stock, makerOrder, takerOrder);
      matchRepository.save(match);
    }

    /**
    * 현금 지갑 내역 생성
    */
    private void createCashWalletHistory(CashWallet cashWallet, CashWalletTransactionType type, int amount) {
      CashWalletHistory history = new CashWalletHistory(cashWallet, type, amount, "매수 주문 취소", cashWallet.getReserve());
      cashWalletHistoryRepository.save(history);
  }

  /**
  * 종목 지갑 내역 생성
  */
  private void createStockWalletHistory(StockWallet stockWallet, StockWalletTransactionType type, int amount) {
    StockWalletHistory history = new StockWalletHistory(stockWallet, type, amount, "매도 주문 취소", stockWallet.getReserve());
    stockWalletHistoryRepository.save(history);
  }

  /**
  * 장 상태 업데이트
  */
  private void updateMarketStatus(int stockId, ExchangeOrderResponse exchangeResponse) throws SecurityException {
    MarketStatus marketStatus = marketStatusRepository.findByStockIdAndTradingDate(stockId, LocalDate.now())
        .orElseThrow(() -> new SecurityException(ErrorCode.MARKET_STATUS_NOT_FOUND));
    marketStatus.update(exchangeResponse.price(), exchangeResponse.totalMatchedAmount(), exchangeResponse.price() * exchangeResponse.totalMatchedAmount());
  }

  /**
   * 주문 취소
   */
  @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
  public OrderCancelResponse cancelOrder(int userId, int orderId) throws SecurityException {
    // 1. 주문 존재 여부 및 소유권 검증
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new SecurityException(ErrorCode.ORDER_NOT_FOUND));

    if (order.getUser().getId() != userId) {
      throw new SecurityException(ErrorCode.ORDER_ACCESS_DENIED);
    }

    // 2. 주문 상태 검증 (이미 취소된 주문인지 확인)
    if (order.getUnfilledAmount() == 0 && order.getCanceledAmount() != 0) {
      throw new SecurityException(ErrorCode.ORDER_ALREADY_CANCELLED);
    }

    // 3. Exchange 서버로 주문 취소 요청
    ExchangeOrderCancelResponse exchangeResponse = sendOrderService.cancelOrderToExchange(orderId, order.getStock().getId(), order.getSide().getValue(), order.getPrice());

    log.info("Order cancellation response: order-id(={}), match-result(={})", orderId, exchangeResponse.matchResult());

    // 4. Exchange 서버 응답에 따른 후속 처리
    return processCancelResponse(order, exchangeResponse);
  }

  /**
   * Exchange 서버 취소 응답에 따른 후속 처리
   */
  private OrderCancelResponse processCancelResponse(Order order, ExchangeOrderCancelResponse exchangeResponse) throws SecurityException {
    if (exchangeResponse.matchResult().equals("Cancelled")) {// 취소 성공: 지갑 롤백 및 내역 생성
      processCancelledOrder(order);
      return OrderCancelResponse.success();
    } else if (exchangeResponse.matchResult().equals("Rejected")) {
      log.info("Cancelled order not found. order-id(={})", order.getId());
      throw new SecurityException(ErrorCode.ORDER_NOT_FOUND);
    } else {
      throw new SecurityException(ErrorCode.EXCHANGE_SERVER_COMMUNICATION_FAILED);
    }
  }

  /**
   * 취소된 주문 처리
   */
  private void processCancelledOrder(Order order) throws SecurityException {
    int userId = order.getUser().getId();
    int cancelledAmount = order.getUnfilledAmount();
    int cancelledPrice = order.getPrice();
    OrderSide orderSide = order.getSide();

    // 1. 주문 상태 업데이트
    order.cancel();

    if (orderSide == OrderSide.BUY) {
      // 매수 주문 취소
      // 현금 지갑 롤백
      CashWallet cashWallet = cashWalletRepository.findByUserIdWithLock(userId)
          .orElseThrow(() -> new SecurityException(ErrorCode.CASH_WALLET_NOT_FOUND));

      int cancelledAmountInCash = cancelledPrice * cancelledAmount;
      cashWallet.updateOrderCancel(cancelledAmountInCash); // 매수 주문으로 묶인 금액 차감

      // 현금 지갑 내역 생성
      createCashWalletHistory(cashWallet, CashWalletTransactionType.TRADE_REFUND, cancelledAmountInCash);
    } else {
      // 매도 주문 취소
      // 종목 지갑 롤백
      StockWallet stockWallet = stockWalletRepository.findByUserIdAndStockIdWithLock(userId, order.getStock().getId())
          .orElseThrow(() -> new SecurityException(ErrorCode.STOCK_WALLET_NOT_FOUND));

      stockWallet.updateOrderCancel(cancelledAmount); // 매도 수량 차감

      // 종목 지갑 내역 생성
      createStockWalletHistory(stockWallet, StockWalletTransactionType.SELL_ORDER_CANCEL, cancelledAmount);
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
