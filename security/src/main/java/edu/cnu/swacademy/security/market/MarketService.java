package edu.cnu.swacademy.security.market;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.market.dto.MarketOpenResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MarketService {

  // 거래소 서버 프로세스 상태 관리
  private LocalDateTime openedAt;
  private Process exchangeServerProcess;

  @Value("${exchange.server.jar-path}")
  private String exchangeServerJarPath;

  @Value("${exchange.server.port}")
  private int exchangeServerPort;

  @Value("${spring.data.redis.host}")
  private String redisHost;

  @Value("${spring.data.redis.port}")
  private int redisPort;

  /**
   * 장 시작
   * 거래소 서버 프로세스를 시작하여 사용자의 주문을 접수할 수 있는 상태로 전환합니다.
   * 
   * @return 장 시작 응답 (엔진 상태, 개장 시각)
   * @throws SecurityException 이미 개장된 경우 발생
   */
  public MarketOpenResponse openMarket() throws SecurityException {
    // 1. 이미 개장된 상태인지 확인
    if (isMarketRunning()) {
      log.info("Market is already running");
      throw new SecurityException(ErrorCode.MARKET_ALREADY_OPEN);
    }

    // 2. 거래소 서버 프로세스 시작
    startExchangeServer();

    // 3. 상태 업데이트
    openedAt = LocalDateTime.now();

    log.info("Market opened successfully at {}", openedAt);

    return new MarketOpenResponse("RUNNING", openedAt);
  }

  /**
   * 거래소 서버 프로세스 시작
   * Exchange 서버 Spring Boot 애플리케이션 프로세스를 실행합니다.
   * 
   * @throws SecurityException 서버 시작 실패 시 발생
   */
  private void startExchangeServer() throws SecurityException {
    log.info("Starting exchange server process.");
    
    try {
      // ProcessBuilder를 사용하여 exchange 서버 실행
      ProcessBuilder processBuilder = new ProcessBuilder(
          "java", 
          "-jar",
          exchangeServerJarPath,
          "--server.port=" + exchangeServerPort
      );
      
      // Redis 환경 변수 설정
      processBuilder.environment().put("REDIS_HOST", redisHost);
      processBuilder.environment().put("REDIS_PORT", String.valueOf(redisPort));
      
      // 프로세스 실행
      exchangeServerProcess = processBuilder.start();
      
      // 프로세스가 정상적으로 시작되었는지 확인
      if (exchangeServerProcess.isAlive()) {
        log.info("Exchange server process started successfully immediately");
      } else {
        log.warn("Exchange server process not alive immediately, waiting and retrying...");
        
        // 짧은 대기 후 재확인 (최대 3회)
        int retry = 1;
        while (retry <= 3) {
          Thread.sleep(2000);

          if (exchangeServerProcess.isAlive()) {
            log.info("Exchange server process started successfully after {} retries", retry);
            break;
          }

          if (retry == 3) {
            log.error("Exchange server process failed to start after 3 attempts");
            throw new SecurityException(ErrorCode.EXCHANGE_SERVER_START_FAILED);
          }

          retry++;
        }
      }
    } catch (IOException e) {
      log.error("Failed to start exchange server process. e : {}, msg : {}", e.getClass(), e.getMessage());
      throw new SecurityException(ErrorCode.EXCHANGE_SERVER_START_FAILED, e);
    } catch (InterruptedException e) {
      log.error("Interrupted while starting exchange server process e : {}, msg : {}", e.getClass(), e.getMessage());
      throw new SecurityException(ErrorCode.EXCHANGE_SERVER_START_FAILED, e);
    }
  }

  /**
   * 거래소 서버 프로세스 상태 조회
   * 
   * @return 현재 거래소 서버 프로세스 상태
   */
  public boolean isMarketRunning() {
    return exchangeServerProcess != null && exchangeServerProcess.isAlive();
  }

  /**
   * 개장 시각 조회
   *
   * @return 개장 시각 (개장되지 않은 경우 null)
   */
  public LocalDateTime getOpenedAt() {
    return openedAt;
  }

  /**
   * 거래소 서버 프로세스 종료
   * 실행 중인 거래소 서버 프로세스를 안전하게 종료합니다.
   * 
   * @throws SecurityException 이미 종료된 상태인 경우 발생
   */
  public void shutdownExchangeServer() throws SecurityException {
    if (!isMarketRunning()) {
      log.info("Exchange server process is already stopped");
      throw new SecurityException(ErrorCode.MARKET_ALREADY_CLOSED);
    }
    
    log.info("Shutting down exchange server process");
    
    exchangeServerProcess.destroy();
    
    // 프로세스가 정상적으로 종료될 때까지 대기
    try {
      boolean terminated = exchangeServerProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
      if (!terminated) {
        log.warn("Exchange server process did not terminate gracefully, forcing shutdown");
        exchangeServerProcess.destroyForcibly(); // 강제 종료
      }
    } catch (Exception e) {
      log.error("Exception occurred while waiting for exchange server process to terminate. e : {}, msg : {}", e.getClass(), e.getMessage());
      exchangeServerProcess.destroyForcibly(); // 강제 종료
    }
    
    log.info("Exchange server process terminated.");
  }
}
