package edu.cnu.swacademy.security.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exchange 서버로 보내는 주문 취소 요청 DTO
 */
public record ExchangeOrderCancelRequest(
    @JsonProperty("order_id")
    int orderId,
    @JsonProperty("stock_id")
    int stockId,
    String side,
    int price
) {}
