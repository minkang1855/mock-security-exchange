package edu.cnu.swacademy.security.order;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.cnu.swacademy.security.order.dto.MatchesResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
public class MatchController {

  private final MatchService matchService;

  /**
   * 체결 내역 조회
   * 사용자가 거래소에 접수한 주문 중 체결된 주문을 조회합니다.
   * 
   * @param request HTTP 요청 (JWT 토큰에서 사용자 ID 추출)
   * @param stockId 종목 ID (선택사항)
   * @param side 매수/매도 방향 (선택사항)
   * @param page 페이지 번호 (기본값: 0)
   * @param size 페이지 크기 (기본값: 10)
   * @param sort 정렬 방향 (기본값: desc)
   * @return 체결 내역 목록
   */
  @GetMapping
  public MatchesResponse getMatches(
      HttpServletRequest request,
      @RequestParam(required = false) Integer stockId,
      @RequestParam(required = false) String side,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "desc") String sort
  ) {
    // JWT에서 사용자 ID 추출
    int userId = (int) request.getAttribute("user_id");

    // 정렬 방향 검증 및 설정
    Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

    return matchService.getMatches(userId, stockId, side, pageable);
  }
}
