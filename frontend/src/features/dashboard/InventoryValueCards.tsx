import { Card } from "../../components/Card";
import type { DashboardSummary } from "../../types/dashboard";

/**
 * Point-in-time value of everything currently on the shelf - not tied to the selected period,
 * unlike SummaryCards' "Revenue this period". Shown at both selling price (with the profit
 * margin baked in) and cost price (what was actually paid for it), so the gap between the two
 * cards is the profit still sitting in inventory, unrealized until it sells.
 */
export function InventoryValueCards({ summary }: { summary: DashboardSummary }) {
  const potentialProfit = summary.inventoryValueAtBasePrice - summary.inventoryValueAtCostPrice;

  const cards = [
    { label: "Stock value at selling price", value: summary.inventoryValueAtBasePrice, accent: false },
    { label: "Stock value at cost", value: summary.inventoryValueAtCostPrice, accent: false },
    { label: "Potential profit on hand", value: potentialProfit, accent: true },
  ];

  return (
    <div>
      <h2 className="mb-2 text-lg font-semibold text-ink-900">Inventory value</h2>
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        {cards.map((card) => (
          <Card key={card.label}>
            <p className="text-xs text-ink-500">{card.label}</p>
            <p className={`mt-1 text-2xl font-semibold ${card.accent ? "text-gold-600" : "text-ink-900"}`}>
              ${card.value.toFixed(2)}
            </p>
          </Card>
        ))}
      </div>
    </div>
  );
}
