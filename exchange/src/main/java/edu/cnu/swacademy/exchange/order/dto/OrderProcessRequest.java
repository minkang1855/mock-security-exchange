package edu.cnu.swacademy.exchange.order.dto;

import java.time.LocalDateTime;

/**
 * 주문 처리 요청 DTO
 */
public record OrderProcessRequest(
    int orderId,
    int stockId,
    int price,
    int amount,
    String side,
    LocalDateTime createdAt
) {
}
