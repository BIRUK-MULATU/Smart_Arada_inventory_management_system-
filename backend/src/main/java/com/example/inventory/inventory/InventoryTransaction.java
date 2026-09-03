package com.example.inventory.inventory;

import com.example.inventory.common.ImmutableEntity;
import com.example.inventory.product.Product;
import com.example.inventory.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction extends ImmutableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransactionType type;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "previous_quantity", nullable = false)
  private int previousQuantity;

  @Column(name = "new_quantity", nullable = false)
  private int newQuantity;

  @Column(name = "reference_type")
  private String referenceType;

  @Column(name = "reference_id")
  private UUID referenceId;

  @Column private String reason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "performed_by", nullable = false)
  private User performedBy;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public TransactionType getType() {
    return type;
  }

  public void setType(TransactionType type) {
    this.type = type;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public int getPreviousQuantity() {
    return previousQuantity;
  }

  public void setPreviousQuantity(int previousQuantity) {
    this.previousQuantity = previousQuantity;
  }

  public int getNewQuantity() {
    return newQuantity;
  }

  public void setNewQuantity(int newQuantity) {
    this.newQuantity = newQuantity;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(String referenceType) {
    this.referenceType = referenceType;
  }

  public UUID getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(UUID referenceId) {
    this.referenceId = referenceId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public User getPerformedBy() {
    return performedBy;
  }

  public void setPerformedBy(User performedBy) {
    this.performedBy = performedBy;
  }
}
