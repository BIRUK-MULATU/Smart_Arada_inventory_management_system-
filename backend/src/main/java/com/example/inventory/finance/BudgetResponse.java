package com.example.inventory.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(
    UUID id,
    String category,
    PeriodType periodType,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal amount) {

  public static BudgetResponse from(Budget budget) {
    return new BudgetResponse(
        budget.getId(),
        budget.getCategory(),
        budget.getPeriodType(),
        budget.getPeriodStart(),
        budget.getPeriodEnd(),
        budget.getAmount());
  }
}
