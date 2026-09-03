package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public interface EmployeeSalesProjection {
  UUID getEmployeeId();

  String getEmployeeName();

  long getSalesCount();

  BigDecimal getRevenue();
}
