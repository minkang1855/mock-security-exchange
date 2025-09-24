package edu.cnu.swacademy.security.order.dto;

import java.util.List;

/**
 * Exchange 서버로부터 받은 주문 응답 DTO
 */
public record ExchangeOrderResponse(
    String matchResult,
    Integer takerOrderId,
    List<MakerOrderResponse> makers,
    Integer price,
    Integer totalMatchedAmount,
    String reason
) {
}

