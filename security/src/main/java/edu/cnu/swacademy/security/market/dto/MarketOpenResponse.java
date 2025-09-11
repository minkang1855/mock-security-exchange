package edu.cnu.swacademy.security.market.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record MarketOpenResponse(
    String engineStatus,
    String openedAt
) {
  public MarketOpenResponse(String engineStatus, LocalDateTime openedAt) {
    this(engineStatus, openedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }
}
