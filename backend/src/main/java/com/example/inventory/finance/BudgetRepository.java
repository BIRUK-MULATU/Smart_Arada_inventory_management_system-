package com.example.inventory.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

  List<Budget> findAllByOrderByPeriodStartDesc();

  boolean existsByCategoryAndPeriodTypeAndPeriodStart(
      String category, PeriodType periodType, java.time.LocalDate periodStart);
}
