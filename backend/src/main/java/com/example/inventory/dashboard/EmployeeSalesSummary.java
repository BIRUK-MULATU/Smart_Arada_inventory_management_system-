package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record EmployeeSalesSummary(
    UUID employeeId, String employeeName, long salesCount, BigDecimal revenue) {

  public static EmployeeSalesSummary from(EmployeeSalesProjection projection) {
    return new EmployeeSalesSummary(
        projection.getEmployeeId(),
        projection.getEmployeeName(),
        projection.getSalesCount(),
        projection.getRevenue());
  }
}
