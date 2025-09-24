package edu.cnu.swacademy.exchange.order.dto;

import java.util.List;

/**
 * 주문 처리 응답 DTO
 */
public record OrderProcessResponse(
    String matchResult,
    Integer takerOrderId,
    List<MakerOrderResponse> makers,
    Integer price,
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
}

