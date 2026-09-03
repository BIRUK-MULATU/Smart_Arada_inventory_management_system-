package com.example.inventory.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StockInRequest(
    @NotNull UUID productId, @NotNull @Positive Integer quantity, @Size(max = 500) String reason) {}
