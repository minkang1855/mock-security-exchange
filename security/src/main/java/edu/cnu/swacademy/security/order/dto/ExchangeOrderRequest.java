package edu.cnu.swacademy.security.order.dto;

import java.time.LocalDateTime;

/**
 * Exchange 서버로 전송할 주문 요청 DTO
 */
public record ExchangeOrderRequest(
    int orderId,
    int stockId,
    int price,
    int amount,
    String side,
    LocalDateTime createdAt
) {
}

