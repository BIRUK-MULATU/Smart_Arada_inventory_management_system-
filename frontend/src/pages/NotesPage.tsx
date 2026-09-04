import { useState } from "react";
import { Button } from "../components/Button";
import { Card } from "../components/Card";
import { EmptyState } from "../components/EmptyState";
import { SyncStatusBadge } from "../components/SyncStatusBadge";
import { deleteNoteOffline, saveNoteOffline } from "../features/notes/offlineNotes";
import { NoteEditorModal } from "../features/notes/NoteEditorModal";
import { useNotes } from "../features/notes/useNotes";
import { triggerSync } from "../features/sync/syncEngine";
import type { LocalNote } from "../db/types";

/**
 * Shared between the admin sidebar and the employee nav - notes are private to whichever user is
 * logged in, never role-scoped, so the same page and the same local Dexie cache work for both.
 */
export function NotesPage() {
  const { notes } = useNotes();
  const [editingNote, setEditingNote] = useState<LocalNote | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const editorOpen = isCreating || editingNote !== null;

  const closeEditor = () => {
    setIsCreating(false);
    setEditingNote(null);
  };

  const handleSave = async (input: { title: string | null; content: string }) => {
    await saveNoteOffline({ id: editingNote?.id, ...input });
    triggerSync();
  };

  const handleDelete = async () => {
    if (!editingNote) {
      return;
    }
    await deleteNoteOffline(editingNote.id);
    triggerSync();
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-ink-900">Notes</h1>
          <p className="text-sm text-ink-500">Private to you - nobody else, including admins, can see these.</p>
        </div>
        <Button onClick={() => setIsCreating(true)}>New note</Button>
      </div>

      {notes && notes.length === 0 && <EmptyState message="No notes yet. Start with “New note”." />}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {notes?.map((note) => (
          <Card
            key={note.id}
            className="flex cursor-pointer flex-col gap-2 hover:border-ink-300"
            onClick={() => setEditingNote(note)}
          >
            <div className="flex items-start justify-between gap-2">
              <p className="font-medium text-ink-900">{note.title || "Untitled"}</p>
              {note.syncStatus !== "SYNCED" && <SyncStatusBadge status={note.syncStatus} />}
            </div>
            <p className="line-clamp-4 whitespace-pre-wrap text-sm text-ink-600">{note.content}</p>
            <p className="mt-auto text-xs text-ink-400">{new Date(note.updatedAtLocal).toLocaleString()}</p>
          </Card>
        ))}
      </div>

      {editorOpen && (
        <NoteEditorModal
          note={editingNote}
          onClose={closeEditor}
          onSave={handleSave}
          onDelete={editingNote ? handleDelete : undefined}
        />
      )}
    </div>
  );
}
