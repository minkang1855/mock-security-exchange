package edu.cnu.swacademy.security.common;

import lombok.Getter;

@Getter
public class SecurityException extends Exception {

  private final ErrorCode errorCode;
  private final ErrorResponse errorResponse;

  public SecurityException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
  }

  public SecurityException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
    this.errorResponse = new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
  }
}
