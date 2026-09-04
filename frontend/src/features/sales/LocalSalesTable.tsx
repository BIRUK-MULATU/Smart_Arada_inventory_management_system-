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
    <div className="overflow-x-auto rounded-lg border border-ink-200 bg-white">
      <table className="min-w-full divide-y divide-ink-200 text-sm">
        <thead className="bg-ink-50">
          <tr>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Items</th>
            <th className="px-4 py-2 text-right font-medium text-ink-600">Total</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Payment</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">When</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600">Status</th>
            <th className="px-4 py-2 text-left font-medium text-ink-600"></th>
          </tr>
        </thead>
        <tbody className="divide-y divide-ink-100">
          {sales.map((sale) => (
            <tr key={sale.clientTransactionId}>
              <td className="px-4 py-2 text-ink-600">
                {sale.items.map((item) => `${item.productName} ×${item.quantity}`).join(", ")}
              </td>
              <td className="px-4 py-2 text-right font-medium text-ink-900">${sale.totalAmount.toFixed(2)}</td>
              <td className="px-4 py-2 text-ink-600">
                {sale.paymentMethod === "BANK" ? (
                  <>
                    Bank
                    {sale.bankAccount && <span className="block text-xs text-ink-400">{sale.bankAccount}</span>}
                  </>
                ) : (
                  "Cash"
                )}
              </td>
              <td className="px-4 py-2 text-ink-500">{new Date(sale.createdAt).toLocaleString()}</td>
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
                    className="min-h-11 rounded-md border border-ink-300 px-3 py-1 text-sm font-medium text-ink-700
                      hover:bg-ink-50 disabled:opacity-50"
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
