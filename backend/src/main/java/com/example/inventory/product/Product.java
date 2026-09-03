package com.example.inventory.product;

import com.example.inventory.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column private String sku;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal basePrice;

  @Column(name = "stock_quantity", nullable = false)
  private int stockQuantity = 0;

  @Column(name = "low_stock_threshold", nullable = false)
  private int lowStockThreshold = 0;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  public int getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(int stockQuantity) {
    this.stockQuantity = stockQuantity;
  }

  public int getLowStockThreshold() {
    return lowStockThreshold;
  }

  public void setLowStockThreshold(int lowStockThreshold) {
    this.lowStockThreshold = lowStockThreshold;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
