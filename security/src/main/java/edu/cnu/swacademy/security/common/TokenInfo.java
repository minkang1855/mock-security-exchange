package edu.cnu.swacademy.security.common;

import java.time.LocalDateTime;

public record TokenInfo(
    String accessToken,
    LocalDateTime accessTokenExpiredAt,
    String refreshToken,
    LocalDateTime refreshTokenExpiredAt
) {}
