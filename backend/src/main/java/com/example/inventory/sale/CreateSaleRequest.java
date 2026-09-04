package com.example.inventory.sale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * bankAccount is intentionally not validated here (e.g. with @NotBlank) - it's only required when
 * paymentMethod is BANK, and Bean Validation can't express that conditional cleanly. Both sale
 * executors check it explicitly instead, right before building the Sale.
 */
public record CreateSaleRequest(
    @NotNull UUID clientTransactionId,
    @NotEmpty List<@Valid CreateSaleItemRequest> items,
    @NotNull PaymentMethod paymentMethod,
    String bankAccount) {}
