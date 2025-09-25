package edu.cnu.swacademy.security.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exchange 서버로부터 받는 주문 취소 응답 DTO
 */
public record ExchangeOrderCancelResponse(
    @JsonProperty("match_result")
    String matchResult // Cancelled, Rejected
) {
}
