package edu.cnu.swacademy.security.asset.dto;

import jakarta.validation.constraints.Min;

public record CashWithdrawalRequest(
    @Min(value = 1)
    int amount
) {}
