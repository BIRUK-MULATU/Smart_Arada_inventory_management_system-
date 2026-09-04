import { Button } from "../../components/Button";
import type { BudgetActual, PeriodType } from "../../types/finance";

const PERIOD_LABELS: Record<PeriodType, string> = {
  MONTHLY: "Monthly",
  QUARTERLY: "Quarterly",
  YEARLY: "Yearly",
  CUSTOM: "Custom",
};

interface BudgetTableProps {
  budgets: BudgetActual[];
  onDelete: (budget: BudgetActual) => void;
}

export function BudgetTable({ budgets, onDelete }: BudgetTableProps) {
  return (
    <div className="flex flex-col gap-3">
      {budgets.map((budget) => {
        const percentUsed = budget.budgetAmount > 0 ? (budget.actualAmount / budget.budgetAmount) * 100 : 0;
        const overBudget = budget.actualAmount > budget.budgetAmount;
        return (
          <div key={budget.id} className="rounded-lg border border-ink-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-ink-900">{budget.category}</p>
                <p className="text-xs text-ink-500">
                  {PERIOD_LABELS[budget.periodType]} · {budget.periodStart} to {budget.periodEnd}
                </p>
              </div>
              <div className="text-right">
                <p className={`font-medium ${overBudget ? "text-red-600" : "text-ink-900"}`}>
                  ${budget.actualAmount.toFixed(2)} / ${budget.budgetAmount.toFixed(2)}
                </p>
                <Button variant="danger" onClick={() => onDelete(budget)}>
                  Delete
                </Button>
              </div>
            </div>
            <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-ink-100">
              <div
                className={`h-full transition-all duration-300 ${overBudget ? "bg-red-500" : "bg-gold-500"}`}
                style={{ width: `${Math.min(100, percentUsed)}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}
