package edu.cnu.swacademy.exchange.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 주문 처리 요청 DTO
 */
public record OrderProcessRequest(
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
