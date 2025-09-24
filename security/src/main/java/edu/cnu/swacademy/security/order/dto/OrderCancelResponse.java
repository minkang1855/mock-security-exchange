package edu.cnu.swacademy.security.order.dto;

/**
 * 주문 취소 응답 DTO
 */
public record OrderCancelResponse(
    String message
) {
    public static OrderCancelResponse success() {
        return new OrderCancelResponse("주문이 성공적으로 취소되었습니다.");
    }
}
