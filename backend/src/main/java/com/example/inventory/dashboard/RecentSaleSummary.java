package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecentSaleSummary(
    UUID id, String employeeName, BigDecimal totalAmount, Instant createdAt) {

  public static RecentSaleSummary from(RecentSaleProjection projection) {
    return new RecentSaleSummary(
        projection.getId(),
        projection.getEmployeeName(),
        projection.getTotalAmount(),
        projection.getCreatedAt());
  }
}
