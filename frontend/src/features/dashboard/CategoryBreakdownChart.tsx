import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Card } from "../../components/Card";
import { EmptyState } from "../../components/EmptyState";
import type { CategoryBreakdown } from "../../types/dashboard";

export function CategoryBreakdownChart({ categories }: { categories: CategoryBreakdown[] }) {
  return (
    <Card>
      <h2 className="mb-2 text-sm font-medium text-slate-600">Revenue and stock by category</h2>
      {categories.length === 0 ? (
        <EmptyState message="No categories yet." />
      ) : (
        <>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={categories}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="categoryName" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="revenue" name="Revenue ($)" fill="#059669" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
          <div className="mt-3 overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead>
                <tr>
                  <th className="px-2 py-1 text-left font-medium text-slate-600">Category</th>
                  <th className="px-2 py-1 text-right font-medium text-slate-600">Units sold</th>
                  <th className="px-2 py-1 text-right font-medium text-slate-600">Revenue</th>
                  <th className="px-2 py-1 text-right font-medium text-slate-600">Stock on hand</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {categories.map((category) => (
                  <tr key={category.categoryId}>
                    <td className="px-2 py-1 text-slate-900">{category.categoryName}</td>
                    <td className="px-2 py-1 text-right text-slate-600">{category.unitsSold}</td>
                    <td className="px-2 py-1 text-right text-slate-600">${category.revenue.toFixed(2)}</td>
                    <td className="px-2 py-1 text-right text-slate-600">{category.stockOnHand}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </Card>
  );
}
