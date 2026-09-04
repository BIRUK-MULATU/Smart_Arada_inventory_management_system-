package com.example.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.inventory.support.SharedPostgresContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class SchemaMigrationTest {

  private static final PostgreSQLContainer<?> POSTGRES = SharedPostgresContainer.instance();

  private Connection conn;

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .load()
        .migrate();
  }

  @BeforeEach
  void openConnection() throws SQLException {
    conn =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    conn.setAutoCommit(false);
  }

  @AfterEach
  void rollbackAndClose() throws SQLException {
    conn.rollback();
    conn.close();
  }

  private UUID insertUser(String email) throws SQLException {
    UUID id = UUID.randomUUID();
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO users (id, name, email, password_hash, role) "
                + "VALUES (?, ?, ?, ?, 'EMPLOYEE')")) {
      ps.setObject(1, id);
      ps.setString(2, "Test User");
      ps.setString(3, email);
      ps.setString(4, "hash");
      ps.executeUpdate();
    }
    return id;
  }

  private UUID insertCategory() throws SQLException {
    // Deliberately inserts its own category rather than relying on the "Uncategorized" row seeded
    // by the V4 migration - other test classes sharing this same Testcontainers instance clean out
    // the categories table between tests, so that seed row isn't guaranteed to still exist here.
    UUID id = UUID.randomUUID();
    try (PreparedStatement ps =
        conn.prepareStatement("INSERT INTO categories (id, name) VALUES (?, ?)")) {
      ps.setObject(1, id);
      ps.setString(2, "Test Category " + id);
      ps.executeUpdate();
    }
    return id;
  }

  private UUID insertProduct(int stockQuantity) throws SQLException {
    UUID id = UUID.randomUUID();
    UUID categoryId = insertCategory();
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO products (id, name, category_id, base_price, stock_quantity,"
                + " low_stock_threshold) "
                + "VALUES (?, ?, ?, 10.00, ?, 5)")) {
      ps.setObject(1, id);
      ps.setString(2, "Test Product");
      ps.setObject(3, categoryId);
      ps.setInt(4, stockQuantity);
      ps.executeUpdate();
    }
    return id;
  }

  private void insertSale(UUID employeeId, UUID clientTxnId) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO sales (id, employee_id, client_transaction_id, total_amount) "
                + "VALUES (?, ?, ?, 10.00)")) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, employeeId);
      ps.setObject(3, clientTxnId);
      ps.executeUpdate();
    }
  }

  @Test
  void rejectsDuplicateClientTransactionId() throws SQLException {
    UUID employeeId = insertUser("dup-test@example.com");
    UUID clientTxnId = UUID.randomUUID();

    insertSale(employeeId, clientTxnId);
    var savepoint = conn.setSavepoint();

    assertThatThrownBy(() -> insertSale(employeeId, clientTxnId))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ux_sales_client_transaction_id");
    conn.rollback(savepoint);
  }

  @Test
  void allowsNegativeStockQuantityForOfflineSyncConflicts() throws SQLException {
    // The offline sync path (Phase 10) must be able to record a sale that already happened even
    // when stock is short, so the database no longer enforces stock_quantity >= 0 - only the
    // online sale path enforces that in application code.
    UUID productId = insertProduct(-1);
    assertThat(productId).isNotNull();
  }

  @Test
  void rejectsInvalidSaleStatus() throws SQLException {
    UUID employeeId = insertUser("bad-status@example.com");
    var savepoint = conn.setSavepoint();

    assertThatThrownBy(
            () -> {
              try (PreparedStatement ps =
                  conn.prepareStatement(
                      "INSERT INTO sales (id, employee_id, client_transaction_id, total_amount,"
                          + " status) VALUES (?, ?, ?, 10.00, 'BOGUS')")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, employeeId);
                ps.setObject(3, UUID.randomUUID());
                ps.executeUpdate();
              }
            })
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("status");
    conn.rollback(savepoint);
  }

  @Test
  void rejectsResolvedSaleWithoutResolutionDetails() throws SQLException {
    UUID employeeId = insertUser("bad-resolution@example.com");
    var savepoint = conn.setSavepoint();

    assertThatThrownBy(
            () -> {
              try (PreparedStatement ps =
                  conn.prepareStatement(
                      "INSERT INTO sales (id, employee_id, client_transaction_id, total_amount,"
                          + " status) VALUES (?, ?, ?, 10.00, 'RESOLVED')")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, employeeId);
                ps.setObject(3, UUID.randomUUID());
                ps.executeUpdate();
              }
            })
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_sales_resolution_consistent");
    conn.rollback(savepoint);
  }

  @Test
  void rejectsInconsistentInventoryLedger() throws SQLException {
    UUID productId = insertProduct(10);
    UUID userId = insertUser("ledger-test@example.com");
    var savepoint = conn.setSavepoint();

    assertThatThrownBy(
            () -> {
              try (PreparedStatement ps =
                  conn.prepareStatement(
                      "INSERT INTO inventory_transactions "
                          + "(id, product_id, type, quantity, previous_quantity,"
                          + " new_quantity, performed_by) "
                          + "VALUES (?, ?, 'STOCK_OUT', 5, 10, 4, ?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, productId);
                ps.setObject(3, userId);
                ps.executeUpdate();
              }
            })
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("ck_inventory_transactions_ledger_consistent");
    conn.rollback(savepoint);
  }

  @Test
  void acceptsConsistentInventoryLedgerEntry() throws SQLException {
    UUID productId = insertProduct(10);
    UUID userId = insertUser("valid-ledger@example.com");

    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO inventory_transactions "
                + "(id, product_id, type, quantity, previous_quantity, new_quantity,"
                + " performed_by) "
                + "VALUES (?, ?, 'STOCK_OUT', 5, 10, 5, ?)")) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, productId);
      ps.setObject(3, userId);
      int rows = ps.executeUpdate();
      assertThat(rows).isEqualTo(1);
    }
  }

  @Test
  void allowsNegativeNewQuantityForOfflineSyncConflicts() throws SQLException {
    // A STOCK_OUT driven by an offline sync conflict can legitimately drive new_quantity negative
    // (selling more than was on hand when the device recorded the sale) - the ledger consistency
    // check (new_quantity = previous_quantity - quantity) still applies, but the >= 0 floor does
    // not.
    UUID productId = insertProduct(1);
    UUID userId = insertUser("negative-ledger@example.com");

    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO inventory_transactions "
                + "(id, product_id, type, quantity, previous_quantity, new_quantity,"
                + " performed_by) "
                + "VALUES (?, ?, 'STOCK_OUT', 3, 1, -2, ?)")) {
      ps.setObject(1, UUID.randomUUID());
      ps.setObject(2, productId);
      ps.setObject(3, userId);
      int rows = ps.executeUpdate();
      assertThat(rows).isEqualTo(1);
    }
  }
}
