package com.example.inventory.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthControllerTest extends AbstractIntegrationTest {

  @Test
  void loginWithValidCredentialsReturnsToken() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"admin@example.com","password":"%s"}
                                        """
                        .formatted(RAW_PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.email").value("admin@example.com"))
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
  }

  @Test
  void loginWithWrongPasswordIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"admin@example.com","password":"wrong-password"}
                                        """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithUnknownEmailIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"nobody@example.com","password":"whatever1"}
                                        """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithInactiveUserIsRejected() throws Exception {
    persistUser("inactive@example.com", Role.EMPLOYEE, false);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"email":"inactive@example.com","password":"%s"}
                                        """
                        .formatted(RAW_PASSWORD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void meWithoutTokenIsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void meWithValidTokenReturnsCurrentUser() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("employee@example.com"));
  }

  @Test
  void meWithTamperedTokenIsUnauthorized() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    mockMvc
        .perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutWithValidTokenSucceeds() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }
}
