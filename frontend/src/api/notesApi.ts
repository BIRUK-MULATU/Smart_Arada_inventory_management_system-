import { apiClient } from "./client";
import type { Note, NoteUpsertResponse, UpsertNoteRequest } from "../types/note";

export const notesApi = {
  async list(): Promise<Note[]> {
    const response = await apiClient.get<Note[]>("/notes");
    return response.data;
  },

  async upsert(id: string, request: UpsertNoteRequest): Promise<NoteUpsertResponse> {
    const response = await apiClient.put<NoteUpsertResponse>(`/notes/${id}`, request);
    return response.data;
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/notes/${id}`);
  },
};
