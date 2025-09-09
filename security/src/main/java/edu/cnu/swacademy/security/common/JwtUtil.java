package edu.cnu.swacademy.security.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Slf4j
@Component
public class JwtUtil {

  private final SecretKey secretKey;
  private final int accessTokenExpiredMinutes;
  private final int refreshTokenExpiredDays;

  public JwtUtil(
      @Value("${jwt.secret-key}") String secret,
      @Value("${jwt.access-token-expired-at}") int accessTokenExpiredAt,
      @Value("${jwt.refresh-token-expired-at}") int refreshTokenExpiredAt
  ) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    this.accessTokenExpiredMinutes = accessTokenExpiredAt;
    this.refreshTokenExpiredDays = refreshTokenExpiredAt;
  }

  public TokenInfo generateAccessAndRefreshToken(Long userId) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime accessTokenExpiredAt = now.plusMinutes(accessTokenExpiredMinutes);
    LocalDateTime refreshTokenExpiredAt = now.plusDays(refreshTokenExpiredDays);
    
    String accessToken = generateToken(userId, accessTokenExpiredAt);
    String refreshToken = generateToken(userId, refreshTokenExpiredAt);
    
    return new TokenInfo(
        accessToken,
        accessTokenExpiredAt,
        refreshToken,
        refreshTokenExpiredAt
    );
  }

  public String generateToken(Long userId, LocalDateTime expiredAt) {
    LocalDateTime now = LocalDateTime.now();

    return Jwts.builder()
        .subject(userId.toString())
        .issuedAt(Date.from(now.atZone(ZoneId.of("Asia/Seoul")).toInstant())) // UTC로 저장
        .expiration(Date.from(expiredAt.atZone(ZoneId.of("Asia/Seoul")).toInstant())) // UTC로 저장
        .signWith(secretKey)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      log.warn("failed to validate token. e = {}, msg = {}", e.getClass(), e.getMessage());
      return false;
    }
  }

 public Long getUserIdFromToken(String token) throws SecurityException {
   try {
     String subject = Jwts.parser()
         .verifyWith(secretKey)
         .build()
         .parseSignedClaims(token)
         .getPayload()
         .getSubject();
     return Long.parseLong(subject);
   } catch (Exception e) {
     throw new SecurityException(ErrorCode.JWT_TOKEN_PARSE_FAILED, e);
   }
 }
}