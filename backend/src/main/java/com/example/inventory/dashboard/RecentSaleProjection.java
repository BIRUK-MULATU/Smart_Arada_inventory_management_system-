package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface RecentSaleProjection {
  UUID getId();

  String getEmployeeName();

  BigDecimal getTotalAmount();

  Instant getCreatedAt();
}
