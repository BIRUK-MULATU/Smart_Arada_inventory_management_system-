package com.example.inventory.finance;

import com.example.inventory.common.BaseEntity;
import com.example.inventory.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget extends BaseEntity {

  @Column(nullable = false)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "period_type", nullable = false)
  private PeriodType periodType;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  /**
   * Explicit end of this budget's period (inclusive), rather than derived purely from periodType -
   * that derivation can't express a CUSTOM admin-picked range. Computed server-side for
   * MONTHLY/QUARTERLY/YEARLY, supplied by the admin for CUSTOM.
   */
  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public PeriodType getPeriodType() {
    return periodType;
  }

  public void setPeriodType(PeriodType periodType) {
    this.periodType = periodType;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public void setPeriodStart(LocalDate periodStart) {
    this.periodStart = periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public void setPeriodEnd(LocalDate periodEnd) {
    this.periodEnd = periodEnd;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }
}
