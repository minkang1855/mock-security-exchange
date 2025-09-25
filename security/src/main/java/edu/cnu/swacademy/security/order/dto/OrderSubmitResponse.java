package edu.cnu.swacademy.security.order.dto;

/**
 * 주문 접수 응답 DTO
 */
public record OrderSubmitResponse(
    Integer orderId,
    String reason
) {
    public static OrderSubmitResponse success(Integer orderId) {
        return new OrderSubmitResponse(orderId, null);
    }
    
    public static OrderSubmitResponse rejected(String reason) {
        return new OrderSubmitResponse(null, reason);
    }
}
