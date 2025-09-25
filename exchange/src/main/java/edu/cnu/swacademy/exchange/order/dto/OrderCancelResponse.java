package edu.cnu.swacademy.exchange.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 주문 취소 응답 DTO
 */
public record OrderCancelResponse(
    @JsonProperty("match_result")
    String matchResult // Cancelled, Rejected
) {
    public static OrderCancelResponse cancelled() {
        return new OrderCancelResponse("Cancelled");
    }
    
    public static OrderCancelResponse rejected() {
        return new OrderCancelResponse("Rejected");
    }
}
