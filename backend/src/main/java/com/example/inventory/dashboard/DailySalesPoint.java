package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One bucketed point in a sales-over-time series. {@code day} is the bucket's start date - the
 * calendar day itself for DAILY granularity, or the 1st of the month/year for MONTHLY/YEARLY.
 */
public record DailySalesPoint(LocalDate day, long salesCount, BigDecimal revenue) {

  public static DailySalesPoint from(DailySalesProjection projection) {
    return new DailySalesPoint(
        projection.getDay(), projection.getSalesCount(), projection.getRevenue());
  }
}
