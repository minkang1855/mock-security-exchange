package edu.cnu.swacademy.security.market.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.cnu.swacademy.security.market.EngineStatus;

public record MarketOpenResponse(
    String engineStatus,
    String openedAt
) {
  public MarketOpenResponse(EngineStatus engineStatus, LocalDateTime openedAt) {
    this(engineStatus.name(), openedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }
}
