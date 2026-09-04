import { useState } from "react";
import { Button } from "../../components/Button";
import { ErrorMessage } from "../../components/ErrorMessage";
import { Input } from "../../components/Input";
import { Modal } from "../../components/Modal";
import type { LocalNote } from "../../db/types";

interface NoteEditorModalProps {
  note: LocalNote | null;
  onClose: () => void;
  onSave: (input: { title: string | null; content: string }) => Promise<void>;
  onDelete?: () => Promise<void>;
}

export function NoteEditorModal({ note, onClose, onSave, onDelete }: NoteEditorModalProps) {
  const [title, setTitle] = useState(note?.title ?? "");
  const [content, setContent] = useState(note?.content ?? "");
  const [error, setError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleSave = async () => {
    if (!content.trim()) {
      setError("Note can't be empty.");
      return;
    }
    setError(null);
    setIsSaving(true);
    try {
      await onSave({ title: title.trim() ? title.trim() : null, content: content.trim() });
      onClose();
    } catch {
      setError("Couldn't save the note on this device. Please try again.");
      setIsSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!onDelete) {
      return;
    }
    setIsDeleting(true);
    try {
      await onDelete();
      onClose();
    } catch {
      setError("Couldn't delete the note on this device. Please try again.");
      setIsDeleting(false);
    }
  };

  return (
    <Modal title={note ? "Edit note" : "New note"} onClose={onClose}>
      <div className="flex flex-col gap-3">
        <Input
          label="Title (optional)"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Title"
        />
        <div className="flex flex-col gap-1">
          <label htmlFor="note-content" className="text-sm font-medium text-ink-700">
            Note
          </label>
          <textarea
            id="note-content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={8}
            placeholder="Write a private note - only you can see this."
            className="rounded-md border border-ink-300 bg-white px-3 py-2 text-sm text-ink-900 shadow-sm
              transition-colors duration-150 focus:outline focus:outline-2 focus:outline-offset-1
              focus:outline-gold-500"
          />
        </div>
        {error && <ErrorMessage message={error} />}
        <div className="flex items-center justify-between gap-2 pt-2">
          <div>
            {note && onDelete && (
              <Button variant="danger" onClick={handleDelete} disabled={isDeleting || isSaving}>
                {isDeleting ? "Deleting…" : "Delete"}
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="secondary" onClick={onClose} disabled={isSaving || isDeleting}>
              Cancel
            </Button>
            <Button onClick={handleSave} disabled={isSaving || isDeleting}>
              {isSaving ? "Saving…" : "Save"}
            </Button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
