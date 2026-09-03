import { useLiveQuery } from "dexie-react-hooks";
import { db } from "../../db/db";
import type { LocalSale, LocalSaleItem } from "../../db/types";

export interface LocalSaleWithItems extends LocalSale {
  items: LocalSaleItem[];
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
      sales.map(async (sale) => ({
        ...sale,
        items: await db.saleItems.where("saleClientTransactionId").equals(sale.clientTransactionId).toArray(),
      })),
    );
  }, [employeeId]);
}
