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

  private UUID insertProduct(int stockQuantity) throws SQLException {
    UUID id = UUID.randomUUID();
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO products (id, name, base_price, stock_quantity, low_stock_threshold) "
                + "VALUES (?, ?, 10.00, ?, 5)")) {
      ps.setObject(1, id);
      ps.setString(2, "Test Product");
      ps.setInt(3, stockQuantity);
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
  void rejectsNegativeStockQuantity() throws SQLException {
    var savepoint = conn.setSavepoint();
    assertThatThrownBy(() -> insertProduct(-1))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("stock_quantity");
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
}
