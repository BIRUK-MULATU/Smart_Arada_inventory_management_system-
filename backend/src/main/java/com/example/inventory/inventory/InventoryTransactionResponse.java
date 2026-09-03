package com.example.inventory.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryTransactionResponse(
    UUID id,
    UUID productId,
    String productName,
    TransactionType type,
    int quantity,
    int previousQuantity,
    int newQuantity,
    String referenceType,
    UUID referenceId,
    String reason,
    UUID performedByUserId,
    String performedByName,
    Instant createdAt) {

  public static InventoryTransactionResponse from(InventoryTransaction transaction) {
    return new InventoryTransactionResponse(
        transaction.getId(),
        transaction.getProduct().getId(),
        transaction.getProduct().getName(),
        transaction.getType(),
        transaction.getQuantity(),
        transaction.getPreviousQuantity(),
        transaction.getNewQuantity(),
        transaction.getReferenceType(),
        transaction.getReferenceId(),
        transaction.getReason(),
        transaction.getPerformedBy().getId(),
        transaction.getPerformedBy().getName(),
        transaction.getCreatedAt());
  }
}
