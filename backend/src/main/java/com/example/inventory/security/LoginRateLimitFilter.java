package com.example.inventory.security;

import com.example.inventory.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Sliding-window rate limit on {@code POST /api/auth/login} only, keyed by client IP. Attempt
 * timestamps are kept in memory - fine for the single-instance deployment this app runs as; a
 * multi-instance deployment would need a shared store instead.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/api/auth/login";

  private final int maxAttempts;
  private final Duration window;
  private final ObjectMapper objectMapper;
  private final Map<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

  public LoginRateLimitFilter(
      @Value("${app.rate-limit.login-max-attempts}") int maxAttempts,
      @Value("${app.rate-limit.login-window-seconds}") long windowSeconds,
      ObjectMapper objectMapper) {
    this.maxAttempts = maxAttempts;
    this.window = Duration.ofSeconds(windowSeconds);
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    if (!isLoginRequest(request) || isAllowed(request.getRemoteAddr(), Instant.now())) {
      filterChain.doFilter(request, response);
      return;
    }
    writeTooManyRequests(response);
  }

  private boolean isLoginRequest(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod())
        && LOGIN_PATH.equals(request.getRequestURI());
  }

  /** Package-private so tests can drive it with controlled timestamps instead of sleeping. */
  boolean isAllowed(String key, Instant now) {
    Deque<Instant> attempts =
        attemptsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
    synchronized (attempts) {
      Instant cutoff = now.minus(window);
      while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
        attempts.pollFirst();
      }
      if (attempts.size() >= maxAttempts) {
        return false;
      }
      attempts.addLast(now);
      return true;
    }
  }

  private void writeTooManyRequests(HttpServletResponse response) throws IOException {
    ApiError body =
        new ApiError(
            Instant.now(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
            "Too many login attempts. Try again later.",
            LOGIN_PATH);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
    objectMapper.writeValue(response.getWriter(), body);
  }
}
