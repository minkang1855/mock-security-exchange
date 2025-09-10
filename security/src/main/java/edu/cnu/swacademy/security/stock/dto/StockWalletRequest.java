package edu.cnu.swacademy.security.stock.dto;

import jakarta.validation.constraints.Min;

public record StockWalletRequest(
    @Min(value = 1)
    int stockId
) {}
