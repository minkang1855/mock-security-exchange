package edu.cnu.swacademy.security.orderbook.dto;

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
}
