package com.example.inventory.user;

import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class UserControllerRbacTest extends AbstractIntegrationTest {

  private enum Endpoint {
    LIST,
    GET_ONE,
    CREATE,
    UPDATE,
    DEACTIVATE
  }

  private MockHttpServletRequestBuilder requestFor(Endpoint endpoint, UUID targetId) {
    return switch (endpoint) {
      case LIST -> get("/api/users");
      case GET_ONE -> get("/api/users/" + targetId);
      case CREATE ->
          post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  """
                                    {"name":"New Employee","email":"new-employee@example.com",
                                     "password":"a-strong-password","role":"EMPLOYEE"}
                                    """);
      case UPDATE ->
          put("/api/users/" + targetId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  """
                                    {"name":"Updated Name","email":"employee@example.com",
                                     "role":"EMPLOYEE","active":true}
                                    """);
      case DEACTIVATE -> delete("/api/users/" + targetId);
    };
  }

  @ParameterizedTest
  @EnumSource(Endpoint.class)
  void employeeTokenIsRejectedFromEveryUserEndpoint(Endpoint endpoint) throws Exception {
    var employee = persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            requestFor(endpoint, employee.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(Endpoint.class)
  void noTokenIsUnauthorizedOnEveryUserEndpoint(Endpoint endpoint) throws Exception {
    var target = persistUser("employee@example.com", Role.EMPLOYEE, true);

    mockMvc.perform(requestFor(endpoint, target.getId())).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @EnumSource(
      value = Endpoint.class,
      names = {"CREATE"},
      mode = EXCLUDE)
  void adminTokenCanReachEveryUserEndpoint(Endpoint endpoint) throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    var target = persistUser("employee@example.com", Role.EMPLOYEE, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            requestFor(endpoint, target.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void adminCanCreateAndDuplicateEmailIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    persistUser("new-employee@example.com", Role.EMPLOYEE, true);

    mockMvc
        .perform(
            requestFor(Endpoint.CREATE, null)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isConflict());
  }

  @Test
  void cannotDeactivateTheLastActiveAdmin() throws Exception {
    var admin = persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            delete("/api/users/" + admin.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isConflict());
  }

  @Test
  void deactivatingOneOfSeveralAdminsSucceeds() throws Exception {
    var admin = persistUser("admin@example.com", Role.ADMIN, true);
    persistUser("second-admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            delete("/api/users/" + admin.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void gettingUnknownUserReturnsNotFound() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            get("/api/users/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminCanListCreatedUsers() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }
}
