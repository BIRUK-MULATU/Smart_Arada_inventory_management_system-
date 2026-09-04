import { afterEach, describe, expect, it } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { db } from "../../db/db";
import { usePendingSyncCounts } from "./usePendingSyncCounts";
import type { SyncStatus } from "../../db/types";

async function seedQueueEntry(clientTransactionId: string, status: SyncStatus): Promise<void> {
  const createdAt = new Date().toISOString();
  await db.syncQueue.add({
    operationType: "CREATE_SALE",
    entityType: "SALE",
    entityId: clientTransactionId,
    clientTransactionId,
    payload: {
      clientTransactionId,
      items: [{ productId: "product-1", quantity: 1, sellingPrice: 9.99 }],
      paymentMethod: "CASH",
    },
    status,
    retryCount: 0,
    lastAttemptAt: null,
    createdAt,
    errorMessage: null,
  });
}

afterEach(async () => {
  await db.syncQueue.clear();
});

describe("usePendingSyncCounts", () => {
  it("counts PENDING and FAILED queue entries separately, ignoring other statuses", async () => {
    await seedQueueEntry("client-txn-1", "PENDING");
    await seedQueueEntry("client-txn-2", "PENDING");
    await seedQueueEntry("client-txn-3", "FAILED");
    await seedQueueEntry("client-txn-4", "SYNCED");
    await seedQueueEntry("client-txn-5", "CONFLICT");

    const { result } = renderHook(() => usePendingSyncCounts());

    await waitFor(() => {
      expect(result.current).toEqual({ pending: 2, failed: 1 });
    });
  });

  it("returns zero counts when the queue is empty", async () => {
    const { result } = renderHook(() => usePendingSyncCounts());

    await waitFor(() => {
      expect(result.current).toEqual({ pending: 0, failed: 0 });
    });
  });
});
