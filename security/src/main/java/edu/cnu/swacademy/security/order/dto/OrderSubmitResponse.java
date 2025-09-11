package edu.cnu.swacademy.security.order.dto;

/**
 * 주문 접수 응답 DTO
 */
public record OrderSubmitResponse(
    String reason
) {
    public static OrderSubmitResponse success() {
        return new OrderSubmitResponse(null);
    }
    
    public static OrderSubmitResponse rejected(String reason) {
        return new OrderSubmitResponse(reason);
    }
}
