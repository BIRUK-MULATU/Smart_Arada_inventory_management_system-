package com.example.inventory.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details) {

  public ApiError(Instant timestamp, int status, String error, String message, String path) {
    this(timestamp, status, error, message, path, List.of());
  }
}
