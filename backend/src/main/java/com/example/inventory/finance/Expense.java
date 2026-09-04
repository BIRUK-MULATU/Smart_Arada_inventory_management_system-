package com.example.inventory.finance;

import com.example.inventory.common.BaseEntity;
import com.example.inventory.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(name = "incurred_on", nullable = false)
  private LocalDate incurredOn;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recorded_by", nullable = false)
  private User recordedBy;

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public LocalDate getIncurredOn() {
    return incurredOn;
  }

  public void setIncurredOn(LocalDate incurredOn) {
    this.incurredOn = incurredOn;
  }

  public User getRecordedBy() {
    return recordedBy;
  }

  public void setRecordedBy(User recordedBy) {
    this.recordedBy = recordedBy;
  }
}
