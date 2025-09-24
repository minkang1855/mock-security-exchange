package edu.cnu.swacademy.security.orderbook.dto;

import java.util.List;

/**
 * 오더북 가격별 정보 응답 DTO
 */
public record OrderBookPriceResponse(
    int totalQuantity,
    List<OrderBookOrderResponse> orders
) {}
