package com.example.inventory.sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
    UUID id,
    UUID employeeId,
    String employeeName,
    UUID clientTransactionId,
    BigDecimal totalAmount,
    List<SaleItemResponse> items,
    Instant createdAt,
    Instant updatedAt) {

  public static SaleResponse from(Sale sale) {
    return new SaleResponse(
        sale.getId(),
        sale.getEmployee().getId(),
        sale.getEmployee().getName(),
        sale.getClientTransactionId(),
        sale.getTotalAmount(),
        sale.getItems().stream().map(SaleItemResponse::from).toList(),
        sale.getCreatedAt(),
        sale.getUpdatedAt());
  }
}
