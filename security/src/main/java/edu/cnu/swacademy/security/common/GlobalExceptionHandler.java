package edu.cnu.swacademy.security.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
    log.info("SecurityException occurred. e : {}, msg : {}", e.getClass(), e.getMessage());
    HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(e.getErrorResponse().code()));
    return ResponseEntity.status(httpStatus).body(e.getErrorResponse());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Exception occurred. e : {}, msg : {}", e.getClass(), e.getMessage());
    ErrorCode internalServerError = ErrorCode.INTERNAL_SERVER_ERROR;
    HttpStatus httpStatus = HttpStatus.valueOf(Integer.parseInt(internalServerError.getCode()));
    return ResponseEntity.status(httpStatus).body(
        new ErrorResponse(
            internalServerError.getCode(),
            internalServerError.getMessage())
    );
  }
}
