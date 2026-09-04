package com.example.inventory.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
    @NotBlank String name,
    String sku,
    String imageUrl,
    @NotNull UUID categoryId,
    @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,
    // Optional: admins can set the cost-of-goods basis later by editing the product once known.
    @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal costPrice,
    @NotNull @Min(0) Integer lowStockThreshold) {}
