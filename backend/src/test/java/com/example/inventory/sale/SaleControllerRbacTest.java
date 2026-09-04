package com.example.inventory.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.product.Product;
import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class SaleControllerRbacTest extends AbstractIntegrationTest {

  private static String saleBody(
      UUID clientTransactionId, UUID productId, int quantity, String sellingPrice) {
    return """
                {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":%d,"sellingPrice":%s}],
                 "paymentMethod":"CASH"}
                """
        .formatted(clientTransactionId, productId, quantity, sellingPrice);
  }

  private MockHttpServletRequestBuilder postSale(
      UUID clientTransactionId, UUID productId, int quantity) {
    return post("/api/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(saleBody(clientTransactionId, productId, quantity, "9.99"));
  }

  @Test
  void employeeCanCreateSale() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 2)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.totalAmount").value(19.98))
        .andExpect(jsonPath("$.items.length()").value(1));
  }

  @Test
  void cashSaleHasNoBankAccount() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 2)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.paymentMethod").value("CASH"))
        .andExpect(jsonPath("$.bankAccount").doesNotExist());
  }

  @Test
  void bankSaleWithoutAnAccountIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            post("/api/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":1,"sellingPrice":9.99}],
                     "paymentMethod":"BANK"}
                    """
                        .formatted(UUID.randomUUID(), product.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void bankSaleWithAnAccountSucceedsAndTheAccountIsVisibleToTheAdmin() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    String created =
        mockMvc
            .perform(
                post("/api/sales")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":1,"sellingPrice":9.99}],
                         "paymentMethod":"BANK","bankAccount":"CBE - 1000234567"}
                        """
                            .formatted(UUID.randomUUID(), product.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentMethod").value("BANK"))
            .andExpect(jsonPath("$.bankAccount").value("CBE - 1000234567"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String saleId = objectMapper.readTree(created).get("id").stringValue();

    mockMvc
        .perform(
            get("/api/sales/" + saleId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paymentMethod").value("BANK"))
        .andExpect(jsonPath("$.bankAccount").value("CBE - 1000234567"));
  }

  @Test
  void missingPaymentMethodIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            post("/api/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":1,"sellingPrice":9.99}]}
                    """
                        .formatted(UUID.randomUUID(), product.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminCannotCreateSale() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 2)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void noTokenCannotCreateSale() throws Exception {
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(postSale(UUID.randomUUID(), product.getId(), 2))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void noTokenCannotListOrGetSales() throws Exception {
    mockMvc.perform(get("/api/sales")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/sales/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
  }

  @Test
  void employeeSeesOnlyOwnSalesInList() throws Exception {
    var employeeA = persistUser("employee-a@example.com", Role.EMPLOYEE, true);
    persistUser("employee-b@example.com", Role.EMPLOYEE, true);
    String tokenA = loginAndGetToken("employee-a@example.com", RAW_PASSWORD);
    String tokenB = loginAndGetToken("employee-b@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc.perform(
        postSale(UUID.randomUUID(), product.getId(), 1)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA));
    mockMvc.perform(
        postSale(UUID.randomUUID(), product.getId(), 1)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB));

    mockMvc
        .perform(get("/api/sales").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].employeeId").value(employeeA.getId().toString()));
  }

  @Test
  void adminSeesAllSalesInList() throws Exception {
    persistUser("employee-a@example.com", Role.EMPLOYEE, true);
    persistUser("employee-b@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String tokenA = loginAndGetToken("employee-a@example.com", RAW_PASSWORD);
    String tokenB = loginAndGetToken("employee-b@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc.perform(
        postSale(UUID.randomUUID(), product.getId(), 1)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA));
    mockMvc.perform(
        postSale(UUID.randomUUID(), product.getId(), 1)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB));

    mockMvc
        .perform(get("/api/sales").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void employeeCannotGetAnotherEmployeesSaleById() throws Exception {
    persistUser("employee-a@example.com", Role.EMPLOYEE, true);
    persistUser("employee-b@example.com", Role.EMPLOYEE, true);
    String tokenA = loginAndGetToken("employee-a@example.com", RAW_PASSWORD);
    String tokenB = loginAndGetToken("employee-b@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    String created =
        mockMvc
            .perform(
                postSale(UUID.randomUUID(), product.getId(), 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String saleId = objectMapper.readTree(created).get("id").stringValue();

    mockMvc
        .perform(get("/api/sales/" + saleId).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminCanGetAnySale() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    String created =
        mockMvc
            .perform(
                postSale(UUID.randomUUID(), product.getId(), 1)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String saleId = objectMapper.readTree(created).get("id").stringValue();

    mockMvc
        .perform(
            get("/api/sales/" + saleId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  @Test
  void saleDeductsStockAndWritesStockOutTransaction() throws Exception {
    var employee = persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 3)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated());

    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(7);

    assertThat(inventoryTransactionRepository.count()).isEqualTo(1);
    var transaction = inventoryTransactionRepository.findAll().get(0);
    assertThat(transaction.getType().name()).isEqualTo("STOCK_OUT");
    assertThat(transaction.getPreviousQuantity()).isEqualTo(10);
    assertThat(transaction.getNewQuantity()).isEqualTo(7);
    assertThat(transaction.getReferenceType()).isEqualTo("SALE");
    assertThat(transaction.getPerformedBy().getId()).isEqualTo(employee.getId());
  }

  @Test
  void saleExceedingStockFailsAndRollsBackCompletely() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 5, true);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 10)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isConflict());

    var unchanged = productRepository.findById(product.getId()).orElseThrow();
    assertThat(unchanged.getStockQuantity()).isEqualTo(5);
    assertThat(saleRepository.count()).isZero();
    assertThat(inventoryTransactionRepository.count()).isZero();
  }

  @Test
  void concurrentSalesForLastUnitOnlyOneSucceeds() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    Product product = persistProduct("Widget", "WIDGET-1", 1, true);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch bothReady = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);

    Callable<Integer> saleAttempt =
        () -> {
          bothReady.countDown();
          go.await();
          return mockMvc
              .perform(
                  postSale(UUID.randomUUID(), product.getId(), 1)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
              .andReturn()
              .getResponse()
              .getStatus();
        };

    Future<Integer> resultA = executor.submit(saleAttempt);
    Future<Integer> resultB = executor.submit(saleAttempt);

    bothReady.await(5, TimeUnit.SECONDS);
    go.countDown();

    int statusA = resultA.get(15, TimeUnit.SECONDS);
    int statusB = resultB.get(15, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(List.of(statusA, statusB)).containsExactlyInAnyOrder(201, 409);

    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isZero();
    assertThat(saleRepository.count()).isEqualTo(1);
    assertThat(inventoryTransactionRepository.count()).isEqualTo(1);
  }

  @Test
  void duplicateClientTransactionIdIsIdempotent() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);
    UUID clientTransactionId = UUID.randomUUID();

    String first =
        mockMvc
            .perform(
                postSale(clientTransactionId, product.getId(), 2)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firstId = objectMapper.readTree(first).get("id").stringValue();

    String second =
        mockMvc
            .perform(
                postSale(clientTransactionId, product.getId(), 2)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String secondId = objectMapper.readTree(second).get("id").stringValue();

    assertThat(secondId).isEqualTo(firstId);
    assertThat(saleRepository.count()).isEqualTo(1);

    var product2 = productRepository.findById(product.getId()).orElseThrow();
    assertThat(product2.getStockQuantity()).isEqualTo(8);
  }

  @Test
  void sellingInactiveProductIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, false);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isConflict());
  }

  @Test
  void sellingUnknownProductReturnsNotFound() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), UUID.randomUUID(), 1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void nonPositiveQuantityIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            postSale(UUID.randomUUID(), product.getId(), 0)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void emptyItemsListIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(
            post("/api/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"clientTransactionId":"%s","items":[],"paymentMethod":"CASH"}
                                        """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void duplicateProductWithinOneSaleIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            post("/api/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"clientTransactionId":"%s","items":[
                                          {"productId":"%s","quantity":1,"sellingPrice":9.99},
                                          {"productId":"%s","quantity":1,"sellingPrice":9.99}
                                        ],"paymentMethod":"CASH"}
                                        """
                        .formatted(UUID.randomUUID(), product.getId(), product.getId())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void totalAmountIsComputedFromLineItemsServerSide() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var productA = persistProduct("Widget A", "WIDGET-A", 10, true);
    var productB = persistProduct("Widget B", "WIDGET-B", 10, true);

    mockMvc
        .perform(
            post("/api/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"clientTransactionId":"%s","items":[
                                          {"productId":"%s","quantity":2,"sellingPrice":5.00},
                                          {"productId":"%s","quantity":3,"sellingPrice":2.50}
                                        ],"paymentMethod":"CASH"}
                                        """
                        .formatted(UUID.randomUUID(), productA.getId(), productB.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.totalAmount").value(17.50))
        .andExpect(jsonPath("$.items.length()").value(2));
  }
}
