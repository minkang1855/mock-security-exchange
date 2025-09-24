package edu.cnu.swacademy.security.orderbook;

import edu.cnu.swacademy.security.orderbook.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 오더북 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderBookService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 오더북 조회
     */
    public OrderBookResponse getOrderBook(int stockId) {
        log.info("Retrieving order book for stockId: {}", stockId);

        // BUY와 SELL 양쪽 오더북 조회
        OrderBookSideResponse buySide = getOrderBookSide(stockId, "BUY");
        OrderBookSideResponse sellSide = getOrderBookSide(stockId, "SELL");

        log.info("Retrieved order book for stockId: {}, buy prices: {}, sell prices: {}", 
            stockId, buySide.prices().size(), sellSide.prices().size());

        return new OrderBookResponse(buySide, sellSide);
    }

    /**
     * 특정 측면(BUY/SELL)의 오더북 조회
     */
    private OrderBookSideResponse getOrderBookSide(int stockId, String side) {
        String priceKey = getPriceKey(stockId, side);
        String totalUnitKey = getTotalUnitKey(stockId, side);

        // 1. 가격 리스트 조회
        List<Object> prices = redisTemplate.opsForList().range(priceKey, 0, -1);
        if (prices == null || prices.isEmpty()) {
            log.info("No prices found for stockId: {}, side: {}", stockId, side);
            return new OrderBookSideResponse(new HashMap<>());
        }

        Map<String, OrderBookPriceResponse> priceMap = new LinkedHashMap<>();

        // 2. 각 가격별로 주문 정보 조회
        for (Object priceObj : prices) {
            int price = (Integer) priceObj;
            String priceStr = String.valueOf(price);
            String orderKey = getOrderKey(stockId, side, price);

            // 3. 해당 가격의 주문 리스트 조회
            List<Object> orders = redisTemplate.opsForList().range(orderKey, 0, -1);
            if (orders == null || orders.isEmpty()) {
                log.warn("No orders found for stockId: {}, side: {}, price: {}", stockId, side, price);
                continue;
            }

            // 4. 주문 정보를 DTO로 변환
            List<OrderBookOrderResponse> orderResponses = orders.stream()
                .map(orderObj -> {
                    // OrderBookEntry 객체를 OrderBookOrderResponse로 변환
                    if (orderObj instanceof OrderBookEntry entry) {
                      return new OrderBookOrderResponse(
                            entry.getOrderId(),
                            entry.getUnfilledUnit(),
                            entry.getCreatedAt()
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            // 5. 총 수량 조회
            Object totalQuantityObj = redisTemplate.opsForHash().get(totalUnitKey, priceStr);
            int totalQuantity = totalQuantityObj != null ? (Integer) totalQuantityObj : 0;

            // 6. 가격별 정보 생성
            OrderBookPriceResponse priceResponse = new OrderBookPriceResponse(totalQuantity, orderResponses);
            priceMap.put(priceStr, priceResponse);

            log.debug("Retrieved price level: stockId={}, side={}, price={}, totalQuantity={}, orderCount={}", 
                stockId, side, price, totalQuantity, orderResponses.size());
        }

        return new OrderBookSideResponse(priceMap);
    }

    /**
     * Redis 키 생성 메서드들
     */
    private String getPriceKey(int stockId, String side) {
        return String.format("%d:%s:prices", stockId, side);
    }

    private String getOrderKey(int stockId, String side, int price) {
        return String.format("%d:%s:%d", stockId, side, price);
    }

    private String getTotalUnitKey(int stockId, String side) {
        return String.format("%d:%s:total-unit", stockId, side);
    }
}
