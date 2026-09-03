package com.example.inventory.sale;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateSaleItemRequest(
    @NotNull UUID productId,
    @NotNull @Positive Integer quantity,
    @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal sellingPrice) {}
