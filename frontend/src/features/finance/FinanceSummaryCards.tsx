import { Card } from "../../components/Card";
import type { FinanceSummary } from "../../types/finance";

export function FinanceSummaryCards({ summary }: { summary: FinanceSummary }) {
  const cards = [
    { label: "Revenue", value: summary.revenue },
    { label: "Cost of goods sold", value: summary.costOfGoodsSold },
    { label: "Gross profit", value: summary.grossProfit },
    { label: "Expenses", value: summary.totalExpenses },
    { label: "Net profit", value: summary.netProfit, emphasize: true },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {cards.map((card) => (
        <Card key={card.label} className={card.emphasize ? "border-slate-900" : undefined}>
          <p className="text-xs text-slate-500">{card.label}</p>
          <p
            className={`mt-1 text-2xl font-semibold ${
              card.value < 0 ? "text-red-600" : "text-slate-900"
            }`}
          >
            ${card.value.toFixed(2)}
          </p>
        </Card>
      ))}
    </div>
  );
}
