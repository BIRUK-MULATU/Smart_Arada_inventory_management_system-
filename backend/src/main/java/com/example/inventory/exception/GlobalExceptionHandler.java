package com.example.inventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiError> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    // @PreAuthorize denials throw AuthorizationDeniedException (a subtype of this) from inside
    // the MVC dispatch, so this handler runs instead of RestAccessDeniedHandler, which only
    // catches denials from the URL-matcher rules in SecurityConfig (thrown before dispatch).
    return build(
        HttpStatus.FORBIDDEN,
        "You do not have permission to access this resource",
        request,
        List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
    return build(
        HttpStatus.CONFLICT, "The request conflicts with existing data", request, List.of());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception on {}", request.getRequestURI(), ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, List.of());
  }

  private ResponseEntity<ApiError> build(
      HttpStatus status, String message, HttpServletRequest request, List<String> details) {
    ApiError body =
        new ApiError(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            details);
    return ResponseEntity.status(status).body(body);
  }
}
