package edu.cnu.swacademy.security.order.dto;

import java.util.List;

/**
 * 체결 내역 목록 응답 DTO
 */
public record MatchesResponse(
    long totalElements,
    List<MatchResponse> rows
) {
}
