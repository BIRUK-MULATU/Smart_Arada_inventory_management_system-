package com.example.inventory.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String sku,
    String imageUrl,
    UUID categoryId,
    String categoryName,
    BigDecimal basePrice,
    // Admin-only - callers must redact this to null before returning it to an EMPLOYEE. Cost
    // price is business-sensitive; it is deliberately never included in SaleItemResponse either.
    BigDecimal costPrice,
    int stockQuantity,
    int lowStockThreshold,
    boolean lowStock,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getSku(),
        product.getImageUrl(),
        product.getCategory().getId(),
        product.getCategory().getName(),
        product.getBasePrice(),
        product.getCostPrice(),
        product.getStockQuantity(),
        product.getLowStockThreshold(),
        product.getStockQuantity() <= product.getLowStockThreshold(),
        product.isActive(),
        product.getCreatedAt(),
        product.getUpdatedAt());
  }

  public ProductResponse withCostPriceRedacted() {
    return new ProductResponse(
        id,
        name,
        sku,
        imageUrl,
        categoryId,
        categoryName,
        basePrice,
        null,
        stockQuantity,
        lowStockThreshold,
        lowStock,
        active,
        createdAt,
        updatedAt);
  }
}
