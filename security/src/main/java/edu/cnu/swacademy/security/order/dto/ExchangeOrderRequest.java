package edu.cnu.swacademy.security.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Exchange 서버로 전송할 주문 요청 DTO
 */
public record ExchangeOrderRequest(
    @JsonProperty("order_id")
    int orderId,
    @JsonProperty("stock_id")
    int stockId,
    int price,
    int amount,
    String side,
    @JsonProperty("created_at")
    LocalDateTime createdAt
) {
}

