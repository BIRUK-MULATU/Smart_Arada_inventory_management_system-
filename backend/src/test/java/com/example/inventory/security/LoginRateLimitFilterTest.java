package com.example.inventory.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class LoginRateLimitFilterTest {

  @Test
  void isAllowedPermitsUpToTheConfiguredLimitWithinTheWindow() {
    var filter = new LoginRateLimitFilter(3, 60, new ObjectMapper());
    Instant now = Instant.parse("2024-01-01T00:00:00Z");

    assertThat(filter.isAllowed("1.2.3.4", now)).isTrue();
    assertThat(filter.isAllowed("1.2.3.4", now)).isTrue();
    assertThat(filter.isAllowed("1.2.3.4", now)).isTrue();
    assertThat(filter.isAllowed("1.2.3.4", now)).isFalse();
  }

  @Test
  void isAllowedTracksEachKeyIndependently() {
    var filter = new LoginRateLimitFilter(1, 60, new ObjectMapper());
    Instant now = Instant.now();

    assertThat(filter.isAllowed("1.2.3.4", now)).isTrue();
    assertThat(filter.isAllowed("5.6.7.8", now)).isTrue();
    assertThat(filter.isAllowed("1.2.3.4", now)).isFalse();
  }

  @Test
  void isAllowedResetsOnceTheWindowElapses() {
    var filter = new LoginRateLimitFilter(1, 60, new ObjectMapper());
    Instant now = Instant.parse("2024-01-01T00:00:00Z");

    assertThat(filter.isAllowed("1.2.3.4", now)).isTrue();
    assertThat(filter.isAllowed("1.2.3.4", now.plusSeconds(30))).isFalse();
    assertThat(filter.isAllowed("1.2.3.4", now.plusSeconds(61))).isTrue();
  }

  @Test
  void requestsToOtherPathsBypassTheLimitEntirely() throws Exception {
    var filter = new LoginRateLimitFilter(0, 60, new ObjectMapper());
    AtomicBoolean chainCalled = new AtomicBoolean(false);
    FilterChain chain = (req, res) -> chainCalled.set(true);

    filter.doFilterInternal(
        new MockHttpServletRequest("GET", "/api/products"), new MockHttpServletResponse(), chain);

    assertThat(chainCalled).isTrue();
  }

  @Test
  void loginRequestsBeyondTheLimitReturn429WithTheStandardErrorShape() throws Exception {
    var filter = new LoginRateLimitFilter(1, 60, new ObjectMapper());
    FilterChain chain = (req, res) -> {};

    filter.doFilterInternal(
        new MockHttpServletRequest("POST", "/api/auth/login"),
        new MockHttpServletResponse(),
        chain);

    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilterInternal(new MockHttpServletRequest("POST", "/api/auth/login"), response, chain);

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    assertThat(response.getContentAsString()).contains("\"status\":429", "/api/auth/login");
  }
}
