package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailySalesProjection {
  LocalDate getDay();

  long getSalesCount();

  BigDecimal getRevenue();
}
