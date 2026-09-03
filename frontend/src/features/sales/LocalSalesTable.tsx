import { useState } from "react";
import { SyncStatusBadge } from "../../components/SyncStatusBadge";
import { retryFailedEntry } from "../sync/syncEngine";
import type { LocalSaleWithItems } from "./useLocalSales";

export function LocalSalesTable({ sales }: { sales: LocalSaleWithItems[] }) {
  const [retryingId, setRetryingId] = useState<number | null>(null);

  const handleRetry = async (queueEntryId: number) => {
    setRetryingId(queueEntryId);
    try {
      await retryFailedEntry(queueEntryId);
    } finally {
      setRetryingId(null);
    }
  };

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Items</th>
            <th className="px-4 py-2 text-right font-medium text-slate-600">Total</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">When</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Status</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600"></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {sales.map((sale) => (
            <tr key={sale.clientTransactionId}>
              <td className="px-4 py-2 text-slate-600">
                {sale.items.map((item) => `${item.productName} ×${item.quantity}`).join(", ")}
              </td>
              <td className="px-4 py-2 text-right font-medium text-slate-900">${sale.totalAmount.toFixed(2)}</td>
              <td className="px-4 py-2 text-slate-500">{new Date(sale.createdAt).toLocaleString()}</td>
              <td className="px-4 py-2">
                <SyncStatusBadge status={sale.syncStatus} />
                {sale.syncStatus === "FAILED" && sale.errorMessage && (
                  <p className="mt-1 text-xs text-red-700">{sale.errorMessage}</p>
                )}
              </td>
              <td className="px-4 py-2 text-right">
                {sale.syncStatus === "FAILED" && sale.queueEntryId !== null && (
                  <button
                    type="button"
                    onClick={() => handleRetry(sale.queueEntryId!)}
                    disabled={retryingId === sale.queueEntryId}
                    className="min-h-11 rounded-md border border-slate-300 px-3 py-1 text-sm font-medium text-slate-700
                      hover:bg-slate-50 disabled:opacity-50"
                  >
                    {retryingId === sale.queueEntryId ? "Retrying…" : "Retry"}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
