import { db } from "../../db/db";
import { extractErrorMessage } from "../../api/errors";
import { syncApi } from "../../api/syncApi";
import { drainNoteQueue } from "../notes/noteSync";
import type { SyncQueueEntry, SyncStatus } from "../../db/types";
import type { SaleStatus } from "../../types/sale";

const MAX_RETRIES = 6;
const BASE_DELAY_MS = 2_000;
const MAX_DELAY_MS = 60_000;
const POLL_INTERVAL_MS = 5_000;

function backoffDelayMs(retryCount: number): number {
  return Math.min(BASE_DELAY_MS * 2 ** retryCount, MAX_DELAY_MS);
}

function isDue(entry: SyncQueueEntry): boolean {
  if (!entry.lastAttemptAt) {
    return true;
  }
  const dueAt = new Date(entry.lastAttemptAt).getTime() + backoffDelayMs(entry.retryCount);
  return Date.now() >= dueAt;
}

function serverStatusToSyncStatus(status: SaleStatus): SyncStatus {
  return status === "CONFLICT" ? "CONFLICT" : "SYNCED";
}

async function processEntry(entry: SyncQueueEntry): Promise<void> {
  await db.syncQueue.update(entry.id!, { status: "SYNCING" });

  try {
    const sale = await syncApi.syncSale(entry.payload);
    const resolvedStatus = serverStatusToSyncStatus(sale.status);
    await db.transaction("rw", db.syncQueue, db.sales, async () => {
      await db.syncQueue.update(entry.id!, {
        status: resolvedStatus,
        lastAttemptAt: new Date().toISOString(),
        errorMessage: null,
      });
      await db.sales.update(entry.clientTransactionId, { syncStatus: resolvedStatus });
    });
  } catch (error) {
    const retryCount = entry.retryCount + 1;
    const nextStatus: SyncStatus = retryCount >= MAX_RETRIES ? "FAILED" : "PENDING";
    const message = extractErrorMessage(error, "Sync failed. Will retry automatically.");
    await db.transaction("rw", db.syncQueue, db.sales, async () => {
      await db.syncQueue.update(entry.id!, {
        status: nextStatus,
        retryCount,
        lastAttemptAt: new Date().toISOString(),
        errorMessage: message,
      });
      await db.sales.update(entry.clientTransactionId, { syncStatus: nextStatus });
    });
  }
}

let draining = false;

/**
 * Drains the local sync queue one entry at a time, oldest first. Sequential (not parallel)
 * processing keeps ordering predictable and avoids piling up concurrent requests against the same
 * products. An entry that failed and is waiting out its backoff window is skipped until it's due -
 * this function is safe to call repeatedly (e.g. on a poll interval) without double-processing.
 */
export async function drainQueue(): Promise<void> {
  if (draining) {
    return;
  }
  draining = true;
  try {
    const pending = await db.syncQueue.where("status").equals("PENDING").sortBy("createdAt");
    for (const entry of pending) {
      if (!navigator.onLine) {
        break;
      }
      if (isDue(entry)) {
        await processEntry(entry);
      }
    }
  } finally {
    draining = false;
  }
}

export function triggerSync(): void {
  if (!navigator.onLine) {
    return;
  }
  void drainQueue();
  void drainNoteQueue();
}

/** Manually retries one FAILED entry: resets its backoff state and re-queues it as PENDING. */
export async function retryFailedEntry(queueId: number): Promise<void> {
  const entry = await db.syncQueue.get(queueId);
  if (!entry) {
    return;
  }
  await db.transaction("rw", db.syncQueue, db.sales, async () => {
    await db.syncQueue.update(queueId, { status: "PENDING", retryCount: 0, lastAttemptAt: null, errorMessage: null });
    await db.sales.update(entry.clientTransactionId, { syncStatus: "PENDING" });
  });
  await drainQueue();
}

/** Starts the background sync engine: syncs on reconnect and polls while online. Call once at app startup. */
export function startSyncEngine(): () => void {
  window.addEventListener("online", triggerSync);
  const interval = window.setInterval(triggerSync, POLL_INTERVAL_MS);
  triggerSync();

  return () => {
    window.removeEventListener("online", triggerSync);
    window.clearInterval(interval);
  };
}
