export interface Expense {
  id: string;
  category: string;
  description: string;
  amount: number;
  incurredOn: string;
  recordedByUserId: string;
  recordedByName: string;
  createdAt: string;
}

export interface CreateExpenseRequest {
  category?: string;
  description: string;
  amount: number;
  incurredOn: string;
}

export type PeriodType = "MONTHLY" | "YEARLY";

export interface BudgetActual {
  id: string;
  category: string;
  periodType: PeriodType;
  periodStart: string;
  budgetAmount: number;
  actualAmount: number;
}

export interface CreateBudgetRequest {
  category?: string;
  periodType: PeriodType;
  periodStart: string;
  amount: number;
}

export interface FinanceSummary {
  from: string;
  to: string;
  revenue: number;
  costOfGoodsSold: number;
  grossProfit: number;
  totalExpenses: number;
  netProfit: number;
}
