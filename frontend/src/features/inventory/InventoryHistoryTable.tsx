import { Badge } from "../../components/Badge";
import type { InventoryTransaction } from "../../types/inventory";

export function InventoryHistoryTable({ transactions }: { transactions: InventoryTransaction[] }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Product</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Type</th>
            <th className="px-4 py-2 text-right font-medium text-slate-600">Quantity</th>
            <th className="px-4 py-2 text-right font-medium text-slate-600">Stock after</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Performed by</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">When</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {transactions.map((transaction) => (
            <tr key={transaction.id}>
              <td className="px-4 py-2 font-medium text-slate-900">{transaction.productName}</td>
              <td className="px-4 py-2">
                <Badge tone={transaction.type === "STOCK_IN" ? "success" : "neutral"}>{transaction.type}</Badge>
              </td>
              <td className="px-4 py-2 text-right text-slate-600">{transaction.quantity}</td>
              <td className="px-4 py-2 text-right text-slate-600">{transaction.newQuantity}</td>
              <td className="px-4 py-2 text-slate-600">{transaction.performedByName}</td>
              <td className="px-4 py-2 text-slate-500">{new Date(transaction.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
