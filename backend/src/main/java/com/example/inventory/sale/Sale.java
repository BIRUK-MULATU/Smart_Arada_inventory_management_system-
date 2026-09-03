package com.example.inventory.sale;

import com.example.inventory.common.BaseEntity;
import com.example.inventory.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

  public List<SaleItem> getItems() {
    return items;
  }

  public void addItem(SaleItem item) {
    item.setSale(this);
    items.add(item);
  }
}
