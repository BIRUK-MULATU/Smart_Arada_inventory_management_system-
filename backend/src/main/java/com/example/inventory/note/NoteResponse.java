package com.example.inventory.note;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
    UUID id, String title, String content, Instant createdAt, Instant updatedAt) {

  static NoteResponse from(Note note) {
    return new NoteResponse(
        note.getId(), note.getTitle(), note.getContent(), note.getCreatedAt(), note.getUpdatedAt());
  }
}
