package com.example.inventory.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

  Page<Expense> findByIncurredOnBetweenOrderByIncurredOnDesc(
      LocalDate from, LocalDate to, Pageable pageable);

  @Query(
      "SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.incurredOn BETWEEN :from AND :to")
  BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

  @Query(
      "SELECT COALESCE(SUM(e.amount), 0) FROM Expense e "
          + "WHERE e.category = :category AND e.incurredOn BETWEEN :from AND :to")
  BigDecimal sumAmountByCategoryBetween(
      @Param("category") String category, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
