package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public interface TopProductProjection {
  UUID getProductId();

  String getProductName();

  long getUnitsSold();

  BigDecimal getRevenue();
}
