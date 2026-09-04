import { db } from "../../db/db";
import type { CreateSaleRequest, PaymentMethod } from "../../types/sale";
import type { CartItem } from "./useCart";

export interface RecordSaleInput {
  employeeId: string;
  items: CartItem[];
  paymentMethod: PaymentMethod;
  /** Only meaningful (and required by the caller) when paymentMethod is "BANK". */
  bankAccount?: string;
}

/**
 * Records a sale entirely locally - no network call. The client_transaction_id is generated
 * exactly once, here, and reused unchanged for the sale record, its line items, and the sync
 * queue payload, so a later retry (Phase 10) never produces a duplicate. All three writes happen
 * in a single Dexie transaction: either the sale, its items, and its queue entry are all written
 * together, or none of them are - the same "no partial writes" discipline as the backend's
 * @Transactional sale creation, applied to local storage.
 */
export async function recordSaleOffline(input: RecordSaleInput): Promise<string> {
  const clientTransactionId = crypto.randomUUID();
  const createdAt = new Date().toISOString();

  const totalAmount = input.items.reduce((sum, item) => sum + item.sellingPrice * item.quantity, 0);

  const payload: CreateSaleRequest = {
    clientTransactionId,
    items: input.items.map((item) => ({
      productId: item.productId,
      quantity: item.quantity,
      sellingPrice: item.sellingPrice,
    })),
    paymentMethod: input.paymentMethod,
    ...(input.paymentMethod === "BANK" ? { bankAccount: input.bankAccount } : {}),
  };

  await db.transaction("rw", db.sales, db.saleItems, db.syncQueue, async () => {
    await db.sales.add({
      clientTransactionId,
      employeeId: input.employeeId,
      totalAmount,
      paymentMethod: input.paymentMethod,
      bankAccount: input.paymentMethod === "BANK" ? (input.bankAccount ?? null) : null,
      syncStatus: "PENDING",
      createdAt,
    });

    await db.saleItems.bulkAdd(
      input.items.map((item) => ({
        id: crypto.randomUUID(),
        saleClientTransactionId: clientTransactionId,
        productId: item.productId,
        productName: item.productName,
        quantity: item.quantity,
        sellingPrice: item.sellingPrice,
        subtotal: item.sellingPrice * item.quantity,
      })),
    );

    await db.syncQueue.add({
      operationType: "CREATE_SALE",
      entityType: "SALE",
      entityId: clientTransactionId,
      clientTransactionId,
      payload,
      status: "PENDING",
      retryCount: 0,
      lastAttemptAt: null,
      createdAt,
      errorMessage: null,
    });
  });

  return clientTransactionId;
}
