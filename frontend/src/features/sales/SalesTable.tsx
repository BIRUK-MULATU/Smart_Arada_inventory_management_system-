import { Badge } from "../../components/Badge";
import type { Sale, SaleStatus } from "../../types/sale";

const statusTone: Record<SaleStatus, "neutral" | "success" | "warning" | "danger"> = {
  COMPLETED: "success",
  CONFLICT: "danger",
  RESOLVED: "neutral",
};

const statusLabel: Record<SaleStatus, string> = {
  COMPLETED: "Recorded",
  CONFLICT: "Needs review",
  RESOLVED: "Resolved",
};

export function SalesTable({ sales, showEmployee }: { sales: Sale[]; showEmployee: boolean }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-ink-200 bg-white">
      <table className="min-w-full divide-y divide-ink-200 text-sm">
        <thead className="bg-ink-50">
          <tr>
            {showEmployee && <th className="px-4 py-2 text-left font-medium text-ink-600">Employee</th>}
            <th className="px-4 py-2 text-left font-medium text-ink-600">Items</th>
            <th className="px-4 py-2 text-right font-medium text-ink-600">Total</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Payment</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">When</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Status</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-100">
          {sales.map((sale) => (
            <tr key={sale.id}>
              {showEmployee && <td className="px-4 py-2 text-ink-900">{sale.employeeName}</td>}
              <td className="px-4 py-2 text-ink-600">
                {sale.items.map((item) => `${item.productName} ×${item.quantity}`).join(", ")}
              </td>
              <td className="px-4 py-2 text-right font-medium text-ink-900">${sale.totalAmount.toFixed(2)}</td>
              <td className="px-4 py-2 text-ink-600">
                {sale.paymentMethod === "BANK" ? (
                  <>
                    Bank
                    {sale.bankAccount && <span className="block text-xs text-ink-400">{sale.bankAccount}</span>}
                  </>
                ) : (
                  "Cash"
                )}
              </td>
              <td className="px-4 py-2 text-ink-500">{new Date(sale.createdAt).toLocaleString()}</td>
              <td className="px-4 py-2">
                <Badge tone={statusTone[sale.status]}>{statusLabel[sale.status]}</Badge>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
