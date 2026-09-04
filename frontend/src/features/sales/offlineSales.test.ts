import { afterEach, describe, expect, it, vi } from "vitest";
import { AppDatabase, db } from "../../db/db";
import { recordSaleOffline } from "./offlineSales";
import type { CartItem } from "./useCart";

const sampleItems: CartItem[] = [
  { productId: "product-1", productName: "Widget", sellingPrice: 9.99, quantity: 2, availableStock: 10 },
];

afterEach(async () => {
  await db.sales.clear();
  await db.saleItems.clear();
  await db.syncQueue.clear();
});

describe("recordSaleOffline", () => {
  it("survives a page reload", async () => {
    const clientTransactionId = await recordSaleOffline({ employeeId: "employee-1", items: sampleItems, paymentMethod: "CASH" });

    // A page reload constructs a brand new Dexie instance; connecting one to the same
    // underlying database name and reading back the data simulates that without a real reload.
    const reopened = new AppDatabase();
    const sale = await reopened.sales.get(clientTransactionId);
    const items = await reopened.saleItems.where("saleClientTransactionId").equals(clientTransactionId).toArray();

    expect(sale).toBeDefined();
    expect(sale?.clientTransactionId).toBe(clientTransactionId);
    expect(items).toHaveLength(1);
    expect(items[0].productId).toBe("product-1");
  });

  it("completes the sales workflow with the network unavailable", async () => {
    const fetchSpy = vi.spyOn(globalThis, "fetch");
    const originalOnLine = navigator.onLine;
    Object.defineProperty(navigator, "onLine", { value: false, configurable: true });

    try {
      const clientTransactionId = await recordSaleOffline({ employeeId: "employee-1", items: sampleItems, paymentMethod: "CASH" });

      const sale = await db.sales.get(clientTransactionId);
      const queueEntry = await db.syncQueue.where("clientTransactionId").equals(clientTransactionId).first();

      expect(sale?.syncStatus).toBe("PENDING");
      expect(queueEntry).toBeDefined();
      expect(queueEntry?.status).toBe("PENDING");
      expect(fetchSpy).not.toHaveBeenCalled();
    } finally {
      Object.defineProperty(navigator, "onLine", { value: originalOnLine, configurable: true });
      fetchSpy.mockRestore();
    }
  });

  it("keeps its original client_transaction_id across reads", async () => {
    const clientTransactionId = await recordSaleOffline({ employeeId: "employee-1", items: sampleItems, paymentMethod: "CASH" });

    const firstRead = await db.sales.get(clientTransactionId);
    const secondRead = await db.sales.get(clientTransactionId);

    expect(firstRead?.clientTransactionId).toBe(clientTransactionId);
    expect(secondRead?.clientTransactionId).toBe(clientTransactionId);
    expect(firstRead?.clientTransactionId).toBe(secondRead?.clientTransactionId);
  });

  it("stores a cash sale with no bank account", async () => {
    const clientTransactionId = await recordSaleOffline({
      employeeId: "employee-1",
      items: sampleItems,
      paymentMethod: "CASH",
    });

    const sale = await db.sales.get(clientTransactionId);
    expect(sale?.paymentMethod).toBe("CASH");
    expect(sale?.bankAccount).toBeNull();

    const queueEntry = await db.syncQueue.where("clientTransactionId").equals(clientTransactionId).first();
    expect(queueEntry?.payload.paymentMethod).toBe("CASH");
    expect(queueEntry?.payload.bankAccount).toBeUndefined();
  });

  it("stores a bank sale with the account it was received into", async () => {
    const clientTransactionId = await recordSaleOffline({
      employeeId: "employee-1",
      items: sampleItems,
      paymentMethod: "BANK",
      bankAccount: "CBE - 1000234567",
    });

    const sale = await db.sales.get(clientTransactionId);
    expect(sale?.paymentMethod).toBe("BANK");
    expect(sale?.bankAccount).toBe("CBE - 1000234567");

    const queueEntry = await db.syncQueue.where("clientTransactionId").equals(clientTransactionId).first();
    expect(queueEntry?.payload.paymentMethod).toBe("BANK");
    expect(queueEntry?.payload.bankAccount).toBe("CBE - 1000234567");
  });
});
