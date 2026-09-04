import { db } from "../../db/db";
import { extractErrorMessage } from "../../api/errors";
import { notesApi } from "../../api/notesApi";
import { refreshNotesSnapshot } from "./offlineNotes";
import type { NoteSyncQueueEntry, SyncStatus } from "../../db/types";

const MAX_RETRIES = 6;
const BASE_DELAY_MS = 2_000;
const MAX_DELAY_MS = 60_000;

function backoffDelayMs(retryCount: number): number {
  return Math.min(BASE_DELAY_MS * 2 ** retryCount, MAX_DELAY_MS);
}

function isDue(entry: NoteSyncQueueEntry): boolean {
  if (!entry.lastAttemptAt) {
    return true;
  }
  const dueAt = new Date(entry.lastAttemptAt).getTime() + backoffDelayMs(entry.retryCount);
  return Date.now() >= dueAt;
}

async function processEntry(entry: NoteSyncQueueEntry): Promise<void> {
  await db.noteSyncQueue.update(entry.id!, { status: "SYNCING" });

  try {
    if (entry.operationType === "DELETE_NOTE") {
      await notesApi.remove(entry.entityId);
      await db.noteSyncQueue.delete(entry.id!);
      return;
    }

    const result = await notesApi.upsert(entry.entityId, entry.payload!);
    await db.noteSyncQueue.delete(entry.id!);

    if (result.conflicted) {
      // The edit was saved as a separate note rather than overwriting a newer version - pull the
      // authoritative list so the reverted original and the new copy both show up correctly,
      // instead of trying to reconcile the two purely locally.
      await refreshNotesSnapshot();
      return;
    }

    await db.notes.update(entry.entityId, {
      baseUpdatedAt: result.note.updatedAt,
      syncStatus: "SYNCED",
    });
  } catch (error) {
    const retryCount = entry.retryCount + 1;
    const nextStatus: SyncStatus = retryCount >= MAX_RETRIES ? "FAILED" : "PENDING";
    const message = extractErrorMessage(error, "Sync failed. Will retry automatically.");
    await db.transaction("rw", db.noteSyncQueue, db.notes, async () => {
      await db.noteSyncQueue.update(entry.id!, {
        status: nextStatus,
        retryCount,
        lastAttemptAt: new Date().toISOString(),
        errorMessage: message,
      });
      if (entry.operationType === "UPSERT_NOTE") {
        await db.notes.update(entry.entityId, { syncStatus: nextStatus });
      }
    });
  }
}

let draining = false;

/** Mirrors sales' drainQueue: sequential, oldest-first, safe to call repeatedly. */
export async function drainNoteQueue(): Promise<void> {
  if (draining) {
    return;
  }
  draining = true;
  try {
    const pending = await db.noteSyncQueue.where("status").equals("PENDING").sortBy("createdAt");
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
