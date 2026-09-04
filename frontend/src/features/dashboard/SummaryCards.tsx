import { Card } from "../../components/Card";
import type { DashboardSummary } from "../../types/dashboard";

export function SummaryCards({ summary }: { summary: DashboardSummary }) {
  const cards = [
    { label: "Total products", value: summary.totalProducts },
    { label: "Total stock", value: summary.totalStock },
    { label: "Active employees", value: summary.totalEmployees },
    { label: "Low stock items", value: summary.lowStockCount },
    { label: "Sales this period", value: summary.periodSalesCount },
    { label: "Revenue this period", value: `$${summary.periodRevenue.toFixed(2)}` },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {cards.map((card) => (
        <Card key={card.label}>
          <p className="text-xs text-ink-500">{card.label}</p>
          <p className="mt-1 text-2xl font-semibold text-ink-900">{card.value}</p>
        </Card>
      ))}
    </div>
  );
}
