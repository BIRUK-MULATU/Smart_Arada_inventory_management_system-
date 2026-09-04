export interface Note {
  id: string;
  title: string | null;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpsertNoteRequest {
  title: string | null;
  content: string;
  baseUpdatedAt: string | null;
}

export interface NoteUpsertResponse {
  note: Note;
  conflicted: boolean;
}
