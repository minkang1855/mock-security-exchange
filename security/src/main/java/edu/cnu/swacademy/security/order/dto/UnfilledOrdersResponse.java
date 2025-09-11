package edu.cnu.swacademy.security.order.dto;

import java.util.List;

/**
 * 미체결 주문 목록 응답 DTO
 */
public record UnfilledOrdersResponse(
    long totalElements,
    List<UnfilledOrderResponse> rows
) {
}
