package edu.cnu.swacademy.security.auth.dto;

import edu.cnu.swacademy.security.common.TokenInfo;

import java.time.format.DateTimeFormatter;

public record LoginResponse(
    String accessToken,
    String accessTokenExpiredAt,
    String refreshToken,
    String refreshTokenExpiredAt
) {

  public LoginResponse(TokenInfo tokenInfo) {
    this(
        tokenInfo.accessToken(),
        tokenInfo.accessTokenExpiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        tokenInfo.refreshToken(),
        tokenInfo.refreshTokenExpiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    );
  }
}