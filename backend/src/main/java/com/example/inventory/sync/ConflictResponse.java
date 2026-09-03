package com.example.inventory.sync;

import com.example.inventory.inventory.InventoryTransaction;
import com.example.inventory.sale.Sale;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ConflictResponse(
    UUID saleId,
    UUID employeeId,
    String employeeName,
    BigDecimal totalAmount,
    Instant syncedAt,
    List<ConflictItemResponse> items) {

  public static ConflictResponse from(Sale sale, List<InventoryTransaction> stockOutTransactions) {
    Map<UUID, InventoryTransaction> byProduct =
        stockOutTransactions.stream()
            .collect(
                Collectors.toMap(
                    transaction -> transaction.getProduct().getId(), Function.identity()));

    List<ConflictItemResponse> items =
        sale.getItems().stream()
            .map(
                item -> {
                  InventoryTransaction transaction = byProduct.get(item.getProduct().getId());
                  int shortfall =
                      transaction == null
                          ? 0
                          : Math.max(
                              0, transaction.getQuantity() - transaction.getPreviousQuantity());
                  return new ConflictItemResponse(
                      item.getProduct().getId(),
                      item.getProduct().getName(),
                      item.getQuantity(),
                      shortfall);
                })
            .toList();

    return new ConflictResponse(
        sale.getId(),
        sale.getEmployee().getId(),
        sale.getEmployee().getName(),
        sale.getTotalAmount(),
        sale.getCreatedAt(),
        items);
  }
}
