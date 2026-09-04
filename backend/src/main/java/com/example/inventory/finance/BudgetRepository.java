package com.example.inventory.finance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

  List<Budget> findAllByOrderByPeriodStartDesc();

  boolean existsByCategoryAndPeriodTypeAndPeriodStartAndPeriodEnd(
      String category, PeriodType periodType, LocalDate periodStart, LocalDate periodEnd);
}
