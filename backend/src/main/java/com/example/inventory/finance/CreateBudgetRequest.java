package com.example.inventory.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBudgetRequest(
    String category,
    @NotNull PeriodType periodType,
    @NotNull LocalDate periodStart,
    @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}
