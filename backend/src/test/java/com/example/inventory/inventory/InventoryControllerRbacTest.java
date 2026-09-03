package com.example.inventory.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class InventoryControllerRbacTest extends AbstractIntegrationTest {

  private enum Endpoint {
    LIST,
    HISTORY,
    STOCK_IN
  }

  private MockHttpServletRequestBuilder requestFor(Endpoint endpoint, UUID productId) {
    return switch (endpoint) {
      case LIST -> get("/api/inventory");
      case HISTORY -> get("/api/inventory/history");
      case STOCK_IN ->
          post("/api/inventory/stock-in")
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  """
                                    {"productId":"%s","quantity":10,"reason":"Restock"}
                                    """
                      .formatted(productId));
    };
  }

  @ParameterizedTest
  @EnumSource(Endpoint.class)
  void employeeTokenIsRejectedFromEveryInventoryEndpoint(Endpoint endpoint) throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            requestFor(endpoint, product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @EnumSource(Endpoint.class)
  void noTokenIsUnauthorizedOnEveryInventoryEndpoint(Endpoint endpoint) throws Exception {
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc.perform(requestFor(endpoint, product.getId())).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @EnumSource(Endpoint.class)
  void adminTokenCanReachEveryInventoryEndpoint(Endpoint endpoint) throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            requestFor(endpoint, product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void stockInUpdatesProductQuantityAndWritesLedgerEntry() throws Exception {
    var admin = persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            requestFor(Endpoint.STOCK_IN, product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("STOCK_IN"))
        .andExpect(jsonPath("$.quantity").value(10))
        .andExpect(jsonPath("$.previousQuantity").value(10))
        .andExpect(jsonPath("$.newQuantity").value(20))
        .andExpect(jsonPath("$.reason").value("Restock"))
        .andExpect(jsonPath("$.performedByUserId").value(admin.getId().toString()));

    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(20);
  }

  @Test
  void stockInOnUnknownProductReturnsNotFound() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            requestFor(Endpoint.STOCK_IN, UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void stockInWithNonPositiveQuantityIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            post("/api/inventory/stock-in")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"productId":"%s","quantity":0}
                                        """
                        .formatted(product.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listIncludesInactiveProducts() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    persistProduct("Active Widget", "WIDGET-ACTIVE", 10, true);
    persistProduct("Inactive Widget", "WIDGET-INACTIVE", 10, false);

    mockMvc
        .perform(get("/api/inventory").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void historyCanBeFilteredByProduct() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var productA = persistProduct("Widget A", "WIDGET-A", 5, true);
    var productB = persistProduct("Widget B", "WIDGET-B", 5, true);

    mockMvc.perform(
        requestFor(Endpoint.STOCK_IN, productA.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken));
    mockMvc.perform(
        requestFor(Endpoint.STOCK_IN, productB.getId())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken));

    mockMvc
        .perform(
            get("/api/inventory/history?productId=" + productA.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].productId").value(productA.getId().toString()));

    mockMvc
        .perform(
            get("/api/inventory/history").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }
}
