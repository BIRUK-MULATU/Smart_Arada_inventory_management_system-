package com.example.inventory.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetActualResponse(
    UUID id,
    String category,
    PeriodType periodType,
    LocalDate periodStart,
    BigDecimal budgetAmount,
    BigDecimal actualAmount) {}
