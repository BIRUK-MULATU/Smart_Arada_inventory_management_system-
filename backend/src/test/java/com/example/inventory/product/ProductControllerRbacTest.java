package com.example.inventory.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ProductControllerRbacTest extends AbstractIntegrationTest {

  private enum WriteEndpoint {
    CREATE,
    UPDATE,
    DEACTIVATE
  }

  private UUID categoryId;

  @BeforeEach
  void setUpCategory() {
    categoryId = persistCategory("Widgets").getId();
  }

  private MockHttpServletRequestBuilder requestFor(WriteEndpoint endpoint, UUID targetId) {
    return switch (endpoint) {
      case CREATE ->
          post("/api/products")
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  """
                                    {"name":"New Widget","sku":"WIDGET-NEW","categoryId":"%s","basePrice":12.50,"lowStockThreshold":3}
                                    """
                      .formatted(categoryId));
      case UPDATE ->
          put("/api/products/" + targetId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  """
                                    {"name":"Updated Widget","sku":"WIDGET-1","categoryId":"%s","basePrice":15.00,
                                     "lowStockThreshold":3,"active":true}
                                    """
                      .formatted(categoryId));
      case DEACTIVATE -> delete("/api/products/" + targetId);
    };
  }

  @ParameterizedTest
  @EnumSource(WriteEndpoint.class)
  void employeeTokenIsRejectedFromWriteEndpoints(WriteEndpoint endpoint) throws Exception {
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
  @EnumSource(WriteEndpoint.class)
  void noTokenIsUnauthorizedOnWriteEndpoints(WriteEndpoint endpoint) throws Exception {
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc.perform(requestFor(endpoint, product.getId())).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @EnumSource(WriteEndpoint.class)
  void adminTokenCanReachWriteEndpoints(WriteEndpoint endpoint) throws Exception {
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
  void employeeCanListAndGetProducts() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    mockMvc
        .perform(
            get("/api/products/" + product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk());
  }

  @Test
  void noTokenIsUnauthorizedOnReadEndpoints() throws Exception {
    mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
  }

  @Test
  void newProductStartsWithZeroStock() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            requestFor(WriteEndpoint.CREATE, null)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stockQuantity").value(0));
  }

  @Test
  void creatingWithDuplicateSkuIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    persistProduct("Existing Widget", "WIDGET-NEW", 5, true);

    mockMvc
        .perform(
            requestFor(WriteEndpoint.CREATE, null)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isConflict());
  }

  @Test
  void creatingWithNegativePriceIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"name":"Bad Widget","basePrice":-5.00,"lowStockThreshold":3}
                                        """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void employeeDefaultListExcludesInactiveProducts() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    persistProduct("Active Widget", "WIDGET-ACTIVE", 10, true);
    persistProduct("Inactive Widget", "WIDGET-INACTIVE", 10, false);

    mockMvc
        .perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void employeeIncludeInactiveFlagIsIgnored() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    persistProduct("Active Widget", "WIDGET-ACTIVE", 10, true);
    persistProduct("Inactive Widget", "WIDGET-INACTIVE", 10, false);

    mockMvc
        .perform(
            get("/api/products?includeInactive=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void adminIncludeInactiveFlagReturnsDeactivatedProducts() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    persistProduct("Active Widget", "WIDGET-ACTIVE", 10, true);
    persistProduct("Inactive Widget", "WIDGET-INACTIVE", 10, false);

    mockMvc
        .perform(
            get("/api/products?includeInactive=true")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void deactivatedProductIsStillFetchableById() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Inactive Widget", "WIDGET-INACTIVE", 10, false);

    mockMvc
        .perform(
            get("/api/products/" + product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void gettingUnknownProductReturnsNotFound() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            get("/api/products/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void filteringByCategoryIdReturnsOnlyThatCategorysProducts() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var electronics = persistCategory("Electronics");
    var glassware = persistCategory("Glassware");

    Product tv = new Product();
    tv.setName("TV");
    tv.setCategory(electronics);
    tv.setBasePrice(new java.math.BigDecimal("300.00"));
    tv.setLowStockThreshold(1);
    productRepository.save(tv);

    Product glass = new Product();
    glass.setName("Drinking Glass");
    glass.setCategory(glassware);
    glass.setBasePrice(new java.math.BigDecimal("2.00"));
    glass.setLowStockThreshold(5);
    productRepository.save(glass);

    mockMvc
        .perform(
            get("/api/products?categoryId=" + electronics.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("TV"));
  }
}
