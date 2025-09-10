package edu.cnu.swacademy.security.asset.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record CashWalletHistoryResponse(
    int historyId,
    String category,
    int amount,
    String reason,
    int savings,
    String createdAt
) {
  public CashWalletHistoryResponse(
      int historyId,
      String category,
      int amount,
      String reason,
      int savings,
      LocalDateTime createdAt
  ) {
    this(
        historyId,
        category,
        amount,
        reason,
        savings,
        createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    );
  }
}
