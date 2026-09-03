import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { db } from "../../db/db";
import { syncApi } from "../../api/syncApi";
import { drainQueue, retryFailedEntry } from "./syncEngine";
import type { Sale } from "../../types/sale";

vi.mock("../../api/syncApi", () => ({
  syncApi: {
    syncSale: vi.fn(),
  },
}));

function fakeSale(overrides: Partial<Sale> = {}): Sale {
  return {
    id: "sale-1",
    employeeId: "employee-1",
    employeeName: "Employee One",
    clientTransactionId: "client-txn-1",
    totalAmount: 19.98,
    status: "COMPLETED",
    resolvedByUserId: null,
    resolvedByName: null,
    resolvedAt: null,
    resolutionNote: null,
    items: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}

async function seedQueueEntry(clientTransactionId: string): Promise<number> {
  const createdAt = new Date().toISOString();
  await db.sales.add({
    clientTransactionId,
    employeeId: "employee-1",
    totalAmount: 19.98,
    syncStatus: "PENDING",
    createdAt,
  });
  return db.syncQueue.add({
    operationType: "CREATE_SALE",
    entityType: "SALE",
    entityId: clientTransactionId,
    clientTransactionId,
    payload: { clientTransactionId, items: [{ productId: "product-1", quantity: 2, sellingPrice: 9.99 }] },
    status: "PENDING",
    retryCount: 0,
    lastAttemptAt: null,
    createdAt,
    errorMessage: null,
  }) as Promise<number>;
}

beforeEach(() => {
  vi.stubGlobal("navigator", { ...navigator, onLine: true });
});

afterEach(async () => {
  vi.unstubAllGlobals();
  vi.mocked(syncApi.syncSale).mockReset();
  await db.sales.clear();
  await db.saleItems.clear();
  await db.syncQueue.clear();
});

describe("drainQueue", () => {
  it("marks a successfully synced sale as SYNCED", async () => {
    const queueId = await seedQueueEntry("client-txn-1");
    vi.mocked(syncApi.syncSale).mockResolvedValue(fakeSale({ status: "COMPLETED" }));

    await drainQueue();

    const sale = await db.sales.get("client-txn-1");
    const queueEntry = await db.syncQueue.get(queueId);
    expect(sale?.syncStatus).toBe("SYNCED");
    expect(queueEntry?.status).toBe("SYNCED");
  });

  it("marks a sale flagged CONFLICT by the server as CONFLICT locally", async () => {
    await seedQueueEntry("client-txn-2");
    vi.mocked(syncApi.syncSale).mockResolvedValue(fakeSale({ status: "CONFLICT" }));

    await drainQueue();

    const sale = await db.sales.get("client-txn-2");
    expect(sale?.syncStatus).toBe("CONFLICT");
  });

  it("keeps a failed sync as PENDING with an incremented retry count below the retry cap", async () => {
    const queueId = await seedQueueEntry("client-txn-3");
    vi.mocked(syncApi.syncSale).mockRejectedValue(new Error("network error"));

    await drainQueue();

    const queueEntry = await db.syncQueue.get(queueId);
    expect(queueEntry?.status).toBe("PENDING");
    expect(queueEntry?.retryCount).toBe(1);
    expect(queueEntry?.errorMessage).toBeTruthy();
  });

  it("does not retry an entry still inside its backoff window", async () => {
    const queueId = await seedQueueEntry("client-txn-4");
    vi.mocked(syncApi.syncSale).mockRejectedValue(new Error("network error"));
    await drainQueue();
    expect(syncApi.syncSale).toHaveBeenCalledTimes(1);

    // The failed entry is due again in ~2s (base backoff) - draining immediately afterward must
    // not re-attempt it yet.
    await drainQueue();
    expect(syncApi.syncSale).toHaveBeenCalledTimes(1);
    const queueEntry = await db.syncQueue.get(queueId);
    expect(queueEntry?.retryCount).toBe(1);
  });

  it("moves an entry to FAILED once it exceeds the retry cap", async () => {
    const queueId = await seedQueueEntry("client-txn-5");
    vi.mocked(syncApi.syncSale).mockRejectedValue(new Error("network error"));
    await db.syncQueue.update(queueId, { retryCount: 5, lastAttemptAt: null });

    await drainQueue();

    const queueEntry = await db.syncQueue.get(queueId);
    const sale = await db.sales.get("client-txn-5");
    expect(queueEntry?.status).toBe("FAILED");
    expect(sale?.syncStatus).toBe("FAILED");
  });
});

describe("retryFailedEntry", () => {
  it("resets a FAILED entry to PENDING and re-syncs it successfully", async () => {
    const queueId = await seedQueueEntry("client-txn-6");
    await db.syncQueue.update(queueId, { status: "FAILED", retryCount: 6, errorMessage: "gave up" });
    await db.sales.update("client-txn-6", { syncStatus: "FAILED" });
    vi.mocked(syncApi.syncSale).mockResolvedValue(fakeSale({ status: "COMPLETED" }));

    await retryFailedEntry(queueId);

    const queueEntry = await db.syncQueue.get(queueId);
    const sale = await db.sales.get("client-txn-6");
    expect(queueEntry?.status).toBe("SYNCED");
    expect(sale?.syncStatus).toBe("SYNCED");
  });
});
