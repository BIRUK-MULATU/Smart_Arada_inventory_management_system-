import { Button } from "../../components/Button";
import type { BudgetActual } from "../../types/finance";

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
          <div key={budget.id} className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">{budget.category}</p>
                <p className="text-xs text-slate-500">
                  {budget.periodType === "MONTHLY" ? "Monthly" : "Yearly"} · starting {budget.periodStart}
                </p>
              </div>
              <div className="text-right">
                <p className={`font-medium ${overBudget ? "text-red-600" : "text-slate-900"}`}>
                  ${budget.actualAmount.toFixed(2)} / ${budget.budgetAmount.toFixed(2)}
                </p>
                <Button variant="danger" onClick={() => onDelete(budget)}>
                  Delete
                </Button>
              </div>
            </div>
            <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-slate-100">
              <div
                className={`h-full ${overBudget ? "bg-red-500" : "bg-slate-900"}`}
                style={{ width: `${Math.min(100, percentUsed)}%` }}
              />
            </div>
          </div>
        );
      })}
    </div>
  );
}
