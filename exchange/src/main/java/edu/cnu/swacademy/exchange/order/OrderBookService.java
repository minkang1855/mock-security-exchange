package edu.cnu.swacademy.exchange.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cnu.swacademy.exchange.order.dto.MakerOrderResponse;
import edu.cnu.swacademy.exchange.order.dto.OrderBookEntry;
import edu.cnu.swacademy.exchange.order.dto.OrderCancelRequest;
import edu.cnu.swacademy.exchange.order.dto.OrderCancelResponse;
import edu.cnu.swacademy.exchange.order.dto.OrderProcessRequest;
import edu.cnu.swacademy.exchange.order.dto.OrderProcessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 오더북 관리 서비스
 * Redis를 사용하여 오더북을 관리하고 주문 매칭을 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBookService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 주문 처리
     * 새로운 주문을 오더북에 추가하고 매칭을 시도합니다.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderProcessResponse processOrder(OrderProcessRequest request) {
        log.info("Processing order: order-id={}, product-id={}, side={}", request.orderId(), request.stockId(), request.side());

        // 1. 틱 사이즈 검증
        if (!isValidTickSize(request.price())) {
            log.warn("Invalid tick size for order-id: {}", request.orderId());
            return OrderProcessResponse.rejected("틱 사이즈 규칙 위반");
        }

        // 2. 매칭 시도
        OrderProcessResponse matchResult = attemptMatching(request);

        if (matchResult.matchResult().equals("Matched")) {
            log.info("Order matched: taker-order-id={}, maker-order-ids={}",
                matchResult.takerOrderId(), matchResult.makers().stream().map(MakerOrderResponse::orderId).toList());
            return matchResult;
        } else if (matchResult.matchResult().equals("Unmatched")) {
            log.info("Order added to order book: orderId={}", request.orderId());
            return OrderProcessResponse.unmatched();
        } else {
            throw new RuntimeException("Order processing failed");
        }
    }

    /**
     * 주문 취소
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCancelResponse cancelOrder(OrderCancelRequest request) {
        log.info("Cancelling order: orderId={}, stockId={}, side={}, price={}", 
            request.orderId(), request.stockId(), request.side(), request.price());

        try {
            // 1. 요청한 정보를 바탕으로 Orders 해당 가격 및 side의 주문 모두 조회
            String orderKey = getOrderKey(request.stockId(), request.side(), request.price());
            List<Object> orders = redisTemplate.opsForList().range(orderKey, 0, -1);
            
            if (orders == null || orders.isEmpty()) {
                log.info("No orders found for stockId={}, side={}, price={}",
                    request.stockId(), request.side(), request.price());
                return OrderCancelResponse.rejected();
            }

            // 2. 조회한 주문을 순회하면서 order_id가 동일한게 있는지 확인
            OrderBookEntry targetOrder = null;
            for (Object orderObj : orders) {
                OrderBookEntry order = convertToOrderBookEntry(orderObj);
                if (order != null && order.getOrderId() == request.orderId()) {
                    targetOrder = order;
                    break;
                }
            }

            // 3. 없으면 예외 던지기
            if (targetOrder == null) {
                log.info("Order not found in order book: orderId={}, stockId={}, side={}, price={}",
                    request.orderId(), request.stockId(), request.side(), request.price());
                return OrderCancelResponse.rejected();
            }

            // 4. 있으면 해당 order를 Orders에서 삭제
            redisTemplate.opsForList().remove(orderKey, 1, targetOrder);
            log.info("Order removed from order book: orderId={}, unfilledUnit={}", 
                request.orderId(), targetOrder.getUnfilledUnit());

            // 5. 취소된 수량 만큼 totalUnits 감소
            String totalUnitKey = getTotalUnitKey(request.stockId(), request.side());
            int cancelledAmount = targetOrder.getUnfilledUnit();
            redisTemplate.opsForHash().increment(totalUnitKey, String.valueOf(request.price()), -cancelledAmount);

            // 6. order를 삭제한 이후 해당 Orders에 주문이 없으면 이에 해당하는 prices와 totalUnits 제거
            if (orders.size() == 1) {
                // 해당 가격의 주문이 모두 없어졌으면 Prices에서도 제거
                removePriceFromOrderBook(request.stockId(),  request.side(), request.price());
                log.info("Removed price {} from price list: stockId={}, side={}", 
                    request.price(), request.stockId(), request.side());
            }

            log.info("Order cancelled successfully: orderId={}, cancelledAmount={}", 
                request.orderId(), cancelledAmount);
            return OrderCancelResponse.cancelled();
        } catch (Exception e) {
            log.error("Failed to cancel order: orderId={}, error={}", request.orderId(), e.getMessage(), e);
            return OrderCancelResponse.rejected();
        }
    }

    /**
     * 매칭 시도
     */
    private OrderProcessResponse attemptMatching(OrderProcessRequest request) {
        String oppositeSide = request.side().equals("BUY") ? "SELL" : "BUY";

//        // 반대편에 매칭되는 주문이 없음
//        if (popFrontOrderAtPrice(request.stockId(), oppositeSide, request.price()) == null) {
//            // 미체결 주문 추가
//            addUnfilledOrderToBook(request, request.amount());
//
//            // 반대편에 해당하는 Price, TotalUnit 제거
//            removePriceFromOrderBook(request.stockId(), oppositeSide, request.price());
//            return OrderProcessResponse.unmatched();
//        }
        
        int remainingAmount = request.amount();
        int totalMatchUnit = 0;
        List<MakerOrderResponse> makers = new ArrayList<>();

        while (true) {
            // 반대편 주문의 가장 앞쪽(FIFO) 주문 조회
            OrderBookEntry frontOrder = popFrontOrderAtPrice(request.stockId(), oppositeSide, request.price());

            // 더 이상 반대편에 매칭되는 주문이 없음
            if (frontOrder == null) {
                // 주문 추가
                addUnfilledOrderToBook(request, remainingAmount);

                // 반대편에 해당하는 Price, TotalUnit 제거
                removePriceFromOrderBook(request.stockId(), oppositeSide, request.price());
                break;
            }

            // 매칭된 수량 계산
            int matchedAmount = Math.min(remainingAmount, frontOrder.getUnfilledUnit());

            // 매칭된 수량 차감
            frontOrder.decreaseAmount(matchedAmount);
            remainingAmount -= matchedAmount;

            // matchUnit에 매칭된 수량 추가
            totalMatchUnit += matchedAmount;

            // 반대편에 매칭된 가격에 해당하는 totalUnits 차감
            updateTotalUnits(request.stockId(), oppositeSide, request.price(), matchedAmount);

            // makers에 체결된 반대편 주문 정보 저장
            makers.add(new MakerOrderResponse(frontOrder.getOrderId(), matchedAmount));

            // 반대편의 매칭된 주문의 남은 수량이 0인 경우
            if (frontOrder.getUnfilledUnit() == 0) {
                List<OrderBookEntry> orders = getOrdersAtPrice(request.stockId(), oppositeSide, request.price());

                // 길이가 0이면 더 이상 반대편의 주문이 없으므로 Price, TotalUnit 제거
                if (orders.isEmpty()) { // 현재 주문만 남아있고 완전히 체결된 경우
                    removePriceFromOrderBook(request.stockId(), oppositeSide, request.price());
                }
            } else {
                // 반대편의 매칭된 주문의 남은 수량이 0이 아니면 해당 order를 Orders 맨 앞에 저장
                updateOrderInList(request.stockId(), oppositeSide, request.price(), frontOrder);

                // 반대편의 매칭된 주문과 매핑되는 가격이 Prices에 저장되지 않았다면 Prices에 해당 매칭된 가격을 저장 (오름차순 정렬)
                addPriceToSortedList(request.stockId(), oppositeSide, request.price());
            }

            // remainingAmount가 0이면 break
            if (remainingAmount == 0) {
                break;
            }
        }
        
        // remainingAmount가 0보다 크면 미체결된 주문을 Orders에 저장
        if (remainingAmount > 0 && !makers.isEmpty()) {
            addUnfilledOrderToBook(request, remainingAmount);
        }
        
        if (!makers.isEmpty()) {
            return OrderProcessResponse.matched(request.orderId(), makers, request.price(), totalMatchUnit);
        } else {
            return OrderProcessResponse.unmatched();
        }
    }

    /**
     * 특정 가격의 가장 앞쪽 주문을 pop (FIFO)하고 관련 데이터 정리
     */
    private OrderBookEntry popFrontOrderAtPrice(int stockId, String side, int price) {
        String orderKey = getOrderKey(stockId, side, price);
        Object frontOrder = redisTemplate.opsForList().leftPop(orderKey);
        log.info("frontOrder={}", frontOrder);

        if (frontOrder == null) {
            return null;
        }

        String priceKey = getPriceKey(stockId, side);
        String totalUnitKey = getTotalUnitKey(stockId, side);

        // Prices에서 해당 가격 제거
        redisTemplate.opsForList().remove(priceKey, 1, price);

        // TotalUnit에서 해당 가격 필드 제거
        redisTemplate.opsForHash().delete(totalUnitKey, String.valueOf(price));

        return convertToOrderBookEntry(frontOrder);
    }

    /**
     * 미체결 주문을 오더북에 추가
     */
    private void addUnfilledOrderToBook(OrderProcessRequest request, int remainingAmount) {
        String orderKey = getOrderKey(request.stockId(), request.side(), request.price());
        String totalUnitKey = getTotalUnitKey(request.stockId(), request.side());

        // 1. 가격 리스트에 추가
        addPriceToSortedList(request.stockId(), request.side(), request.price());

        // 2. 주문 리스트에 추가
        OrderBookEntry entry = new OrderBookEntry(request.orderId(), remainingAmount, request.createdAt());
        redisTemplate.opsForList().rightPush(orderKey, entry);

        // 3. 총 수량 업데이트
        redisTemplate.opsForHash().increment(totalUnitKey, String.valueOf(request.price()), remainingAmount);
    }

    /**
     * 특정 가격의 모든 데이터를 오더북에서 제거
     */
    private void removePriceFromOrderBook(int stockId, String side, int price) {
        String priceKey = getPriceKey(stockId, side);
        String totalUnitKey = getTotalUnitKey(stockId, side);
        
        // 1. 가격 리스트에서 해당 가격 제거
        redisTemplate.opsForList().remove(priceKey, 1, price);
        
        // 2. 총 수량에서 해당 가격 필드 제거
        redisTemplate.opsForHash().delete(totalUnitKey, String.valueOf(price));
        
        log.info("Removed price {} from order book: stockId={}, side={}", price, stockId, side);
    }


    /**
     * 특정 가격의 주문들 조회
     */
    private List<OrderBookEntry> getOrdersAtPrice(int stockId, String side, int price) {
        String orderKey = getOrderKey(stockId, side, price);
        List<Object> orders = redisTemplate.opsForList().range(orderKey, 0, -1);

        List<OrderBookEntry> result = new ArrayList<>();
        if (orders != null && !orders.isEmpty()) {
            orders.forEach(order -> result.add(convertToOrderBookEntry(order)));
        }
        return result;
    }

    /**
     * 틱 사이즈 검증
     */
    private boolean isValidTickSize(int price) {
        // 간단한 틱 사이즈 검증 로직
        if (price < 2000) return price % 1 == 0;
        if (price < 5000) return price % 5 == 0;
        if (price < 20000) return price % 10 == 0;
        if (price < 50000) return price % 50 == 0;
        if (price < 200000) return price % 100 == 0;
        if (price < 500000) return price % 500 == 0;
        return price % 1000 == 0;
    }

    /**
     * TotalUnits 업데이트
     */
    private void updateTotalUnits(int stockId, String side, int price, int amount) {
        String totalUnitKey = getTotalUnitKey(stockId, side);
        redisTemplate.opsForHash().increment(totalUnitKey, String.valueOf(price), amount);
    }

    /**
     * 주문 리스트에서 특정 주문 업데이트 (맨 앞에 저장)
     */
    private void updateOrderInList(int stockId, String side, int price, OrderBookEntry entry) {
        String orderKey = getOrderKey(stockId, side, price);

        // 업데이트된 주문을 맨 앞에 추가
        redisTemplate.opsForList().leftPush(orderKey, entry);
    }

    /**
     * 가격을 오름차순으로 정렬된 리스트에 추가
     */
    private void addPriceToSortedList(int stockId, String side, int price) {
        String priceKey = getPriceKey(stockId, side);

        // 1. 해당 priceKey에 대한 list 조회
        List<Object> existingPrices = redisTemplate.opsForList().range(priceKey, 0, -1);

        if (existingPrices == null || existingPrices.isEmpty()) {
            // 리스트가 비어있으면 그냥 추가
            redisTemplate.opsForList().rightPush(priceKey, price);
            return;
        }

        // 2. Integer 리스트로 변환
        List<Integer> prices = existingPrices.stream()
            .map(obj -> (Integer) obj)
            .toList();

        // 3. 이진 탐색으로 삽입 위치 찾기
        int insertPos = Collections.binarySearch(prices, price);

        if (insertPos >= 0) {
            // 이미 존재하는 price면 스킵
        } else {
            // 삽입 위치 계산 (음수를 양수로 변환)
            insertPos = -(insertPos + 1);

            if (insertPos >= prices.size()) {
                // 맨 뒤에 삽입
                redisTemplate.opsForList().rightPush(priceKey, price);
            } else {
                // 중간에 삽입 (Redis LINSERT 사용)
                Integer existingPrice = prices.get(insertPos);
                redisTemplate.getConnectionFactory().getConnection()
                    .listCommands()
                    .lInsert(priceKey.getBytes(),
                        RedisListCommands.Position.BEFORE,
                        String.valueOf(existingPrice).getBytes(),
                        String.valueOf(price).getBytes());
            }
        }
    }

    private OrderBookEntry convertToOrderBookEntry(Object orderObj) {
        try {
            if (orderObj instanceof OrderBookEntry) {
                return (OrderBookEntry) orderObj;
            } else if (orderObj instanceof Map) {
                return objectMapper.convertValue(orderObj, OrderBookEntry.class);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to convert to OrderBookEntry: {}", orderObj, e);
            return null;
        }
    }

    /**
     * Redis 키 메서드들
     */
    private String getPriceKey(int stockId, String side) {
        return String.format("%d:%s", stockId, side);
    }

    private String getOrderKey(int stockId, String side, int price) {
        return String.format("%d:%s:%d", stockId, side, price);
    }

    private String getTotalUnitKey(int stockId, String side) {
        return String.format("%d:%s:total-unit", stockId, side);
    }
}
