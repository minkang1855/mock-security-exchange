package edu.cnu.swacademy.exchange.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 오더북 엔트리 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookEntry {
    private int orderId;
    private int unfilledUnit;
    private LocalDateTime createdAt;

    public void decreaseAmount(int matchedAmount) {
        this.unfilledUnit -= matchedAmount;
    }
}
