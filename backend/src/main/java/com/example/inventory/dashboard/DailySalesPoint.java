package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesPoint(LocalDate day, long salesCount, BigDecimal revenue) {

  public static DailySalesPoint from(DailySalesProjection projection) {
    return new DailySalesPoint(
        projection.getDay(), projection.getSalesCount(), projection.getRevenue());
  }
}
