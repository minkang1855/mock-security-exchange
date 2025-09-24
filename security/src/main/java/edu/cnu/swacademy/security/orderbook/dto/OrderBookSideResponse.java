package edu.cnu.swacademy.security.orderbook.dto;

import java.util.Map;

/**
 * 오더북 매수/매도 측면 응답 DTO
 */
public record OrderBookSideResponse(
    Map<String, OrderBookPriceResponse> prices
) {}
