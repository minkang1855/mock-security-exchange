package edu.cnu.swacademy.security.stock.dto;

import jakarta.validation.constraints.Min;

public record StockDepositRequest(
    @Min(value = 1)
    int stockWalletId,

    @Min(value = 1)
    int amount
) {}
