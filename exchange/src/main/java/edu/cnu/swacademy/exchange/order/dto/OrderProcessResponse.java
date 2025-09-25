package edu.cnu.swacademy.exchange.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 주문 처리 응답 DTO
 */
public record OrderProcessResponse(
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
    public static OrderProcessResponse unmatched() {
        return new OrderProcessResponse("Unmatched", null, null, null, null, null);
    }
    
    public static OrderProcessResponse matched(int takerOrderId, List<MakerOrderResponse> makers, int price, int totalMatchedAmount) {
        return new OrderProcessResponse("Matched", takerOrderId, makers, price, totalMatchedAmount, null);
    }
    
    public static OrderProcessResponse rejected(String reason) {
        return new OrderProcessResponse("Rejected", null, null, null, null, reason);
    }
    
    public static OrderProcessResponse cancelled() {
        return new OrderProcessResponse("Cancelled", null, null, null, null, null);
    }
}

