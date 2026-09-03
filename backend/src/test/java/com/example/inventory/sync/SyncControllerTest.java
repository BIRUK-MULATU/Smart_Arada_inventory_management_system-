package com.example.inventory.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.product.Product;
import com.example.inventory.sale.SaleStatus;
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

class SyncControllerTest extends AbstractIntegrationTest {

  private static String saleBody(
      UUID clientTransactionId, UUID productId, int quantity, String sellingPrice) {
    return """
                {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":%d,"sellingPrice":%s}]}
                """
        .formatted(clientTransactionId, productId, quantity, sellingPrice);
  }

  private MockHttpServletRequestBuilder postSync(
      UUID clientTransactionId, UUID productId, int quantity) {
    return post("/api/sync/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(saleBody(clientTransactionId, productId, quantity, "9.99"));
  }

  @Test
  void syncingTheSameSaleTenTimesProducesExactlyOneRow() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);
    UUID clientTransactionId = UUID.randomUUID();

    String firstId = null;
    for (int i = 0; i < 10; i++) {
      String response =
          mockMvc
              .perform(
                  postSync(clientTransactionId, product.getId(), 2)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
              .andReturn()
              .getResponse()
              .getContentAsString();
      String id = objectMapper.readTree(response).get("id").stringValue();
      if (firstId == null) {
        firstId = id;
      } else {
        assertThat(id).isEqualTo(firstId);
      }
    }

    assertThat(saleRepository.count()).isEqualTo(1);
    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(8);
    assertThat(inventoryTransactionRepository.count()).isEqualTo(1);
  }

  @Test
  void twoDevicesSellingTheLastUnitBothSucceedStockGoesNegativeBothFlagged() throws Exception {
    // Both devices' offline caches showed the last unit as available; by the time both sync,
    // the unit is already gone (stock is 0) - representing the real-world case where two
    // employees, unaware of each other, each believed they were selling the one remaining item.
    // Neither sync can be "the lucky one that gets it cleanly" - both are short, so both must be
    // flagged for admin review.
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    Product product = persistProduct("Widget", "WIDGET-1", 0, true);

    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch bothReady = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);

    Callable<Integer> syncAttempt =
        () -> {
          bothReady.countDown();
          go.await();
          return mockMvc
              .perform(
                  postSync(UUID.randomUUID(), product.getId(), 1)
                      .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
              .andReturn()
              .getResponse()
              .getStatus();
        };

    Future<Integer> resultA = executor.submit(syncAttempt);
    Future<Integer> resultB = executor.submit(syncAttempt);

    bothReady.await(5, TimeUnit.SECONDS);
    go.countDown();

    int statusA = resultA.get(15, TimeUnit.SECONDS);
    int statusB = resultB.get(15, TimeUnit.SECONDS);
    executor.shutdown();

    // Unlike the online path, both offline syncs are accepted - each represents a sale that
    // already happened on a device - so both requests succeed with 201, never 409.
    assertThat(List.of(statusA, statusB)).containsExactly(201, 201);

    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(-2);
    assertThat(saleRepository.count()).isEqualTo(2);
    assertThat(
            saleRepository.findAll().stream()
                .filter(s -> s.getStatus() == SaleStatus.CONFLICT)
                .count())
        .isEqualTo(2);
    assertThat(inventoryTransactionRepository.count()).isEqualTo(2);
  }

  @Test
  void syncInterruptedMidRequestAndRetriedProducesOneSale() throws Exception {
    // Simulates a client that sent a sync request, lost the response (e.g. the connection dropped
    // mid-flight), and retried with the same client_transaction_id not knowing whether the first
    // attempt landed. Both must be safe to send; only one sale must ever exist.
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);
    UUID clientTransactionId = UUID.randomUUID();

