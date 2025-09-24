package edu.cnu.swacademy.security.order.dto;

public record MakerOrderResponse(
    Integer orderId,
    Integer matchedAmount
) {
}
