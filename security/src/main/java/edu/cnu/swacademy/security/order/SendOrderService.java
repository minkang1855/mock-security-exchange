package edu.cnu.swacademy.security.order;

import edu.cnu.swacademy.security.common.ErrorCode;
import edu.cnu.swacademy.security.common.SecurityException;
import edu.cnu.swacademy.security.order.dto.ExchangeOrderCancelRequest;
import edu.cnu.swacademy.security.order.dto.ExchangeOrderCancelResponse;
import edu.cnu.swacademy.security.order.dto.ExchangeOrderRequest;
import edu.cnu.swacademy.security.order.dto.ExchangeOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * Exchange 서버와의 주문 통신 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendOrderService {

    private final RestTemplate restTemplate;

    @Value("${exchange.server.host:localhost}")
    private String exchangeServerHost;

    @Value("${exchange.server.port:8081}")
    private int exchangeServerPort;

    /**
     * Exchange 서버로 주문 전송
     */
    public ExchangeOrderResponse sendOrderToExchange(int orderId, int stockId, int price, int amount, String side, LocalDateTime createdAt) throws SecurityException {
        try {
            String url = String.format("http://%s:%d/api/v1/market/order", exchangeServerHost, exchangeServerPort);
            
            ExchangeOrderRequest request = new ExchangeOrderRequest(
                orderId,
                stockId,
                price,
                amount,
                side,
                createdAt
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ExchangeOrderRequest> entity = new HttpEntity<>(request, headers);

            log.info("Sending order to exchange server: orderId={}, stockWalletId={}, side={}", orderId, stockId, side);

            ResponseEntity<ExchangeOrderResponse> response = restTemplate.postForEntity(url, entity, ExchangeOrderResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExchangeOrderResponse orderResponse = response.getBody();
                log.info("Order sent to exchange successfully: orderId={}, matchResult={}", 
                    orderId, orderResponse.matchResult());
                return orderResponse;
            } else {
                log.error("Failed to send order to exchange: orderId={}, status={}", 
                    orderId, response.getStatusCode());
                throw new SecurityException(ErrorCode.EXCHANGE_SERVER_COMMUNICATION_FAILED);
            }

        } catch (Exception e) {
            log.error("Error sending order to exchange server: orderId={}, error={}", 
                orderId, e.getMessage(), e);
            throw new SecurityException(ErrorCode.EXCHANGE_SERVER_COMMUNICATION_FAILED, e);
        }
    }

    /**
     * Exchange 서버로 주문 취소 요청 전송
     */
    public ExchangeOrderCancelResponse cancelOrderToExchange(int orderId, int productId, String side, int price) throws SecurityException {
        try {
            String url = String.format("http://%s:%d/api/v1/market/order", exchangeServerHost, exchangeServerPort);
            ExchangeOrderCancelRequest request = new ExchangeOrderCancelRequest(orderId, productId, side, price);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ExchangeOrderCancelRequest> entity = new HttpEntity<>(request, headers);

            log.info("Sending order cancellation to exchange server: orderId={}, productId={}, side={}, price={}", orderId, productId, side, price);

            ResponseEntity<ExchangeOrderCancelResponse> response = restTemplate.exchange(
                url, HttpMethod.DELETE, entity, ExchangeOrderCancelResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExchangeOrderCancelResponse cancelResponse = response.getBody();
                log.info("Order cancellation sent to exchange successfully: orderId={}, matchResult={}", 
                    orderId, cancelResponse.matchResult());
                return cancelResponse;
            } else {
                log.error("Failed to send order cancellation to exchange: orderId={}, status={}", 
                    orderId, response.getStatusCode());
                throw new SecurityException(ErrorCode.EXCHANGE_SERVER_COMMUNICATION_FAILED);
            }

        } catch (Exception e) {
            log.error("Error sending order cancellation to exchange server: orderId={}, error={}", 
                orderId, e.getMessage(), e);
            throw new SecurityException(ErrorCode.EXCHANGE_SERVER_COMMUNICATION_FAILED, e);
        }
    }
}

