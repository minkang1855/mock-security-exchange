package edu.cnu.swacademy.security.orderbook.dto;

/**
 * 오더북 조회 응답 DTO
 */
public record OrderBookResponse(
    OrderBookSideResponse buy,
    OrderBookSideResponse sell
) {}
