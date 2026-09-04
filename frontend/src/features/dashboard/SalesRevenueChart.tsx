import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { Card } from "../../components/Card";
import { EmptyState } from "../../components/EmptyState";
import type { DailySalesPoint, SalesGranularity } from "../../types/dashboard";

const GRANULARITIES: { value: SalesGranularity; label: string }[] = [
  { value: "DAILY", label: "Daily" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "YEARLY", label: "Yearly" },
];

interface SalesRevenueChartProps {
  points: DailySalesPoint[];
  granularity: SalesGranularity;
  onGranularityChange: (granularity: SalesGranularity) => void;
}

export function SalesRevenueChart({ points, granularity, onGranularityChange }: SalesRevenueChartProps) {
  return (
    <Card>
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-medium text-ink-600">Sales and revenue over time</h2>
        <div className="flex rounded-md border border-ink-300 text-xs">
          {GRANULARITIES.map((option, index) => (
            <button
              key={option.value}
              type="button"
              onClick={() => onGranularityChange(option.value)}
              className={`min-h-9 px-3 py-1 font-medium transition-colors duration-150 ${
                index > 0 ? "border-l border-ink-300" : ""
              } ${
                granularity === option.value
                  ? "bg-ink-950 text-gold-400"
                  : "bg-white text-ink-700 hover:bg-ink-50"
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>
      {points.length === 0 ? (
        <EmptyState message="No sales in this period yet." />
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={points}>
            <CartesianGrid strokeDasharray="3 3" stroke="#d8d3ca" />
            <XAxis dataKey="day" tick={{ fontSize: 12 }} />
            <YAxis yAxisId="left" tick={{ fontSize: 12 }} allowDecimals={false} />
            <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} />
            <Tooltip />
            <Legend />
            <Line yAxisId="left" type="monotone" dataKey="salesCount" name="Sales" stroke="#17140f" strokeWidth={2} />
            <Line yAxisId="right" type="monotone" dataKey="revenue" name="Revenue ($)" stroke="#059669" strokeWidth={2} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </Card>
  );
}
