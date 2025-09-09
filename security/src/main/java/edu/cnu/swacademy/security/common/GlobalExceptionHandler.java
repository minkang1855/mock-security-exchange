package edu.cnu.swacademy.security.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
    HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(e.getErrorResponse().code()));
    return ResponseEntity.status(httpStatus).body(e.getErrorResponse());
  }
}
