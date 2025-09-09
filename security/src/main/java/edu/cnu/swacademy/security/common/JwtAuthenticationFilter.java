package edu.cnu.swacademy.security.common;

import java.io.IOException;

import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter implements Filter {

  private final JwtUtil jwtUtil;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    try {
      String authHeader = httpRequest.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        log.warn("JwtAuthenticationFilter invalid auth header.");
        sendUnauthorizedResponse(httpResponse);
        return;
      }

      String token = authHeader.substring(7);

      if (!jwtUtil.validateToken(token)) {
        log.warn("JwtAuthenticationFilter invalid token.");
        sendUnauthorizedResponse(httpResponse);
        return;
      }

      Long userId = jwtUtil.getUserIdFromToken(token);
      log.info("JwtAuthenticationFilter got user id (= {})", userId);
      httpRequest.setAttribute("user_id", userId);

      chain.doFilter(request, response);
    } catch (Exception e) {
      log.warn("JwtAuthenticationFilter exception. e = {}, msg = {}", e.getClass(), e.getMessage());
      sendUnauthorizedResponse(httpResponse);
    }
  }

  private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
    ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter()
        .write(
            String.format("{\"code\":\"%s\",\"message\":\"%s\"}",
                errorCode.getCode(), errorCode.getMessage())
        );
  }
}
