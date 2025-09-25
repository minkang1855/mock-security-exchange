package edu.cnu.swacademy.security.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MakerOrderResponse(
    @JsonProperty("order_id")
    Integer orderId,
    @JsonProperty("matched_amount")
    Integer matchedAmount
) {
}
