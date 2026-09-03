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
import type { DailySalesPoint } from "../../types/dashboard";

export function SalesRevenueChart({ points }: { points: DailySalesPoint[] }) {
  if (points.length === 0) {
    return (
      <Card>
        <EmptyState message="No sales in this period yet." />
      </Card>
    );
  }

  return (
    <Card>
      <h2 className="mb-2 text-sm font-medium text-slate-600">Sales and revenue over time</h2>
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={points}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
          <XAxis dataKey="day" tick={{ fontSize: 12 }} />
          <YAxis yAxisId="left" tick={{ fontSize: 12 }} allowDecimals={false} />
          <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} />
          <Tooltip />
          <Legend />
          <Line yAxisId="left" type="monotone" dataKey="salesCount" name="Sales" stroke="#0f172a" strokeWidth={2} />
          <Line yAxisId="right" type="monotone" dataKey="revenue" name="Revenue ($)" stroke="#059669" strokeWidth={2} />
        </LineChart>
      </ResponsiveContainer>
    </Card>
  );
}
