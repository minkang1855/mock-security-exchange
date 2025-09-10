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
  CASH_WALLET_NOT_FOUND("404", "Cash wallet not found."),

  EMAIL_ALREADY_EXISTS("409", "Email already exists."),
  WALLET_ALREADY_EXISTS("409", "Wallet already exists."),

  CASH_WALLET_BLOCKED("412", "Cash wallet is blocked."),
  INSUFFICIENT_BALANCE("428", "Insufficient balance."),

  PASSWORD_HASHING_FAILED("500", "Password hashing failed."),
  JWT_TOKEN_PARSE_FAILED("500", "JWT token parse failed."),
  ENCRYPTION_FAILED("500", "Encryption failed."),
  DECRYPTION_FAILED("500", "Decryption failed."),
  ACCOUNT_NUMBER_GENERATION_FAILED("500", "Failed to generate unique account number.");

  private final String code;
  private final String message;
}