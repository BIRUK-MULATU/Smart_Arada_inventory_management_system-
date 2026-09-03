package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductResponse(
    UUID productId, String productName, long unitsSold, BigDecimal revenue) {

  public static TopProductResponse from(TopProductProjection projection) {
    return new TopProductResponse(
        projection.getProductId(),
        projection.getProductName(),
        projection.getUnitsSold(),
        projection.getRevenue());
  }
}
