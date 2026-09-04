import { Button } from "../../components/Button";
import type { Expense } from "../../types/finance";

interface ExpenseTableProps {
  expenses: Expense[];
  onDelete: (expense: Expense) => void;
}

export function ExpenseTable({ expenses, onDelete }: ExpenseTableProps) {
  return (
    <div className="overflow-x-auto rounded-lg border border-ink-200 bg-white">
      <table className="min-w-full divide-y divide-ink-200 text-sm">
        <thead className="bg-ink-50">
          <tr>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Date</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Category</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Description</th>
            <th className="px-4 py-2 text-right font-medium text-ink-600">Amount</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Recorded by</th>
            <th className="px-4 py-2" />
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-100">
          {expenses.map((expense) => (
            <tr key={expense.id}>
              <td className="px-4 py-2 text-ink-500">{expense.incurredOn}</td>
              <td className="px-4 py-2 text-ink-600">{expense.category}</td>
              <td className="px-4 py-2 text-ink-900">{expense.description}</td>
              <td className="px-4 py-2 text-right font-medium text-ink-900">${expense.amount.toFixed(2)}</td>
              <td className="px-4 py-2 text-ink-500">{expense.recordedByName}</td>
              <td className="px-4 py-2 text-right">
                <Button variant="danger" onClick={() => onDelete(expense)}>
                  Delete
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
