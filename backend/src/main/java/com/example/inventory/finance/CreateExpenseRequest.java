package com.example.inventory.finance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseRequest(
    String category,
    @NotBlank String description,
    @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
    @NotNull LocalDate incurredOn) {}
