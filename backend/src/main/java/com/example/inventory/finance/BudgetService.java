package com.example.inventory.finance;

import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

  private final BudgetRepository budgetRepository;
  private final ExpenseRepository expenseRepository;
  private final UserRepository userRepository;

  public BudgetService(
      BudgetRepository budgetRepository,
      ExpenseRepository expenseRepository,
      UserRepository userRepository) {
    this.budgetRepository = budgetRepository;
    this.expenseRepository = expenseRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public BudgetResponse createBudget(CreateBudgetRequest request, UUID createdByUserId) {
    User createdBy =
        userRepository
            .findById(createdByUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + createdByUserId));

    String category = ExpenseService.normalizeCategory(request.category());
    LocalDate periodStart = normalizePeriodStart(request.periodType(), request.periodStart());

    if (budgetRepository.existsByCategoryAndPeriodTypeAndPeriodStart(
        category, request.periodType(), periodStart)) {
      throw new ConflictException("A budget already exists for " + category + " in that period");
    }

    Budget budget = new Budget();
    budget.setCategory(category);
    budget.setPeriodType(request.periodType());
    budget.setPeriodStart(periodStart);
    budget.setAmount(request.amount());
    budget.setCreatedBy(createdBy);

    return BudgetResponse.from(budgetRepository.save(budget));
  }

  @Transactional
  public BudgetResponse updateBudgetAmount(UUID id, java.math.BigDecimal amount) {
    Budget budget =
        budgetRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    budget.setAmount(amount);
    return BudgetResponse.from(budgetRepository.save(budget));
  }

  @Transactional
  public void deleteBudget(UUID id) {
    if (!budgetRepository.existsById(id)) {
      throw new ResourceNotFoundException("Budget not found: " + id);
    }
    budgetRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<BudgetActualResponse> listBudgetsWithActuals() {
    return budgetRepository.findAllByOrderByPeriodStartDesc().stream()
        .map(
            budget -> {
              java.math.BigDecimal actual =
                  expenseRepository.sumAmountByCategoryBetween(
                      budget.getCategory(), budget.getPeriodStart(), budget.periodEndInclusive());
              return new BudgetActualResponse(
                  budget.getId(),
                  budget.getCategory(),
                  budget.getPeriodType(),
                  budget.getPeriodStart(),
                  budget.getAmount(),
                  actual);
            })
        .toList();
  }

  private LocalDate normalizePeriodStart(PeriodType periodType, LocalDate periodStart) {
    return periodType == PeriodType.MONTHLY
        ? periodStart.withDayOfMonth(1)
        : periodStart.withDayOfYear(1);
  }
}
