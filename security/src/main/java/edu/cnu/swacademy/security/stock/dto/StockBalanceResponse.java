package edu.cnu.swacademy.security.stock.dto;

public record StockBalanceResponse(
    int stockWalletId,
    int stockId,
    int reserve,
    int deposit,
    int available
) {}
