package com.example.inventory.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String sku,
    String imageUrl,
    BigDecimal basePrice,
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
        product.getBasePrice(),
        product.getStockQuantity(),
        product.getLowStockThreshold(),
        product.getStockQuantity() <= product.getLowStockThreshold(),
        product.isActive(),
        product.getCreatedAt(),
        product.getUpdatedAt());
  }
}
