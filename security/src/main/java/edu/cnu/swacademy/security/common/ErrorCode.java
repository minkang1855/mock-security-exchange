package edu.cnu.swacademy.security.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor  
public enum ErrorCode {

  INVALID_CREDENTIALS("401", "Invalid credentials."),
  UNAUTHORIZED("401", "Unauthorized"),

  REFRESH_TOKEN_INVALID("403", "Refresh token is invalid."),

  USER_NOT_FOUND("404", "User not found."),
  REFRESH_TOKEN_NOT_FOUND("404", "Refresh token not found."),

  EMAIL_ALREADY_EXISTS("409", "Email already exists."),

  PASSWORD_HASHING_FAILED("500", "Password hashing failed."),
  JWT_TOKEN_PARSE_FAILED("500", "JWT token parse failed.");

  private final String code;
  private final String message;
}