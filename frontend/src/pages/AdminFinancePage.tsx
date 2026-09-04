import { useState } from "react";
import { extractErrorMessage } from "../api/errors";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Pagination } from "../components/Pagination";
import type { BudgetFormValues } from "../features/finance/BudgetForm";
import { BudgetForm } from "../features/finance/BudgetForm";
import { BudgetTable } from "../features/finance/BudgetTable";
import type { ExpenseFormValues } from "../features/finance/ExpenseForm";
import { ExpenseForm } from "../features/finance/ExpenseForm";
import { ExpenseTable } from "../features/finance/ExpenseTable";
import { FinanceSummaryCards } from "../features/finance/FinanceSummaryCards";
import {
  useBudgets,
  useCreateBudget,
  useDeleteBudget,
  useDeleteExpense,
  useExpenses,
  useFinanceSummary,
  useRecordExpense,
} from "../features/finance/useFinance";
import type { BudgetActual, Expense } from "../types/finance";

const DEFAULT_PERIOD_DAYS = 30;

function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function AdminFinancePage() {
  const [{ from, to }] = useState(() => ({
    to: isoDate(new Date()),
    from: isoDate(new Date(Date.now() - (DEFAULT_PERIOD_DAYS - 1) * 24 * 60 * 60 * 1000)),
  }));

  const { data: summary, isLoading: summaryLoading, isError: summaryError } = useFinanceSummary(from, to);
  const [expensePage, setExpensePage] = useState(0);
  const { data: expenses, isLoading: expensesLoading } = useExpenses(from, to, expensePage);
  const { data: budgets, isLoading: budgetsLoading } = useBudgets();

  const recordExpense = useRecordExpense();
  const deleteExpense = useDeleteExpense();
  const createBudget = useCreateBudget();
  const deleteBudget = useDeleteBudget();

  const [expenseError, setExpenseError] = useState<string | null>(null);
  const [budgetError, setBudgetError] = useState<string | null>(null);

  const handleAddExpense = async (values: ExpenseFormValues) => {
    setExpenseError(null);
    try {
      await recordExpense.mutateAsync({
        category: values.category || undefined,
        description: values.description,
        amount: values.amount,
        incurredOn: values.incurredOn,
      });
    } catch (error) {
      setExpenseError(extractErrorMessage(error));
    }
  };

  const handleDeleteExpense = (expense: Expense) => {
    if (window.confirm(`Delete the "${expense.description}" expense?`)) {
      deleteExpense.mutate(expense.id);
    }
  };

  const handleAddBudget = async (values: BudgetFormValues) => {
    setBudgetError(null);
    try {
      await createBudget.mutateAsync({
        category: values.category || undefined,
        periodType: values.periodType,
        periodStart: values.periodStart,
        amount: values.amount,
      });
    } catch (error) {
      setBudgetError(extractErrorMessage(error));
    }
  };

  const handleDeleteBudget = (budget: BudgetActual) => {
    if (window.confirm(`Delete the ${budget.category} budget?`)) {
      deleteBudget.mutate(budget.id);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Finance</h1>
        <p className="text-sm text-slate-500">
          {from} – {to}
        </p>
      </div>

      {summaryLoading && <LoadingSpinner />}
      {summaryError && <ErrorMessage message="Couldn't load the finance summary." />}
      {summary && <FinanceSummaryCards summary={summary} />}

      <div className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold text-slate-900">Budgets</h2>
        <BudgetForm onSubmit={handleAddBudget} />
        {budgetError && <ErrorMessage message={budgetError} />}
        {budgetsLoading && <LoadingSpinner />}
        {budgets && budgets.length === 0 && <EmptyState message="No budgets set yet." />}
        {budgets && budgets.length > 0 && <BudgetTable budgets={budgets} onDelete={handleDeleteBudget} />}
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold text-slate-900">Expenses</h2>
        <ExpenseForm onSubmit={handleAddExpense} />
        {expenseError && <ErrorMessage message={expenseError} />}
        {expensesLoading && <LoadingSpinner />}
        {expenses && expenses.content.length === 0 && <EmptyState message="No expenses logged in this period." />}
        {expenses && expenses.content.length > 0 && (
          <div className="flex flex-col gap-3">
            <ExpenseTable expenses={expenses.content} onDelete={handleDeleteExpense} />
            <Pagination page={expensePage} totalPages={expenses.page.totalPages} onPageChange={setExpensePage} />
          </div>
        )}
      </div>
    </div>
  );
}
