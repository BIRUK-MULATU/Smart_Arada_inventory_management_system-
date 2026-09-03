package com.example.inventory.sale;

/**
 * {@code created} is false when this call recognized an idempotent retry instead of inserting a new
 * sale.
 */
public record SaleCreationResult(SaleResponse sale, boolean created) {}
