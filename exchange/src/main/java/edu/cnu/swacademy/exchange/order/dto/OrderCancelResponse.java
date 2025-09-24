package edu.cnu.swacademy.exchange.order.dto;

/**
 * 주문 취소 응답 DTO
 */
public record OrderCancelResponse(
    String matchResult // Cancelled, Rejected
) {
    public static OrderCancelResponse cancelled() {
        return new OrderCancelResponse("Cancelled");
    }
    
    public static OrderCancelResponse rejected() {
        return new OrderCancelResponse("Rejected");
    }
}
