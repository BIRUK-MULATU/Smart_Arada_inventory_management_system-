package com.example.inventory.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class GlobalExceptionHandlerTest extends AbstractIntegrationTest {

  @Test
  void malformedJsonBodyReturnsBadRequestNotAServerError() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("The request body or parameters could not be read"));
  }

  @Test
  void unknownAuthenticatedPathReturnsNotFoundNotAServerError() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            get("/api/totally-unknown-path").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void errorResponsesNeverIncludeAStackTraceOrExceptionClassName() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    String body =
        mockMvc
            .perform(
                post("/api/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not json"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body).doesNotContain("com.example.inventory");
    org.assertj.core.api.Assertions.assertThat(body).doesNotContainIgnoringCase("exception");
    org.assertj.core.api.Assertions.assertThat(body).doesNotContain("\tat ");
  }
}
