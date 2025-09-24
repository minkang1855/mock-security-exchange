package edu.cnu.swacademy.exchange.order.dto;

/**
 * 주문 취소 요청 DTO
 */
public record OrderCancelRequest(
    int orderId,
    int stockId,
    String side,
    int price
) {}
