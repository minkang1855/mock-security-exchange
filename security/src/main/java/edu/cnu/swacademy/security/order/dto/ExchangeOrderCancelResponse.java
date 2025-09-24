package edu.cnu.swacademy.security.order.dto;

/**
 * Exchange 서버로부터 받는 주문 취소 응답 DTO
 */
public record ExchangeOrderCancelResponse(
    String matchResult // Cancelled, Rejected
) {
}
