import { useLiveQuery } from "dexie-react-hooks";
import { db } from "../../db/db";
import type { LocalSale, LocalSaleItem } from "../../db/types";

export interface LocalSaleWithItems extends LocalSale {
  items: LocalSaleItem[];
  queueEntryId: number | null;
  errorMessage: string | null;
}

/** Sales this device has queued for this employee, newest first, regardless of sync status. */
export function useLocalSales(employeeId: string | undefined) {
  return useLiveQuery(async () => {
    if (!employeeId) {
      return [];
    }
    const sales = await db.sales.where("employeeId").equals(employeeId).sortBy("createdAt");
    sales.reverse();
    return Promise.all(
      sales.map(async (sale) => {
        const [items, queueEntry] = await Promise.all([
          db.saleItems.where("saleClientTransactionId").equals(sale.clientTransactionId).toArray(),
          db.syncQueue.where("clientTransactionId").equals(sale.clientTransactionId).first(),
        ]);
        return {
          ...sale,
          items,
          queueEntryId: queueEntry?.id ?? null,
          errorMessage: queueEntry?.errorMessage ?? null,
        };
      }),
    );
  }, [employeeId]);
}
