package com.example.inventory.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class DashboardControllerTest extends AbstractIntegrationTest {

  private static Stream<String> dashboardPaths() {
    return Stream.of(
        "/api/dashboard/summary",
        "/api/dashboard/sales",
        "/api/dashboard/low-stock",
        "/api/dashboard/top-products");
  }

  @ParameterizedTest
  @MethodSource("dashboardPaths")
  void employeeTokenIsRejectedFromEveryDashboardEndpoint(String path) throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @MethodSource("dashboardPaths")
  void noTokenIsUnauthorizedOnEveryDashboardEndpoint(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("dashboardPaths")
  void adminTokenCanReachEveryDashboardEndpoint(String path) throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc
        .perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
  }

  private String adminToken() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    return loginAndGetToken("admin@example.com", RAW_PASSWORD);
  }

  private String employeeToken(String email) throws Exception {
    persistUser(email, Role.EMPLOYEE, true);
    return loginAndGetToken(email, RAW_PASSWORD);
  }

  private void stockIn(String adminToken, UUID productId, int quantity) throws Exception {
    mockMvc.perform(
        post("/api/inventory/stock-in")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                                {"productId":"%s","quantity":%d}
                                """
                    .formatted(productId, quantity)));
  }

  private void createSale(String employeeToken, UUID productId, int quantity, String sellingPrice)
      throws Exception {
    mockMvc.perform(
        post("/api/sales")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                                {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":%d,"sellingPrice":%s}]}
                                """
                    .formatted(UUID.randomUUID(), productId, quantity, sellingPrice)));
  }

  private void syncSale(String employeeToken, UUID productId, int quantity, String sellingPrice)
      throws Exception {
    mockMvc.perform(
        post("/api/sync/sales")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                                {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":%d,"sellingPrice":%s}]}
                                """
                    .formatted(UUID.randomUUID(), productId, quantity, sellingPrice)));
  }

  @ParameterizedTest
  @MethodSource("dashboardPaths")
  void negativeStockFromAnOfflineSyncConflictDoesNotBreakAnyDashboardQuery(String path)
      throws Exception {
    String admin = adminToken();
    String employee = employeeToken("employee@example.com");
    var product = persistProduct("Oversold Widget", "WIDGET-OVERSOLD", 1, true);

    // Sync a sale for more than is in stock - this drives stock_quantity negative, which every
    // dashboard query (sums, low-stock comparisons, rankings) must tolerate without erroring.
    syncSale(employee, product.getId(), 5, "10.00");
    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(-4);

    mockMvc
        .perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk());
  }

  @Test
  void summaryCountsAllProductsAndStockRegardlessOfActiveStatus() throws Exception {
    String admin = adminToken();
    persistProduct("Active Widget", "WIDGET-ACTIVE", 10, true);
    persistProduct("Inactive Widget", "WIDGET-INACTIVE", 5, false);

    mockMvc
        .perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalProducts").value(2))
        .andExpect(jsonPath("$.totalStock").value(15));
  }

  @Test
  void summaryCountsOnlyActiveEmployees() throws Exception {
    String admin = adminToken();
    persistUser("active-employee@example.com", Role.EMPLOYEE, true);
    persistUser("inactive-employee@example.com", Role.EMPLOYEE, false);

    mockMvc
        .perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalEmployees").value(1));
  }

  @Test
  void lowStockCountAndListExcludeInactiveProducts() throws Exception {
    String admin = adminToken();
    // stockQuantity 2 <= lowStockThreshold 5 (set by persistProduct helper): qualifies.
    persistProduct("Low Stock Active", "WIDGET-LOW-ACTIVE", 2, true);
    persistProduct("Low Stock Inactive", "WIDGET-LOW-INACTIVE", 2, false);
    persistProduct("Well Stocked", "WIDGET-OK", 50, true);

    mockMvc
        .perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lowStockCount").value(1));

    mockMvc
        .perform(
            get("/api/dashboard/low-stock").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].sku").value("WIDGET-LOW-ACTIVE"));
  }

  @Test
  void periodRevenueAndSalesByEmployeeReflectCreatedSales() throws Exception {
    String admin = adminToken();
    String employee = employeeToken("employee@example.com");
    var product = persistProduct("Widget", "WIDGET-1", 100, true);
    stockIn(admin, product.getId(), 50);

    createSale(employee, product.getId(), 2, "10.00");
    createSale(employee, product.getId(), 3, "10.00");

    mockMvc
        .perform(get("/api/dashboard/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodSalesCount").value(2))
        .andExpect(jsonPath("$.periodRevenue").value(50.00))
        .andExpect(jsonPath("$.salesByEmployee.length()").value(1))
        .andExpect(jsonPath("$.salesByEmployee[0].salesCount").value(2))
        .andExpect(jsonPath("$.salesByEmployee[0].revenue").value(50.00))
        .andExpect(jsonPath("$.recentSales.length()").value(2))
        .andExpect(jsonPath("$.recentInventoryMovements.length()").value(3));
  }

  @Test
  void explicitHistoricalPeriodExcludesSalesMadeToday() throws Exception {
    String admin = adminToken();
    String employee = employeeToken("employee@example.com");
    var product = persistProduct("Widget", "WIDGET-1", 100, true);
    stockIn(admin, product.getId(), 50);
    createSale(employee, product.getId(), 1, "10.00");

    mockMvc
        .perform(
            get("/api/dashboard/summary?from=2020-01-01&to=2020-01-31")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.periodSalesCount").value(0))
        .andExpect(jsonPath("$.periodRevenue").value(0))
        .andExpect(jsonPath("$.salesByEmployee.length()").value(0));
  }

  @Test
  void dailySalesIncludesTodaysBucketByDefault() throws Exception {
    String admin = adminToken();
    String employee = employeeToken("employee@example.com");
    var product = persistProduct("Widget", "WIDGET-1", 100, true);
    stockIn(admin, product.getId(), 50);
    createSale(employee, product.getId(), 2, "10.00");

    String response =
        mockMvc
            .perform(
                get("/api/dashboard/sales").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    var today = LocalDate.now(ZoneOffset.UTC).toString();
    assertThat(response).contains(today);
    assertThat(response).contains("\"salesCount\":1");
  }

  @Test
  void topProductsRanksByUnitsSoldWithinPeriod() throws Exception {
    String admin = adminToken();
    String employee = employeeToken("employee@example.com");
    var popular = persistProduct("Popular Widget", "WIDGET-POPULAR", 100, true);
    var unpopular = persistProduct("Unpopular Widget", "WIDGET-UNPOPULAR", 100, true);
    stockIn(admin, popular.getId(), 50);
    stockIn(admin, unpopular.getId(), 50);

    createSale(employee, popular.getId(), 10, "5.00");
    createSale(employee, unpopular.getId(), 1, "5.00");

    mockMvc
        .perform(
            get("/api/dashboard/top-products").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].productId").value(popular.getId().toString()))
        .andExpect(jsonPath("$[0].unitsSold").value(10))
        .andExpect(jsonPath("$[1].productId").value(unpopular.getId().toString()))
        .andExpect(jsonPath("$[1].unitsSold").value(1));
  }
}
