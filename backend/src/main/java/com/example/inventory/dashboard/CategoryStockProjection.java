package com.example.inventory.dashboard;

import java.util.UUID;

public interface CategoryStockProjection {
  UUID getCategoryId();

  String getCategoryName();

  long getStockOnHand();
}
