package edu.cnu.swacademy.security.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Exchange 서버로부터 받은 주문 응답 DTO
 */
public record ExchangeOrderResponse(
    @JsonProperty("match_result")
    String matchResult,
    @JsonProperty("taker_order_id")
    Integer takerOrderId,
    List<MakerOrderResponse> makers,
    Integer price,
    @JsonProperty("total_matched_amount")
    Integer totalMatchedAmount,
    String reason
) {
}

