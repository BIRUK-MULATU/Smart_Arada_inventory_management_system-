import { notesApi } from "../../api/notesApi";
import { db } from "../../db/db";
import type { UpsertNoteRequest } from "../../types/note";

export interface SaveNoteInput {
  id?: string;
  title: string | null;
  content: string;
}

/**
 * Saves a note entirely locally - no network call. If a write for this same note is still sitting
 * PENDING in the queue (the user saved again before the previous save synced), that entry's
 * payload is replaced in place rather than appending a second one: sending both separately would
 * have the second carry the same (still-null-or-stale) baseUpdatedAt as the first, which the
 * server would misread as an edit based on out-of-date knowledge of a note it just created itself,
 * turning a normal fast edit into a spurious conflict copy. Collapsing to the latest edit avoids
 * that entirely and only ever queues one write per note.
 */
export async function saveNoteOffline(input: SaveNoteInput): Promise<string> {
  const id = input.id ?? crypto.randomUUID();
  const now = new Date().toISOString();

  await db.transaction("rw", db.notes, db.noteSyncQueue, async () => {
    const existingLocal = await db.notes.get(id);
    const baseUpdatedAt = existingLocal?.baseUpdatedAt ?? null;

    await db.notes.put({
      id,
      title: input.title,
      content: input.content,
      baseUpdatedAt,
      syncStatus: "PENDING",
      updatedAtLocal: now,
    });

    const payload: UpsertNoteRequest = { title: input.title, content: input.content, baseUpdatedAt };

    const replaceable = await db.noteSyncQueue
      .where("entityId")
      .equals(id)
      .and((entry) => entry.operationType === "UPSERT_NOTE" && entry.status === "PENDING")
      .first();

    if (replaceable) {
      await db.noteSyncQueue.update(replaceable.id!, { payload, createdAt: now });
    } else {
      await db.noteSyncQueue.add({
        operationType: "UPSERT_NOTE",
        entityId: id,
        payload,
        status: "PENDING",
        retryCount: 0,
        lastAttemptAt: null,
        createdAt: now,
        errorMessage: null,
      });
    }
  });

  return id;
}

/**
 * Deletes a note locally and queues the server delete. A not-yet-synced write for the same note
 * (it was created and edited entirely offline) is dropped from the queue instead of left to run -
 * the server has never heard of this note, so there's nothing there to delete either.
 */
export async function deleteNoteOffline(id: string): Promise<void> {
  await db.transaction("rw", db.notes, db.noteSyncQueue, async () => {
    const existingLocal = await db.notes.get(id);
    await db.notes.delete(id);
    await db.noteSyncQueue
      .where("entityId")
      .equals(id)
      .and((entry) => entry.operationType === "UPSERT_NOTE" && entry.status === "PENDING")
      .delete();

    if (existingLocal?.baseUpdatedAt) {
      await db.noteSyncQueue.add({
        operationType: "DELETE_NOTE",
        entityId: id,
        payload: null,
        status: "PENDING",
        retryCount: 0,
        lastAttemptAt: null,
        createdAt: new Date().toISOString(),
        errorMessage: null,
      });
    }
  });
}

/**
 * Pulls the authoritative note list from the server and merges it into the local cache. Only
 * notes with no unsynced local work (new, or already SYNCED) are overwritten or removed - a note
 * with a PENDING/SYNCING/FAILED edit is left alone so a refresh can never discard something the
 * user hasn't successfully saved yet.
 */
export async function refreshNotesSnapshot(): Promise<void> {
  let serverNotes;
  try {
    serverNotes = await notesApi.list();
  } catch {
    return;
  }

  await db.transaction("rw", db.notes, async () => {
    const localNotes = await db.notes.toArray();
    const localById = new Map(localNotes.map((note) => [note.id, note]));
    const serverIds = new Set(serverNotes.map((note) => note.id));

    for (const serverNote of serverNotes) {
      const local = localById.get(serverNote.id);
      if (!local || local.syncStatus === "SYNCED") {
        await db.notes.put({
          id: serverNote.id,
          title: serverNote.title,
          content: serverNote.content,
          baseUpdatedAt: serverNote.updatedAt,
          syncStatus: "SYNCED",
          updatedAtLocal: serverNote.updatedAt,
        });
      }
    }

    for (const local of localNotes) {
      if (local.syncStatus === "SYNCED" && !serverIds.has(local.id)) {
        await db.notes.delete(local.id);
      }
    }
  });
}
