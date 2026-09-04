package com.example.inventory.finance;

import com.example.inventory.exception.BadRequestException;
import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.math.BigDecimal;
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
    Period period = resolvePeriod(request.periodType(), request.periodStart(), request.periodEnd());

    if (budgetRepository.existsByCategoryAndPeriodTypeAndPeriodStartAndPeriodEnd(
        category, request.periodType(), period.start(), period.end())) {
      throw new ConflictException("A budget already exists for " + category + " in that period");
    }

    Budget budget = new Budget();
    budget.setCategory(category);
    budget.setPeriodType(request.periodType());
    budget.setPeriodStart(period.start());
    budget.setPeriodEnd(period.end());
    budget.setAmount(request.amount());
    budget.setCreatedBy(createdBy);

    return BudgetResponse.from(budgetRepository.save(budget));
  }

  @Transactional
  public BudgetResponse updateBudgetAmount(UUID id, BigDecimal amount) {
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
              BigDecimal actual =
                  expenseRepository.sumAmountByCategoryBetween(
                      budget.getCategory(), budget.getPeriodStart(), budget.getPeriodEnd());
              return new BudgetActualResponse(
                  budget.getId(),
                  budget.getCategory(),
                  budget.getPeriodType(),
                  budget.getPeriodStart(),
                  budget.getPeriodEnd(),
                  budget.getAmount(),
                  actual);
            })
        .toList();
  }

  /**
   * MONTHLY/QUARTERLY/YEARLY are normalized to the calendar period containing the requested start
   * date and their end date is always computed server-side (any submitted periodEnd is ignored, so
   * the two can never disagree). CUSTOM is taken from the request exactly as given.
   */
  private Period resolvePeriod(PeriodType periodType, LocalDate periodStart, LocalDate periodEnd) {
    return switch (periodType) {
      case MONTHLY -> {
        LocalDate start = periodStart.withDayOfMonth(1);
        yield new Period(start, start.plusMonths(1).minusDays(1));
      }
      case QUARTERLY -> {
        int quarterStartMonth = ((periodStart.getMonthValue() - 1) / 3) * 3 + 1;
        LocalDate start = periodStart.withMonth(quarterStartMonth).withDayOfMonth(1);
        yield new Period(start, start.plusMonths(3).minusDays(1));
      }
      case YEARLY -> {
        LocalDate start = periodStart.withDayOfYear(1);
        yield new Period(start, start.plusYears(1).minusDays(1));
      }
      case CUSTOM -> {
        if (periodEnd == null) {
          throw new BadRequestException("periodEnd is required for a CUSTOM budget period");
        }
        if (periodEnd.isBefore(periodStart)) {
          throw new BadRequestException("periodEnd cannot be before periodStart");
        }
        yield new Period(periodStart, periodEnd);
      }
    };
  }

  private record Period(LocalDate start, LocalDate end) {}
}
