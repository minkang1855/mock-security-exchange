package edu.cnu.swacademy.security.order;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import edu.cnu.swacademy.security.order.dto.MatchResponse;
import edu.cnu.swacademy.security.order.dto.MatchesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 체결 서비스
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MatchService {

  private final OrderRepository orderRepository;

  /**
   * 사용자의 체결 내역 목록을 조회합니다.
   * 
   * @param userId 사용자 ID
   * @param stockId 종목 ID (선택사항)
   * @param side 주문 방향 (선택사항)
   * @param pageable 페이징 정보
   * @return 체결 내역 목록
   */
  public MatchesResponse getMatches(int userId, Integer stockId, String side, Pageable pageable) {
    // 조건에 따른 조회
    Page<Order> matchPage = getMatchesByConditions(userId, stockId, side, pageable);

    // DTO 변환
    List<MatchResponse> matchResponses = matchPage.getContent().stream()
        .map(this::convertToMatchResponse)
        .toList();

    log.info("Found {} matches for user-id(={})", matchPage.getTotalElements(), userId);
    return new MatchesResponse(matchPage.getTotalElements(), matchResponses);
  }

  /**
   * 조건에 따른 체결 내역 조회
   * 
   * @param userId 사용자 ID
   * @param stockId 종목 ID (선택사항)
   * @param side 주문 방향 (선택사항)
   * @param pageable 페이징 정보
   * @return 체결 내역 페이지
   */
  private Page<Order> getMatchesByConditions(int userId, Integer stockId, String side, Pageable pageable) {
    if (stockId != null && side != null) {
      OrderSide orderSide = OrderSide.valueOf(side.toUpperCase());
      return orderRepository.findMatchedByUserIdAndStockIdAndSide(userId, stockId, orderSide, pageable);
    } else if (stockId != null) {
      return orderRepository.findMatchedByUserIdAndStockId(userId, stockId, pageable);
    } else if (side != null) {
      OrderSide orderSide = OrderSide.valueOf(side.toUpperCase());
      return orderRepository.findMatchedByUserIdAndSide(userId, orderSide, pageable);
    } else {
      return orderRepository.findMatchedByUserId(userId, pageable);
    }
  }

  /**
   * Order 엔티티를 MatchResponse DTO로 변환
   * 
   * @param order Order 엔티티
   * @return MatchResponse DTO
   */
  private MatchResponse convertToMatchResponse(Order order) {
    return new MatchResponse(
        order.getStock().getId(),
        order.getSide(),
        order.getPrice(),
        order.getAmount(),
        order.getUnfilledAmount(),
        order.getUpdatedAt()
    );
  }
}
