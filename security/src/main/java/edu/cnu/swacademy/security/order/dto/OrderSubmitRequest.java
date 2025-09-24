package edu.cnu.swacademy.security.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 주문 접수 요청 DTO
 */
public record OrderSubmitRequest(
    @JsonProperty("stock_id")
    @Min(value = 1)
    int stockId,
    
    @NotBlank
    String side,
    
    @Min(value = 1)
    int price,
    
    @Min(value = 1)
    int quantity
) {
}
