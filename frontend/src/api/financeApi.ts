import { apiClient } from "./client";
import type { Page } from "../types/inventory";
import type {
  BudgetActual,
  CreateBudgetRequest,
  CreateExpenseRequest,
  Expense,
  FinanceSummary,
} from "../types/finance";

export const financeApi = {
  async getSummary(from?: string, to?: string): Promise<FinanceSummary> {
    const response = await apiClient.get<FinanceSummary>("/finance/summary", { params: { from, to } });
    return response.data;
  },

  async listExpenses(from: string, to: string, page = 0, size = 20): Promise<Page<Expense>> {
    const response = await apiClient.get<Page<Expense>>("/finance/expenses", {
      params: { from, to, page, size },
    });
    return response.data;
  },

  async recordExpense(request: CreateExpenseRequest): Promise<Expense> {
    const response = await apiClient.post<Expense>("/finance/expenses", request);
    return response.data;
  },

  async deleteExpense(id: string): Promise<void> {
    await apiClient.delete(`/finance/expenses/${id}`);
  },

  async listBudgets(): Promise<BudgetActual[]> {
    const response = await apiClient.get<BudgetActual[]>("/finance/budgets");
    return response.data;
  },

  async createBudget(request: CreateBudgetRequest): Promise<void> {
    await apiClient.post("/finance/budgets", request);
  },

  async updateBudgetAmount(id: string, amount: number): Promise<void> {
    await apiClient.put(`/finance/budgets/${id}`, { amount });
  },

  async deleteBudget(id: string): Promise<void> {
    await apiClient.delete(`/finance/budgets/${id}`);
  },
};
