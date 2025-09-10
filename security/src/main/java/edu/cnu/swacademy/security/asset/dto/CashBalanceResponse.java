package edu.cnu.swacademy.security.asset.dto;

public record CashBalanceResponse(
    int cashWalletId,
    int savings,
    int tiedSavings,
    int available
) {}
