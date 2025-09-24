package edu.cnu.swacademy.exchange.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 오더북 엔트리 DTO
 */
@Getter
@AllArgsConstructor
public class OrderBookEntry {
    private int orderId;
    private int unfilledUnit;
    private LocalDateTime createdAt;

    public static OrderBookEntry of(int orderId, int unfilledUnit, LocalDateTime createDate) {
        return new OrderBookEntry(orderId, unfilledUnit, createDate);
    }

    public void decreaseAmount(int matchedAmount) {
        this.unfilledUnit -= matchedAmount;
    }
}
