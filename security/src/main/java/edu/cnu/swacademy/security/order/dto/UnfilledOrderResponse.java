package edu.cnu.swacademy.security.order.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.cnu.swacademy.security.order.OrderSide;

/**
 * 미체결 주문 응답 DTO
 */
public record UnfilledOrderResponse(
    int stockId,
    int orderId,
    String side,
    int price,
    int quantity,
    int unfilledQuantity,
    int canceledQuantity,
    String createdAt
) {
    public UnfilledOrderResponse(int stockId, int orderId, OrderSide side, int price, int quantity, 
                                int unfilledQuantity, int canceledQuantity, LocalDateTime createdAt) {
        this(
            stockId,
            orderId,
            side.getValue(),
            price,
            quantity,
            unfilledQuantity,
            canceledQuantity,
            createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
