package edu.cnu.swacademy.security.market;

import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.market.dto.MarketCloseResponse;
import edu.cnu.swacademy.security.market.dto.MarketOpenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {

  private final MarketService marketService;

  /**
   * 장 시작
   * 거래소 서버 프로세스를 시작하여 사용자의 주문을 접수할 수 있는 상태로 전환합니다.
   * 
   * @return 장 시작 응답 (엔진 상태, 개장 시각)
   * @throws SecurityException 이미 개장된 경우 발생
   */
  @PostMapping("/open")
  public MarketOpenResponse openMarket() throws SecurityException {
    return marketService.openMarket();
  }

  /**
   * 장 종료
   * 거래소 서버 프로세스를 종료하여 더 이상 새로운 주문을 받지 않도록 합니다.
   * 종료 시점에 당일 거래 결과를 기반으로 다음 거래일의 기준가, 상한가, 하한가를 계산합니다.
   * 
   * @return 장 종료 응답 (엔진 상태, 개장 시각)
   * @throws SecurityException 이미 종료된 경우 발생
   */
  @PostMapping("/close")
  public MarketCloseResponse closeMarket() throws SecurityException {
    return marketService.closeMarket();
  }
}
