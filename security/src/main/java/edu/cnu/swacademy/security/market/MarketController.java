package edu.cnu.swacademy.security.market;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.market.dto.MarketOpenResponse;
import lombok.RequiredArgsConstructor;

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
}
