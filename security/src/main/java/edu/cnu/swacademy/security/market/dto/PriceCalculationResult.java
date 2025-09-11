package edu.cnu.swacademy.security.market.dto;

import java.math.BigDecimal;

/**
 * 가격 계산 결과 DTO
 * 다음 거래일의 기준가, 상한가, 하한가를 포함합니다.
 */
public record PriceCalculationResult(
    BigDecimal referencePrice,    // 기준가 (전일 거래량가중평균가격)
    BigDecimal upperLimitPrice,   // 상한가 (기준가 × 1.05)
    BigDecimal lowerLimitPrice    // 하한가 (기준가 × 0.95)
) {
}
