package edu.cnu.swacademy.security.asset.dto;

import java.util.List;

public record CashWalletHistoriesResponse(
    int totalElements,
    List<CashWalletHistoryResponse> rows
) {}
