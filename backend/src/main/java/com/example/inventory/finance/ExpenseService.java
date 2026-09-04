package com.example.inventory.finance;

import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

  private static final String DEFAULT_CATEGORY = "Overall";

  private final ExpenseRepository expenseRepository;
  private final UserRepository userRepository;

  public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository) {
    this.expenseRepository = expenseRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public ExpenseResponse recordExpense(CreateExpenseRequest request, UUID recordedByUserId) {
    User recordedBy =
        userRepository
            .findById(recordedByUserId)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found: " + recordedByUserId));

    Expense expense = new Expense();
    expense.setCategory(normalizeCategory(request.category()));
    expense.setDescription(request.description());
    expense.setAmount(request.amount());
    expense.setIncurredOn(request.incurredOn());
    expense.setRecordedBy(recordedBy);

    return ExpenseResponse.from(expenseRepository.save(expense));
  }

  @Transactional(readOnly = true)
  public Page<ExpenseResponse> listExpenses(LocalDate from, LocalDate to, Pageable pageable) {
    return expenseRepository
        .findByIncurredOnBetweenOrderByIncurredOnDesc(from, to, pageable)
        .map(ExpenseResponse::from);
  }

  @Transactional
  public void deleteExpense(UUID id) {
    if (!expenseRepository.existsById(id)) {
      throw new ResourceNotFoundException("Expense not found: " + id);
    }
    expenseRepository.deleteById(id);
  }

  static String normalizeCategory(String category) {
    return category == null || category.isBlank() ? DEFAULT_CATEGORY : category.trim();
  }
}
