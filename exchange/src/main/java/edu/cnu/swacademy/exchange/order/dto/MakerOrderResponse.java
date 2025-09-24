package edu.cnu.swacademy.exchange.order.dto;

public record MakerOrderResponse(
    Integer orderId,
    Integer matchedAmount
) {
}
