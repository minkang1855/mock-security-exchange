package edu.cnu.swacademy.security.market.dto;

import java.time.LocalDateTime;

import edu.cnu.swacademy.security.market.EngineStatus;

/**
 * 장 종료 응답 DTO
 */
public record MarketCloseResponse(
    String engineStatus,
    String openedAt
) {
    public MarketCloseResponse(EngineStatus engineStatus, LocalDateTime openedAt) {
        this(engineStatus.name(), openedAt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
