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
    PaymentMethod paymentMethod,
    String bankAccount,
    SaleStatus status,
    UUID resolvedByUserId,
    String resolvedByName,
    Instant resolvedAt,
    String resolutionNote,
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
        sale.getPaymentMethod(),
        sale.getBankAccount(),
        sale.getStatus(),
        sale.getResolvedBy() != null ? sale.getResolvedBy().getId() : null,
        sale.getResolvedBy() != null ? sale.getResolvedBy().getName() : null,
        sale.getResolvedAt(),
        sale.getResolutionNote(),
        sale.getItems().stream().map(SaleItemResponse::from).toList(),
        sale.getCreatedAt(),
        sale.getUpdatedAt());
  }
}