    String first =
        mockMvc
            .perform(
                postSync(clientTransactionId, product.getId(), 3)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String firstId = objectMapper.readTree(first).get("id").stringValue();

    String retried =
        mockMvc
            .perform(
                postSync(clientTransactionId, product.getId(), 3)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String retriedId = objectMapper.readTree(retried).get("id").stringValue();

    assertThat(retriedId).isEqualTo(firstId);
    assertThat(saleRepository.count()).isEqualTo(1);
    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(7);
  }

  @Test
  void insufficientStockStillSyncsAndFlagsConflictWithShortfall() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 2, true);

    mockMvc
        .perform(
            postSync(UUID.randomUUID(), product.getId(), 5)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFLICT"));

    var updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getStockQuantity()).isEqualTo(-3);

    var transaction = inventoryTransactionRepository.findAll().get(0);
    assertThat(transaction.getPreviousQuantity()).isEqualTo(2);
    assertThat(transaction.getNewQuantity()).isEqualTo(-3);
    assertThat(transaction.getReason()).contains("short by 3");
  }

  @Test
  void employeeCannotListOrResolveConflicts() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(get("/api/sync/conflicts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            post("/api/sync/conflicts/" + UUID.randomUUID() + "/resolve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adjustments":[{"productId":"%s","restockQuantity":1}],"note":"test"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCannotSyncSales() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(
            postSync(UUID.randomUUID(), product.getId(), 1)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminResolutionClearsConflictAndWritesInventoryTransaction() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 2, true);

    String synced =
        mockMvc
            .perform(
                postSync(UUID.randomUUID(), product.getId(), 5)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
            .andExpect(jsonPath("$.status").value("CONFLICT"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String saleId = objectMapper.readTree(synced).get("id").stringValue();

    assertThat(inventoryTransactionRepository.count()).isEqualTo(1);

    mockMvc
        .perform(
            post("/api/sync/conflicts/" + saleId + "/resolve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adjustments":[{"productId":"%s","restockQuantity":3}],"note":"Restocked from back room"}
                    """
                        .formatted(product.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESOLVED"))
        .andExpect(jsonPath("$.resolutionNote").value("Restocked from back room"));

    var resolved = saleRepository.findById(UUID.fromString(saleId)).orElseThrow();
    assertThat(resolved.getStatus()).isEqualTo(SaleStatus.RESOLVED);
    assertThat(resolved.getResolvedBy()).isNotNull();
    assertThat(resolved.getResolvedAt()).isNotNull();

    var updatedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(0);

    assertThat(inventoryTransactionRepository.count()).isEqualTo(2);
    var resolutionTransaction =
        inventoryTransactionRepository.findAll().stream()
            .filter(t -> "CONFLICT_RESOLUTION".equals(t.getReferenceType()))
            .findFirst()
            .orElseThrow();
    assertThat(resolutionTransaction.getType().name()).isEqualTo("STOCK_IN");
    assertThat(resolutionTransaction.getPreviousQuantity()).isEqualTo(-3);
    assertThat(resolutionTransaction.getNewQuantity()).isEqualTo(0);

    // Once resolved, the conflict no longer shows up in the open-conflicts list.
    mockMvc
        .perform(
            get("/api/sync/conflicts").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void openConflictsListShowsShortfallDetails() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 2, true);

    mockMvc.perform(
        postSync(UUID.randomUUID(), product.getId(), 5)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken));

    mockMvc
        .perform(
            get("/api/sync/conflicts").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].items[0].shortfall").value(3))
        .andExpect(jsonPath("$.content[0].items[0].productId").value(product.getId().toString()))
        .andExpect(jsonPath("$.content[0].employeeName").exists());
  }

  @Test
  void cannotResolveAnAlreadyResolvedOrNonConflictSale() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    String synced =
        mockMvc
            .perform(
                postSync(UUID.randomUUID(), product.getId(), 2)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String saleId = objectMapper.readTree(synced).get("id").stringValue();

    mockMvc
        .perform(
            post("/api/sync/conflicts/" + saleId + "/resolve")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"adjustments":[{"productId":"%s","restockQuantity":0}],"note":"not actually a conflict"}
                    """
                        .formatted(product.getId())))
        .andExpect(status().isConflict());
  }
}
