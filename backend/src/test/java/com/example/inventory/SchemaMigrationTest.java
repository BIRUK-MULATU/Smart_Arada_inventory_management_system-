package com.example.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class SchemaMigrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .load()
        .migrate();
  }

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  private UUID insertUser(Connection conn, String email) throws SQLException {
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

  private UUID insertProduct(Connection conn, int stockQuantity) throws SQLException {
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

  private void insertSale(Connection conn, UUID employeeId, UUID clientTxnId) throws SQLException {
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
    try (Connection conn = connect()) {
      UUID employeeId = insertUser(conn, "dup-test@example.com");
      UUID clientTxnId = UUID.randomUUID();

      insertSale(conn, employeeId, clientTxnId);

      assertThatThrownBy(() -> insertSale(conn, employeeId, clientTxnId))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("ux_sales_client_transaction_id");
    }
  }

  @Test
  void rejectsNegativeStockQuantity() throws SQLException {
    try (Connection conn = connect()) {
      assertThatThrownBy(() -> insertProduct(conn, -1))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("stock_quantity");
    }
  }

  @Test
  void rejectsInconsistentInventoryLedger() throws SQLException {
    try (Connection conn = connect()) {
      UUID productId = insertProduct(conn, 10);
      UUID userId = insertUser(conn, "ledger-test@example.com");

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
    }
  }

  @Test
  void acceptsConsistentInventoryLedgerEntry() throws SQLException {
    try (Connection conn = connect()) {
      UUID productId = insertProduct(conn, 10);
      UUID userId = insertUser(conn, "valid-ledger@example.com");

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
}
