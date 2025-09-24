package edu.cnu.swacademy.security.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor  
public enum ErrorCode {

  INVALID_ORDER_SIDE("400", "Invalid order side"),
  ORDER_CANCEL_REJECTED("400", "Order cancel rejected."),
  INVALID_REQUEST("400", "Bad Request"),

  INVALID_CREDENTIALS("401", "Invalid credentials."),
  UNAUTHORIZED("401", "Unauthorized"),

  REFRESH_TOKEN_INVALID("403", "Refresh token is invalid."),
  ORDER_ACCESS_DENIED("403", "You can't cancel because it's not your own order."),

  USER_NOT_FOUND("404", "User not found."),
  REFRESH_TOKEN_NOT_FOUND("404", "Refresh token not found."),
  CASH_WALLET_NOT_FOUND("404", "Cash wallet not found."),
  STOCK_NOT_FOUND("404", "Stock not found."),
  STOCK_WALLET_NOT_FOUND("404", "Stock wallet not found."),
  ORDER_NOT_FOUND("404", "Order not found."),
  MARKET_STATUS_NOT_FOUND("404", "Market status not found."),

  EMAIL_ALREADY_EXISTS("409", "Email already exists."),
  CASH_WALLET_ALREADY_EXISTS("409", "Cash wallet already exists."),
  CASH_WALLET_ALREADY_BLOCKED("409", "Cash wallet is already blocked."),
  CASH_WALLET_ALREADY_UNBLOCKED("409", "Cash wallet is already unblocked."),
  STOCK_WALLET_ALREADY_EXISTS("409", "Stock wallet already exists."),
  STOCK_WALLET_ALREADY_BLOCKED("409", "Stock wallet is already blocked."),
  STOCK_WALLET_ALREADY_UNBLOCKED("409", "Stock wallet is already unblocked."),
  MARKET_ALREADY_OPEN("409", "Market is already open."),
  MARKET_ALREADY_CLOSED("409", "Market is already closed."),
  ORDER_ALREADY_CANCELLED("409", "Already canceled order."),

  CASH_WALLET_BLOCKED("412", "Cash wallet is blocked."),
  STOCK_WALLET_BLOCKED("412", "Stock wallet is blocked."),

  INVALID_TICK_SIZE("416", "Invalid tick size."),
  PRICE_OUT_OF_LIMITS("416", "Price is out of limits."),

  INSUFFICIENT_BALANCE("428", "Insufficient balance."),

  INTERNAL_SERVER_ERROR("500", "Internal server error."),
  PASSWORD_HASHING_FAILED("500", "Password hashing failed."),
  JWT_TOKEN_PARSE_FAILED("500", "JWT token parse failed."),
  ENCRYPTION_FAILED("500", "Encryption failed."),
  DECRYPTION_FAILED("500", "Decryption failed."),
  ACCOUNT_NUMBER_GENERATION_FAILED("500", "Failed to generate unique account number."),
  EXCHANGE_SERVER_START_FAILED("500", "Failed to start exchange server."),
  EXCHANGE_SERVER_COMMUNICATION_FAILED("500", "Failed to communicate with exchange server."),
  FAILED_CALCULATE_NEXT_PRICES("500", "Failed to calculate next prices.");

  private final String code;
  private final String message;
}