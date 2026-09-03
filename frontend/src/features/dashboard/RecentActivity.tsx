import { Card } from "../../components/Card";
import { EmptyState } from "../../components/EmptyState";
import type { DashboardSummary } from "../../types/dashboard";

export function RecentActivity({ summary }: { summary: DashboardSummary }) {
  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
      <Card>
        <h2 className="mb-2 text-sm font-medium text-slate-600">Recent sales</h2>
        {summary.recentSales.length === 0 && <EmptyState message="No sales yet." />}
        {summary.recentSales.length > 0 && (
          <ul className="divide-y divide-slate-100">
            {summary.recentSales.map((sale) => (
              <li key={sale.id} className="flex items-center justify-between py-2 text-sm">
                <span className="text-slate-900">{sale.employeeName}</span>
                <span className="text-slate-600">${sale.totalAmount.toFixed(2)}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>
      <Card>
        <h2 className="mb-2 text-sm font-medium text-slate-600">Recent inventory movements</h2>
        {summary.recentInventoryMovements.length === 0 && <EmptyState message="No inventory movements yet." />}
        {summary.recentInventoryMovements.length > 0 && (
          <ul className="divide-y divide-slate-100">
            {summary.recentInventoryMovements.map((movement) => (
              <li key={movement.id} className="flex items-center justify-between py-2 text-sm">
                <span className="text-slate-900">{movement.productName}</span>
                <span className={movement.type === "STOCK_IN" ? "text-green-700" : "text-slate-600"}>
                  {movement.type === "STOCK_IN" ? "+" : "-"}
                  {movement.quantity}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  );
}
