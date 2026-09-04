package com.example.inventory.product;

import com.example.inventory.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal basePrice;

  /** Cost-of-goods basis, admin-only data - never exposed to an EMPLOYEE via ProductResponse. */
  @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal costPrice = BigDecimal.ZERO;

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

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public BigDecimal getBasePrice() {
    return basePrice;
  }

  public void setBasePrice(BigDecimal basePrice) {
    this.basePrice = basePrice;
  }

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
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
