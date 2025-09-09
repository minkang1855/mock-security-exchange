package edu.cnu.swacademy.security.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor  
public enum ErrorCode {

  EMAIL_ALREADY_EXISTS("409", "Email already exists."),
  USER_NOT_FOUND("404", "User not found."),
  INVALID_CREDENTIALS("401", "Invalid credentials."),
  JWT_TOKEN_GENERATION_FAILED("500", "JWT token generation failed."),

  PASSWORD_HASHING_FAILED("500", "Password hashing failed.");

  private final String code;
  private final String message;
}