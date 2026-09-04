package com.example.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Drives one continuous flow across every major capability of the system through its real HTTP API,
 * the same way a full session in the app would: an admin logs in and manages the catalog, an
 * employee logs in and records a sale online, the admin corrects stock manually, and the employee
 * syncs a sale that happened while they were offline. No step is mocked - every request goes
 * through JWT auth, RBAC, the real transactional service layer, and the real Postgres schema.
 */
class EndToEndFlowTest extends AbstractIntegrationTest {

  @Test
  void loginProductManagementSaleInventoryAndSync() throws Exception {
    // 1. Login: admin account exists and can authenticate.
    persistUser("admin@example.com", Role.ADMIN, true);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    // 2. Product management: admin creates a category, then creates and updates a product in it.
    String categoryResponse =
        mockMvc
            .perform(
                post("/api/categories")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"E2E Category"}
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String categoryId = objectMapper.readTree(categoryResponse).get("id").stringValue();

    String createResponse =
        mockMvc
            .perform(
                post("/api/products")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"E2E Widget","sku":"E2E-1","categoryId":"%s","basePrice":12.50,"lowStockThreshold":3}
                        """
                            .formatted(categoryId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String productId = objectMapper.readTree(createResponse).get("id").stringValue();

    mockMvc
        .perform(
            put("/api/products/" + productId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"E2E Widget","sku":"E2E-1","categoryId":"%s","basePrice":15.00,"lowStockThreshold":3,"active":true}
                    """
                        .formatted(categoryId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.basePrice").value(15.00))
        .andExpect(jsonPath("$.categoryName").value("E2E Category"));

    // Stock starts at zero - admin stocks it up before anyone can sell it.
    mockMvc
        .perform(
            post("/api/inventory/stock-in")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productId":"%s","quantity":10,"reason":"initial stock"}
                    """
                        .formatted(productId)))
        .andExpect(status().isCreated());

    // 3. An employee logs in.
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    // Employee can browse the catalog but not manage it.
    mockMvc
        .perform(get("/api/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].stockQuantity").value(10));
    mockMvc
        .perform(
            post("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Nope","categoryId":"%s","basePrice":1.00,"lowStockThreshold":1}
                    """
                        .formatted(categoryId)))
        .andExpect(status().isForbidden());

    // 4. Sale creation: an online sale deducts stock transactionally.
    mockMvc
        .perform(
            post("/api/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":4,"sellingPrice":15.00}]}
                    """
                        .formatted(UUID.randomUUID(), productId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    var afterOnlineSale = productRepository.findById(UUID.fromString(productId)).orElseThrow();
    assertThat(afterOnlineSale.getStockQuantity()).isEqualTo(6);

    // 5. Inventory update: admin reviews the movement history and tops up stock again.
    mockMvc
        .perform(
            get("/api/inventory/history").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));

    mockMvc
        .perform(
            post("/api/inventory/stock-in")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productId":"%s","quantity":5,"reason":"top up"}
                    """
                        .formatted(productId)))
        .andExpect(status().isCreated());

    var afterTopUp = productRepository.findById(UUID.fromString(productId)).orElseThrow();
    assertThat(afterTopUp.getStockQuantity()).isEqualTo(11);

    // 6. Sync: a sale the employee recorded offline (e.g. on the drive home) syncs once back
    // online, going through the same idempotent, accept-and-flag sync path as any other device.
    UUID offlineClientTransactionId = UUID.randomUUID();
    String syncResponse =
        mockMvc
            .perform(
                post("/api/sync/sales")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":2,"sellingPrice":15.00}]}
                        """
                            .formatted(offlineClientTransactionId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String syncedSaleId = objectMapper.readTree(syncResponse).get("id").stringValue();

    var afterSync = productRepository.findById(UUID.fromString(productId)).orElseThrow();
    assertThat(afterSync.getStockQuantity()).isEqualTo(9);

    // Retrying the same sync (e.g. the device retries because it never saw the response) must not
    // create a second sale.
    mockMvc
        .perform(
            post("/api/sync/sales")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientTransactionId":"%s","items":[{"productId":"%s","quantity":2,"sellingPrice":15.00}]}
                    """
                        .formatted(offlineClientTransactionId, productId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(syncedSaleId));

    assertThat(saleRepository.count()).isEqualTo(2);
    var finalStock = productRepository.findById(UUID.fromString(productId)).orElseThrow();
    assertThat(finalStock.getStockQuantity()).isEqualTo(9);

    // Both the online sale and the synced sale are visible to the admin.
    mockMvc
        .perform(get("/api/sales").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }
}
