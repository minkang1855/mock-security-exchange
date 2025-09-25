package edu.cnu.swacademy.exchange.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주문 취소 요청 DTO
 */
public record OrderCancelRequest(
    @JsonProperty("order_id")
    int orderId,
    @JsonProperty("stock_id")
    int stockId,
    String side,
    int price
) {}
