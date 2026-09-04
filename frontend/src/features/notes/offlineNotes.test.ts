import { afterEach, describe, expect, it, vi } from "vitest";
import { db } from "../../db/db";
import { notesApi } from "../../api/notesApi";
import { deleteNoteOffline, refreshNotesSnapshot, saveNoteOffline } from "./offlineNotes";

vi.mock("../../api/notesApi", () => ({
  notesApi: {
    list: vi.fn(),
    upsert: vi.fn(),
    remove: vi.fn(),
  },
}));

afterEach(async () => {
  vi.mocked(notesApi.list).mockReset();
  await db.notes.clear();
  await db.noteSyncQueue.clear();
});

describe("saveNoteOffline", () => {
  it("creates a note locally and queues an upsert", async () => {
    const id = await saveNoteOffline({ title: "Shopping", content: "Milk, eggs" });

    const note = await db.notes.get(id);
    const queueEntry = await db.noteSyncQueue.where("entityId").equals(id).first();

    expect(note?.content).toBe("Milk, eggs");
    expect(note?.syncStatus).toBe("PENDING");
    expect(note?.baseUpdatedAt).toBeNull();
    expect(queueEntry?.operationType).toBe("UPSERT_NOTE");
    expect(queueEntry?.payload).toEqual({ title: "Shopping", content: "Milk, eggs", baseUpdatedAt: null });
  });

  it("collapses a second save before the first syncs into a single queue entry", async () => {
    const id = await saveNoteOffline({ title: "Draft", content: "v1" });
    await saveNoteOffline({ id, title: "Draft", content: "v2" });

    const entries = await db.noteSyncQueue.where("entityId").equals(id).toArray();
    const note = await db.notes.get(id);

    expect(entries).toHaveLength(1);
    expect(entries[0].payload?.content).toBe("v2");
    expect(note?.content).toBe("v2");
  });

  it("keeps the queued baseUpdatedAt in sync with the note's own once it has synced", async () => {
    const id = await saveNoteOffline({ title: "Title", content: "v1" });
    await db.notes.update(id, { baseUpdatedAt: "2026-01-01T00:00:00Z", syncStatus: "SYNCED" });
    await db.noteSyncQueue.where("entityId").equals(id).delete();

    await saveNoteOffline({ id, title: "Title", content: "v2" });

    const queueEntry = await db.noteSyncQueue.where("entityId").equals(id).first();
    expect(queueEntry?.payload?.baseUpdatedAt).toBe("2026-01-01T00:00:00Z");
  });
});

describe("deleteNoteOffline", () => {
  it("removes a never-synced note locally without queuing a server delete", async () => {
    const id = await saveNoteOffline({ title: "Title", content: "content" });

    await deleteNoteOffline(id);

    const note = await db.notes.get(id);
    const entries = await db.noteSyncQueue.where("entityId").equals(id).toArray();
    expect(note).toBeUndefined();
    expect(entries).toHaveLength(0);
  });

  it("queues a server delete for a note that has already synced", async () => {
    const id = await saveNoteOffline({ title: "Title", content: "content" });
    await db.notes.update(id, { baseUpdatedAt: "2026-01-01T00:00:00Z", syncStatus: "SYNCED" });
    await db.noteSyncQueue.where("entityId").equals(id).delete();

    await deleteNoteOffline(id);

    const entries = await db.noteSyncQueue.where("entityId").equals(id).toArray();
    expect(entries).toHaveLength(1);
    expect(entries[0].operationType).toBe("DELETE_NOTE");
  });

  it("drops a pending unsynced edit when the note is deleted before it ever syncs", async () => {
    const id = await saveNoteOffline({ title: "Title", content: "content" });

    await deleteNoteOffline(id);

    const entries = await db.noteSyncQueue.where("entityId").equals(id).toArray();
    expect(entries).toHaveLength(0);
  });
});

describe("refreshNotesSnapshot", () => {
  it("pulls a new server note into the local cache", async () => {
    vi.mocked(notesApi.list).mockResolvedValue([
      { id: "note-1", title: "Server note", content: "From another device", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    ]);

    await refreshNotesSnapshot();

    const note = await db.notes.get("note-1");
    expect(note?.content).toBe("From another device");
    expect(note?.syncStatus).toBe("SYNCED");
  });

  it("never overwrites a note with an unsynced local edit", async () => {
    const id = await saveNoteOffline({ title: "Mine", content: "My unsynced edit" });
    vi.mocked(notesApi.list).mockResolvedValue([
      { id, title: "Mine", content: "Stale server content", createdAt: "2026-01-01T00:00:00Z", updatedAt: "2026-01-01T00:00:00Z" },
    ]);

    await refreshNotesSnapshot();

    const note = await db.notes.get(id);
    expect(note?.content).toBe("My unsynced edit");
    expect(note?.syncStatus).toBe("PENDING");
  });

  it("removes a fully-synced note that no longer exists on the server", async () => {
    await db.notes.put({
      id: "note-1",
      title: "Gone",
      content: "Deleted elsewhere",
      baseUpdatedAt: "2026-01-01T00:00:00Z",
      syncStatus: "SYNCED",
      updatedAtLocal: "2026-01-01T00:00:00Z",
    });
    vi.mocked(notesApi.list).mockResolvedValue([]);

    await refreshNotesSnapshot();

    expect(await db.notes.get("note-1")).toBeUndefined();
  });

  it("leaves a locally-cached copy alone on a network failure", async () => {
    await db.notes.put({
      id: "note-1",
      title: "Cached",
      content: "Last known good",
      baseUpdatedAt: "2026-01-01T00:00:00Z",
      syncStatus: "SYNCED",
      updatedAtLocal: "2026-01-01T00:00:00Z",
    });
    vi.mocked(notesApi.list).mockRejectedValue(new Error("offline"));

    await refreshNotesSnapshot();

    expect((await db.notes.get("note-1"))?.content).toBe("Last known good");
  });
});
