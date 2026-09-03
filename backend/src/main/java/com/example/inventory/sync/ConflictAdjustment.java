package com.example.inventory.sync;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * {@code restockQuantity} of 0 means the admin is accepting the shortfall as a loss for this
 * product - no stock change, but the resolution as a whole still clears the conflict.
 */
public record ConflictAdjustment(
    @NotNull UUID productId, @NotNull @Min(0) Integer restockQuantity) {}
