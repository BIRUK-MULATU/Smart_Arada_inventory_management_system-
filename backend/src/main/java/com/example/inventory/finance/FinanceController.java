package com.example.inventory.finance;

import com.example.inventory.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only throughout (enforced by the /api/finance/** matcher in SecurityConfig). */
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

  private final FinanceService financeService;
  private final ExpenseService expenseService;
  private final BudgetService budgetService;

  public FinanceController(
      FinanceService financeService, ExpenseService expenseService, BudgetService budgetService) {
    this.financeService = financeService;
    this.expenseService = expenseService;
    this.budgetService = budgetService;
  }

  @GetMapping("/summary")
  public FinanceSummaryResponse getSummary(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return financeService.getSummary(from, to);
  }

  @GetMapping("/expenses")
  public Page<ExpenseResponse> listExpenses(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20) Pageable pageable) {
    return expenseService.listExpenses(from, to, pageable);
  }

  @PostMapping("/expenses")
  public ResponseEntity<ExpenseResponse> recordExpense(
      @Valid @RequestBody CreateExpenseRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    ExpenseResponse created = expenseService.recordExpense(request, principal.userId());
    return ResponseEntity.created(URI.create("/api/finance/expenses/" + created.id()))
        .body(created);
  }

  @DeleteMapping("/expenses/{id}")
  public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
    expenseService.deleteExpense(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/budgets")
  public List<BudgetActualResponse> listBudgets() {
    return budgetService.listBudgetsWithActuals();
  }

  @PostMapping("/budgets")
  public ResponseEntity<BudgetResponse> createBudget(
      @Valid @RequestBody CreateBudgetRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    BudgetResponse created = budgetService.createBudget(request, principal.userId());
    return ResponseEntity.created(URI.create("/api/finance/budgets/" + created.id())).body(created);
  }

  @PutMapping("/budgets/{id}")
  public BudgetResponse updateBudgetAmount(
      @PathVariable UUID id, @Valid @RequestBody UpdateBudgetAmountRequest request) {
    return budgetService.updateBudgetAmount(id, request.amount());
  }

  @DeleteMapping("/budgets/{id}")
  public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
    budgetService.deleteBudget(id);
    return ResponseEntity.noContent().build();
  }

  public record UpdateBudgetAmountRequest(
      @jakarta.validation.constraints.NotNull
          @jakarta.validation.constraints.DecimalMin(value = "0.00")
          BigDecimal amount) {}
}
