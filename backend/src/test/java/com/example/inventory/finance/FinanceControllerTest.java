package com.example.inventory.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.product.Product;
import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class FinanceControllerTest extends AbstractIntegrationTest {

  private Product persistProductWithCost(
      String name, int stock, String basePrice, String costPrice) {
    Product product = new Product();
    product.setName(name);
    product.setCategory(defaultCategory());
    product.setBasePrice(new BigDecimal(basePrice));
    product.setCostPrice(new BigDecimal(costPrice));
    product.setStockQuantity(stock);
    product.setLowStockThreshold(1);
    product.setActive(true);
    return productRepository.save(product);
  }

  private static final String[] FINANCE_GET_PATHS = {
    "/api/finance/summary", "/api/finance/budgets"
  };

  @Test
  void employeeCannotReachAnyFinanceEndpoint() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    for (String path : FINANCE_GET_PATHS) {
      mockMvc
          .perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
          .andExpect(status().isForbidden());
    }

    mockMvc
        .perform(
            post("/api/finance/expenses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"description":"Nope","amount":10.00,"incurredOn":"2026-01-01"}
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void noTokenIsUnauthorizedOnFinanceEndpoints() throws Exception {
    for (String path : FINANCE_GET_PATHS) {
      mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
  }

  @Test
  void costPriceIsRedactedForEmployeeButVisibleForAdmin() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProductWithCost("Widget", 10, "10.00", "4.00");

    mockMvc
        .perform(
            get("/api/products/" + product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.costPrice").doesNotExist());

    mockMvc
        .perform(
            get("/api/products/" + product.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.costPrice").value(4.00));

    mockMvc
        .perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].costPrice").doesNotExist());
  }

  @Test
  void summaryComputesRevenueCogsAndProfitFromSalesAndExpenses() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProductWithCost("Widget", 10, "10.00", "3.00");

    // Sell 2 units at 10.00 each: revenue 20.00, COGS 2*3.00 = 6.00, gross profit 14.00.
    mockMvc.perform(
        post("/api/sales")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":2,"sellingPrice":10.00}]}
                """
                    .formatted(UUID.randomUUID(), product.getId())));

    // Record a 5.00 expense today: net profit 14.00 - 5.00 = 9.00.
    mockMvc
        .perform(
            post("/api/finance/expenses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"category":"Utilities","description":"Electricity","amount":5.00,"incurredOn":"%s"}
                    """
                        .formatted(LocalDate.now())))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/finance/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revenue").value(20.00))
        .andExpect(jsonPath("$.costOfGoodsSold").value(6.00))
        .andExpect(jsonPath("$.grossProfit").value(14.00))
        .andExpect(jsonPath("$.totalExpenses").value(5.00))
        .andExpect(jsonPath("$.netProfit").value(9.00));
  }

  @Test
  void expenseCategoryDefaultsToOverallWhenBlank() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    String response =
        mockMvc
            .perform(
                post("/api/finance/expenses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"description":"Misc","amount":1.00,"incurredOn":"2026-01-01"}
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(objectMapper.readTree(response).get("category").stringValue()).isEqualTo("Overall");
  }

  @Test
  void adminCanDeleteAnExpense() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    String created =
        mockMvc
            .perform(
                post("/api/finance/expenses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"description":"Misc","amount":1.00,"incurredOn":"2026-01-01"}
                        """))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String expenseId = objectMapper.readTree(created).get("id").stringValue();

    mockMvc
        .perform(
            delete("/api/finance/expenses/" + expenseId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/finance/expenses?from=2026-01-01&to=2026-01-01")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void creatingADuplicateBudgetScopeIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    String body =
        """
        {"category":"Rent","periodType":"MONTHLY","periodStart":"2026-03-15","amount":500.00}
        """;

    mockMvc
        .perform(
            post("/api/finance/budgets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.periodStart").value("2026-03-01"));

    // Same category/period-type/month (even a different day within it, since MONTHLY periods are
    // normalized to the 1st) must be rejected as a duplicate scope.
    mockMvc
        .perform(
            post("/api/finance/budgets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"category":"Rent","periodType":"MONTHLY","periodStart":"2026-03-01","amount":600.00}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void budgetActualsReflectExpensesWithinThePeriod() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc.perform(
        post("/api/finance/budgets")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"category":"Rent","periodType":"MONTHLY","periodStart":"2026-03-01","amount":500.00}
                """));

    // Inside March: counted.
    mockMvc.perform(
        post("/api/finance/expenses")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"category":"Rent","description":"March rent","amount":450.00,"incurredOn":"2026-03-15"}
                """));
    // Outside March (April): not counted.
    mockMvc.perform(
        post("/api/finance/expenses")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"category":"Rent","description":"April rent","amount":450.00,"incurredOn":"2026-04-01"}
                """));

    mockMvc
        .perform(get("/api/finance/budgets").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].budgetAmount").value(500.00))
        .andExpect(jsonPath("$[0].actualAmount").value(450.00));
  }

  @Test
  void adminCanUpdateAndDeleteABudget() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    String created =
        mockMvc
            .perform(
                post("/api/finance/budgets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"category":"Rent","periodType":"MONTHLY","periodStart":"2026-03-01","amount":500.00}
                        """))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String budgetId = objectMapper.readTree(created).get("id").stringValue();

    mockMvc
        .perform(
            put("/api/finance/budgets/" + budgetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"amount":750.00}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(750.00));

    mockMvc
        .perform(
            delete("/api/finance/budgets/" + budgetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/api/finance/budgets").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
