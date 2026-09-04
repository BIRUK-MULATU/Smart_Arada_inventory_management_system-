package com.example.inventory.sale;

import com.example.inventory.dashboard.DailySalesProjection;
import com.example.inventory.dashboard.EmployeeSalesProjection;
import com.example.inventory.dashboard.RecentSaleProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

  /**
   * Eagerly fetches employee, resolvedBy, items, and each item's product so the result is safe to
   * read (including by {@code SaleResponse.from}) outside of any transaction - this is used from
   * {@code SaleService.createSale}/{@code SyncService.syncSale}'s idempotency pre-check and
   * race-condition fallback, both of which run without an open Hibernate session by the time the
   * result is converted to a DTO.
   */
  @Query(
      "SELECT DISTINCT s FROM Sale s "
          + "JOIN FETCH s.employee "
          + "LEFT JOIN FETCH s.resolvedBy "
          + "LEFT JOIN FETCH s.items i "
          + "LEFT JOIN FETCH i.product "
          + "WHERE s.clientTransactionId = :clientTransactionId")
  Optional<Sale> findByClientTransactionId(@Param("clientTransactionId") UUID clientTransactionId);

  Page<Sale> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<Sale> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId, Pageable pageable);

  Page<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status, Pageable pageable);

  @Query(
      "SELECT s.id AS id, s.employee.name AS employeeName, s.totalAmount AS totalAmount, "
          + "s.createdAt AS createdAt FROM Sale s ORDER BY s.createdAt DESC")
  List<RecentSaleProjection> findRecentSales(Pageable pageable);

  long countByCreatedAtBetween(Instant from, Instant to);

  @Query(
      "SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.createdAt BETWEEN :from AND :to")
  BigDecimal sumRevenueBetween(@Param("from") Instant from, @Param("to") Instant to);

  @Query(
      "SELECT s.employee.id AS employeeId, s.employee.name AS employeeName, COUNT(s) AS salesCount, "
          + "COALESCE(SUM(s.totalAmount), 0) AS revenue FROM Sale s WHERE s.createdAt BETWEEN :from AND :to "
          + "GROUP BY s.employee.id, s.employee.name ORDER BY SUM(s.totalAmount) DESC")
  List<EmployeeSalesProjection> findSalesByEmployee(
      @Param("from") Instant from, @Param("to") Instant to);

  /**
   * {@code unit} is a Postgres date_trunc() unit ('day', 'month', or 'year'), passed as an ordinary
   * bind parameter (safe from injection either way) but always sourced from {@link
   * com.example.inventory.dashboard.SalesGranularity#truncUnit()} rather than directly from request
   * input, so only the three values that enum defines can ever reach this query.
   */
  @Query(
      value =
          "SELECT bucket AS day, COUNT(*) AS sales_count, COALESCE(SUM(total_amount), 0) AS revenue "
              + "FROM (SELECT date_trunc(:unit, created_at)::date AS bucket, total_amount FROM sales "
              + "      WHERE created_at BETWEEN :from AND :to) bucketed "
              + "GROUP BY bucket ORDER BY bucket",
      nativeQuery = true)
  List<DailySalesProjection> findSalesGroupedByPeriod(
      @Param("from") Instant from, @Param("to") Instant to, @Param("unit") String unit);
}
