import type { Sale } from "../../types/sale";

export function SalesTable({ sales, showEmployee }: { sales: Sale[]; showEmployee: boolean }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr>
            {showEmployee && <th className="px-4 py-2 text-left font-medium text-slate-600">Employee</th>}
            <th className="px-4 py-2 text-left font-medium text-slate-600">Items</th>
            <th className="px-4 py-2 text-right font-medium text-slate-600">Total</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">When</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {sales.map((sale) => (
            <tr key={sale.id}>
              {showEmployee && <td className="px-4 py-2 text-slate-900">{sale.employeeName}</td>}
              <td className="px-4 py-2 text-slate-600">
                {sale.items.map((item) => `${item.productName} ×${item.quantity}`).join(", ")}
              </td>
              <td className="px-4 py-2 text-right font-medium text-slate-900">${sale.totalAmount.toFixed(2)}</td>
              <td className="px-4 py-2 text-slate-500">{new Date(sale.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
