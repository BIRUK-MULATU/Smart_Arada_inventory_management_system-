package com.example.inventory.sale;

import com.example.inventory.common.BaseEntity;
import com.example.inventory.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sales")
public class Sale extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private User employee;

  @Column(name = "client_transaction_id", nullable = false)
  private UUID clientTransactionId;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod = PaymentMethod.CASH;

  /** Only ever set when paymentMethod is BANK - the account the employee received payment into. */
  @Column(name = "bank_account")
  private String bankAccount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SaleStatus status = SaleStatus.COMPLETED;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolved_by")
  private User resolvedBy;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolution_note")
  private String resolutionNote;

  @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<SaleItem> items = new ArrayList<>();

  public User getEmployee() {
    return employee;
  }

  public void setEmployee(User employee) {
    this.employee = employee;
  }

  public UUID getClientTransactionId() {
    return clientTransactionId;
  }

  public void setClientTransactionId(UUID clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getBankAccount() {
    return bankAccount;
  }

  public void setBankAccount(String bankAccount) {
    this.bankAccount = bankAccount;
  }

  public List<SaleItem> getItems() {
    return items;
  }

  public void addItem(SaleItem item) {
    item.setSale(this);
    items.add(item);
  }

  public SaleStatus getStatus() {
    return status;
  }

  public void setStatus(SaleStatus status) {
    this.status = status;
  }

  public User getResolvedBy() {
    return resolvedBy;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public String getResolutionNote() {
    return resolutionNote;
  }

  public void resolveConflict(User resolvedBy, Instant resolvedAt, String note) {
    this.status = SaleStatus.RESOLVED;
    this.resolvedBy = resolvedBy;
    this.resolvedAt = resolvedAt;
    this.resolutionNote = note;
  }
}
