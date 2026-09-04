package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public interface CategorySalesProjection {
  UUID getCategoryId();

  String getCategoryName();

  long getUnitsSold();

  BigDecimal getRevenue();
}
