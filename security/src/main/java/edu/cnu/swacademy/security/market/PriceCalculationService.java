package edu.cnu.swacademy.security.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.market.dto.PriceCalculationResult;
import edu.cnu.swacademy.security.stock.Stock;
import edu.cnu.swacademy.security.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 가격 계산 서비스
 * 당일 거래 결과를 기반으로 다음 거래일의 기준가, 상한가, 하한가를 계산합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PriceCalculationService {

  private final MarketStatusRepository marketStatusRepository;
  private final StockRepository stockRepository;

  private static final BigDecimal PRICE_LIMIT_PERCENTAGE = new BigDecimal("0.05"); // 5%
  private static final int PRICE_SCALE = 0; // 소수점 0자리
  private static final int TEMPORARY_PREVIOUS_CLOSE_PRICE = 30000;

  /**
   * 각 종목별 다음 거래일의 기준가, 상한가, 하한가를 계산하고 MarketStatus에 저장합니다.
   *
   * @param tradingDate 거래일
   */
  @Transactional(rollbackFor = Exception.class)
  public void createMarketStatus(LocalDate tradingDate) {
    log.info("Starting price calculation for all stocks on trading date: {}", tradingDate);

    // 1. 모든 종목 조회
    List<Stock> allStocks = stockRepository.findAll();

    if (allStocks.isEmpty()) {
      log.warn("No stocks found in the system");
      return;
    }

    log.info("Found {} stocks to calculate prices for", allStocks.size());

    int successCount = 0;
    int failCount = 0;

    // 2. 각 종목별로 기준가, 상하한가 계산 및 저장
    for (Stock stock : allStocks) {
      try {
        PriceCalculationResult priceCalculationResult = calculateNextDayPrices(stock.getId(), tradingDate);
        if (priceCalculationResult != null) {
          marketStatusRepository.save(
              new MarketStatus(
                  stock,
                  tradingDate,
                  priceCalculationResult.referencePrice().intValue(),
                  priceCalculationResult.upperLimitPrice().intValue(),
                  priceCalculationResult.lowerLimitPrice().intValue()
              )
          );
          successCount++;
          log.info("Successfully calculated prices for stock-id(={}), trading-date(={})", stock.getId(), tradingDate);
        }
      } catch (Exception e) {
        failCount++;
        log.error("Failed to calculate prices for stock-id(={}). e : {}, msg : {}",
            stock.getId(), e.getClass(), e.getMessage());
      }
    }

    log.info("Price calculation completed - Success: {}, Failed: {}", successCount, failCount);
  }

  /**
   * 전일 거래 결과를 기반으로 다음 거래일의 기준가, 상한가, 하한가를 계산합니다.
   * 
   * @param stockId 종목 ID
   * @param tradingDate 거래일
   * @return 가격 계산 결과 (기준가, 상한가, 하한가)
   * @throws SecurityException 계산 실패 시 발생
   */
  private PriceCalculationResult calculateNextDayPrices(int stockId, LocalDate tradingDate) throws SecurityException {
    log.info("Calculating next day prices for trading date: {}", tradingDate);
    
    try {
      // 당일날 생성된 MarketStatus가 있는지 확인
      boolean existTodayMarketStatus = marketStatusRepository.existsByStockIdAndTradingDate(stockId, tradingDate);

      if (!existTodayMarketStatus) {
        Optional<MarketStatus> beforeMarketStatusOptional = marketStatusRepository.findByStockIdAndTradingDate(stockId, tradingDate.minusDays(1));

        if (beforeMarketStatusOptional.isEmpty()) {
          // 전일 MarketStatus가 없는 경우 가상의 전일 종가(30,000원)를 기준가로 사용
          log.warn("No trading data found for date: {}", tradingDate);
          return calculatePricesWithPreviousClose(tradingDate);
        } else {
          // 전일 MarketStatus가 있는 경우 거래량가중평균가격(직전일의 총 거래대금 ÷ 총 거래량)을 다음 날 기준가로 삼음
          MarketStatus beforeMarketStatus = beforeMarketStatusOptional.get();

          // 거래량가중평균가격(VWAP) 계산
          BigDecimal vwap = BigDecimal.valueOf(beforeMarketStatus.getTradingAmount()).divide(BigDecimal.valueOf(beforeMarketStatus.getTradingVolume()), 0, RoundingMode.CEILING);

          // 기준가를 틱 사이즈에 맞게 조정
          BigDecimal adjustedReferencePrice = TickSizeUtil.validateAndAdjustTickSize(vwap);

          // 상한가, 하한가 계산
          BigDecimal upperLimitPrice = calculateUpperLimitPrice(adjustedReferencePrice);
          BigDecimal lowerLimitPrice = calculateLowerLimitPrice(adjustedReferencePrice);

          log.info("Price calculation completed - VWAP: {}, Upper: {}, Lower: {}", 
          adjustedReferencePrice, upperLimitPrice, lowerLimitPrice);

          return new PriceCalculationResult(adjustedReferencePrice, upperLimitPrice, lowerLimitPrice);
        }
      }
      // MarketStatus는 하루에 한 번만 생성하면 되므로 이미 있는 경우 별도 계산 X
      return null;
    } catch (Exception e) {
      log.error("Failed to calculate next day prices for date: {}. e: {}, msg: {}", tradingDate, e.getClass(), e.getMessage());
      throw new SecurityException(ErrorCode.FAILED_CALCULATE_NEXT_PRICES, e);
    }
  }
  
  /**
   * 상한가 계산
   * 상한가 = 기준가 × (1.05%)
   * 
   * @param referencePrice 기준가
   * @return 상한가 (틱 사이즈에 맞게 조정됨)
   */
  private BigDecimal calculateUpperLimitPrice(BigDecimal referencePrice) {
    BigDecimal upperLimit = referencePrice.multiply(BigDecimal.ONE.add(PRICE_LIMIT_PERCENTAGE))
        .setScale(PRICE_SCALE, RoundingMode.CEILING);
    
    // 틱 사이즈에 맞게 조정 (상한가는 올림 방향으로)
    return TickSizeUtil.validateAndAdjustTickSize(upperLimit);
  }
  
  /**
   * 하한가 계산
   * 하한가 = 기준가 × (0.95%)
   * 
   * @param referencePrice 기준가
   * @return 하한가 (틱 사이즈에 맞게 조정됨)
   */
  private BigDecimal calculateLowerLimitPrice(BigDecimal referencePrice) {
    BigDecimal lowerLimit = referencePrice.multiply(BigDecimal.ONE.subtract(PRICE_LIMIT_PERCENTAGE))
        .setScale(PRICE_SCALE, RoundingMode.FLOOR);
    
    // 틱 사이즈에 맞게 조정 (하한가는 내림 방향으로)
    return TickSizeUtil.validateAndAdjustTickSize(lowerLimit);
  }
  
  /**
   * 거래 데이터가 없는 경우 가상의 전일 종가(30,000원)를 기준가로 사용
   * 
   * @param tradingDate 거래일
   * @return 가격 계산 결과
   */
  private PriceCalculationResult calculatePricesWithPreviousClose(LocalDate tradingDate) {
    log.info("Using previous close price as reference for date: {}", tradingDate);

    BigDecimal previousClose = BigDecimal.valueOf(TEMPORARY_PREVIOUS_CLOSE_PRICE);
    
    // 기준가도 틱 사이즈에 맞게 조정
    BigDecimal adjustedReferencePrice = TickSizeUtil.validateAndAdjustTickSize(previousClose);
    
    BigDecimal upperLimitPrice = calculateUpperLimitPrice(adjustedReferencePrice);
    BigDecimal lowerLimitPrice = calculateLowerLimitPrice(adjustedReferencePrice);
    
    log.info("Tick size adjusted - Original: {}, Adjusted: {}", previousClose, adjustedReferencePrice);
    
    return new PriceCalculationResult(adjustedReferencePrice, upperLimitPrice, lowerLimitPrice);
  }
}
