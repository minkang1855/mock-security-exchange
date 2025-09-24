package edu.cnu.swacademy.security.orderbook.dto;

import java.time.LocalDateTime;

/**
 * 오더북 주문 정보 응답 DTO
 */
public record OrderBookOrderResponse(
    int orderId,
    int unfilledQuantity,
    LocalDateTime createdAt
) {}
