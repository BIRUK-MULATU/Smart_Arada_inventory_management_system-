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

export type PeriodType = "MONTHLY" | "QUARTERLY" | "YEARLY" | "CUSTOM";

export interface BudgetActual {
  id: string;
  category: string;
  periodType: PeriodType;
  periodStart: string;
  periodEnd: string;
  budgetAmount: number;
  actualAmount: number;
}

export interface CreateBudgetRequest {
  category?: string;
  periodType: PeriodType;
  periodStart: string;
  /** Required for CUSTOM; ignored (recomputed server-side) for every other period type. */
  periodEnd?: string;
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
