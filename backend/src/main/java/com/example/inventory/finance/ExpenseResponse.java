package com.example.inventory.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    String category,
    String description,
    BigDecimal amount,
    LocalDate incurredOn,
    UUID recordedByUserId,
    String recordedByName,
    Instant createdAt) {

  public static ExpenseResponse from(Expense expense) {
    return new ExpenseResponse(
        expense.getId(),
        expense.getCategory(),
        expense.getDescription(),
        expense.getAmount(),
        expense.getIncurredOn(),
        expense.getRecordedBy().getId(),
        expense.getRecordedBy().getName(),
        expense.getCreatedAt());
  }
}
