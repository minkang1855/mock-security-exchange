package edu.cnu.swacademy.security.order.dto;

/**
 * Exchange 서버로 보내는 주문 취소 요청 DTO
 */
public record ExchangeOrderCancelRequest(
    int orderId,
    int productId,
    String side,
    int price
) {}
