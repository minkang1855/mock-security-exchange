package edu.cnu.swacademy.security.market;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 틱 사이즈 계산 유틸리티
 * 가격 구간에 따른 호가 단위를 계산하고 적용합니다.
 */
public class TickSizeUtil {

  /**
   * 가격 구간별 호가 단위
   */
  private static final BigDecimal TICK_SIZE_1 = new BigDecimal("1");      // 2,000원 미만
  private static final BigDecimal TICK_SIZE_5 = new BigDecimal("5");      // 2,000원 이상 5,000원 미만
  private static final BigDecimal TICK_SIZE_10 = new BigDecimal("10");    // 5,000원 이상 20,000원 미만
  private static final BigDecimal TICK_SIZE_50 = new BigDecimal("50");    // 20,000원 이상 50,000원 미만
  private static final BigDecimal TICK_SIZE_100 = new BigDecimal("100");  // 50,000원 이상 200,000원 미만
  private static final BigDecimal TICK_SIZE_500 = new BigDecimal("500");  // 200,000원 이상 500,000원 미만
  private static final BigDecimal TICK_SIZE_1000 = new BigDecimal("1000"); // 500,000원 이상

  /**
   * 가격 구간별 경계값
   */
  private static final BigDecimal PRICE_2000 = new BigDecimal("2000");
  private static final BigDecimal PRICE_5000 = new BigDecimal("5000");
  private static final BigDecimal PRICE_20000 = new BigDecimal("20000");
  private static final BigDecimal PRICE_50000 = new BigDecimal("50000");
  private static final BigDecimal PRICE_200000 = new BigDecimal("200000");
  private static final BigDecimal PRICE_500000 = new BigDecimal("500000");

  /**
   * 가격에 맞는 틱 사이즈를 반환합니다.
   * 
   * @param price 가격
   * @return 해당 가격 구간의 틱 사이즈
   */
  public static BigDecimal getTickSize(BigDecimal price) {
    if (price.compareTo(PRICE_2000) < 0) {
      return TICK_SIZE_1;
    } else if (price.compareTo(PRICE_5000) < 0) {
      return TICK_SIZE_5;
    } else if (price.compareTo(PRICE_20000) < 0) {
      return TICK_SIZE_10;
    } else if (price.compareTo(PRICE_50000) < 0) {
      return TICK_SIZE_50;
    } else if (price.compareTo(PRICE_200000) < 0) {
      return TICK_SIZE_100;
    } else if (price.compareTo(PRICE_500000) < 0) {
      return TICK_SIZE_500;
    } else {
      return TICK_SIZE_1000;
    }
  }

  /**
   * 가격을 틱 사이즈에 맞게 조정하고 검증합니다.
   * 
   * @param price 조정할 가격
   * @return 틱 사이즈에 맞게 조정된 가격
   */
  public static BigDecimal validateAndAdjustTickSize(BigDecimal price) {
    if (isValidTickSize(price)) {
      return price;
    } else {
      return adjustToTickSize(price);
    }
  }

  /**
   * 가격이 틱 사이즈에 맞는지 검증합니다.
   * 
   * @param price 검증할 가격
   * @return 틱 사이즈에 맞으면 true, 아니면 false
   */
  private static boolean isValidTickSize(BigDecimal price) {
    BigDecimal tickSize = getTickSize(price);
    BigDecimal remainder = price.remainder(tickSize);
    return remainder.compareTo(BigDecimal.ZERO) == 0;
  }

  /**
   * 가격을 틱 사이즈에 맞게 조정합니다.
   * 가격을 틱 사이즈로 나누고 반올림한 후 다시 틱 사이즈를 곱함
   *  예시 1)
   *    price = 12345
   *    tickSize = 10
   *    1단계: 12345 ÷ 10 = 1234 (반올림)
   *    2단계: 1234 × 10 = 12340
   *    결과: 12340원 (10원의 배수)
   *  ----------------------------
   *  예시 2)
   *    price = 1999
   *    tickSize = 1
   *    1단계: 1999 ÷ 1 = 1999 (반올림)
   *    2단계: 1999 × 1 = 1999
   *    결과: 1999원 (1원의 배수)
   * @param price 조정할 가격
   * @return 틱 사이즈에 맞게 조정된 가격
   */
  private static BigDecimal adjustToTickSize(BigDecimal price) {
    BigDecimal tickSize = getTickSize(price);

    return price.divide(tickSize, 0, RoundingMode.HALF_UP)
        .multiply(tickSize);
  }  
}
