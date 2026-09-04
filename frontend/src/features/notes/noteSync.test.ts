import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { db } from "../../db/db";
import { notesApi } from "../../api/notesApi";
import { drainNoteQueue } from "./noteSync";

vi.mock("../../api/notesApi", () => ({
  notesApi: {
    list: vi.fn(),
    upsert: vi.fn(),
    remove: vi.fn(),
  },
}));

async function seedUpsertEntry(entityId: string, overrides: Partial<{ retryCount: number; lastAttemptAt: string | null }> = {}) {
  await db.notes.put({
    id: entityId,
    title: "Title",
    content: "Content",
    baseUpdatedAt: null,
    syncStatus: "PENDING",
    updatedAtLocal: new Date().toISOString(),
  });
  return db.noteSyncQueue.add({
    operationType: "UPSERT_NOTE",
    entityId,
    payload: { title: "Title", content: "Content", baseUpdatedAt: null },
    status: "PENDING",
    retryCount: overrides.retryCount ?? 0,
    lastAttemptAt: overrides.lastAttemptAt ?? null,
    createdAt: new Date().toISOString(),
    errorMessage: null,
  }) as Promise<number>;
}

beforeEach(() => {
  vi.stubGlobal("navigator", { ...navigator, onLine: true });
});

afterEach(async () => {
  vi.unstubAllGlobals();
  vi.mocked(notesApi.upsert).mockReset();
  vi.mocked(notesApi.remove).mockReset();
  vi.mocked(notesApi.list).mockReset();
  await db.notes.clear();
  await db.noteSyncQueue.clear();
});

describe("drainNoteQueue", () => {
  it("marks a successfully synced note as SYNCED and removes the queue entry", async () => {
    const queueId = await seedUpsertEntry("note-1");
    vi.mocked(notesApi.upsert).mockResolvedValue({
      conflicted: false,
      note: { id: "note-1", title: "Title", content: "Content", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    });

    await drainNoteQueue();

    const note = await db.notes.get("note-1");
    const queueEntry = await db.noteSyncQueue.get(queueId);
    expect(note?.syncStatus).toBe("SYNCED");
    expect(note?.baseUpdatedAt).toBe("2026-01-01T00:00:00Z");
    expect(queueEntry).toBeUndefined();
  });

  it("pulls the authoritative list instead of trusting a conflicted response locally", async () => {
    await seedUpsertEntry("note-1");
    vi.mocked(notesApi.upsert).mockResolvedValue({
      conflicted: true,
      note: { id: "note-2", title: "Title (conflicting edit)", content: "Content", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    });
    vi.mocked(notesApi.list).mockResolvedValue([
      { id: "note-1", title: "Title", content: "Original", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
      { id: "note-2", title: "Title (conflicting edit)", content: "Content", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    ]);

    await drainNoteQueue();

    expect(notesApi.list).toHaveBeenCalled();
    expect(await db.notes.get("note-2")).toBeDefined();
    const queueEntries = await db.noteSyncQueue.toArray();
    expect(queueEntries).toHaveLength(0);
  });

  it("processes a delete and removes the queue entry", async () => {
    await db.noteSyncQueue.add({
      operationType: "DELETE_NOTE",
      entityId: "note-1",
      payload: null,
      status: "PENDING",
      retryCount: 0,
      lastAttemptAt: null,
      createdAt: new Date().toISOString(),
      errorMessage: null,
    });
    vi.mocked(notesApi.remove).mockResolvedValue(undefined);

    await drainNoteQueue();

    expect(notesApi.remove).toHaveBeenCalledWith("note-1");
    expect(await db.noteSyncQueue.toArray()).toHaveLength(0);
  });

  it("keeps a failed sync as PENDING with an incremented retry count", async () => {
    const queueId = await seedUpsertEntry("note-1");
    vi.mocked(notesApi.upsert).mockRejectedValue(new Error("network error"));

    await drainNoteQueue();

    const queueEntry = await db.noteSyncQueue.get(queueId);
    const note = await db.notes.get("note-1");
    expect(queueEntry?.status).toBe("PENDING");
    expect(queueEntry?.retryCount).toBe(1);
    expect(note?.syncStatus).toBe("PENDING");
  });

  it("moves an entry to FAILED once it exceeds the retry cap", async () => {
    const queueId = await seedUpsertEntry("note-1", { retryCount: 5, lastAttemptAt: null });
    vi.mocked(notesApi.upsert).mockRejectedValue(new Error("network error"));

    await drainNoteQueue();

    const queueEntry = await db.noteSyncQueue.get(queueId);
    const note = await db.notes.get("note-1");
    expect(queueEntry?.status).toBe("FAILED");
    expect(note?.syncStatus).toBe("FAILED");
  });
});
