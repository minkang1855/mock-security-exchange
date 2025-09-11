package edu.cnu.swacademy.security.order.dto;

import edu.cnu.swacademy.security.order.OrderSide;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 체결 내역 응답 DTO
 */
public record MatchResponse(
    int stockId,
    OrderSide side,
    int price,
    int quantity,
    int unfilledQuantity,
    String createdAt
) {
    public MatchResponse(int stockId, OrderSide side, int price, int quantity, int unfilledQuantity, LocalDateTime createdAt) {
        this(stockId, side, price, quantity, unfilledQuantity, createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
