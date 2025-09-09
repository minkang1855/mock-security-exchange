package edu.cnu.swacademy.security.common;

import lombok.Getter;

@Getter
public class SecurityException extends Exception {

  private final ErrorResponse errorResponse;

  public SecurityException(ErrorResponse errorResponse) {
    super(errorResponse.message());
    this.errorResponse = errorResponse;
  }

  public SecurityException(ErrorResponse errorResponse, Throwable cause) {
    super(errorResponse.message(), cause);
    this.errorResponse = errorResponse;
  }
}
