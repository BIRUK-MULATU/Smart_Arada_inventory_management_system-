package com.example.inventory.sale;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResponse(
    UUID id,
    UUID productId,
    String productName,
    int quantity,
    BigDecimal sellingPrice,
    BigDecimal subtotal) {

  public static SaleItemResponse from(SaleItem item) {
    return new SaleItemResponse(
        item.getId(),
        item.getProduct().getId(),
        item.getProduct().getName(),
        item.getQuantity(),
        item.getSellingPrice(),
        item.getSubtotal());
  }
}
