package edu.cnu.swacademy.security.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor  
public enum ErrorCode {

  INVALID_CREDENTIALS("401", "Invalid credentials."),

  USER_NOT_FOUND("404", "User not found."),

  EMAIL_ALREADY_EXISTS("409", "Email already exists."),

  PASSWORD_HASHING_FAILED("500", "Password hashing failed."),
  JWT_TOKEN_PARSE_FAILED("500", "JWT token parse failed.");

  private final String code;
  private final String message;
}