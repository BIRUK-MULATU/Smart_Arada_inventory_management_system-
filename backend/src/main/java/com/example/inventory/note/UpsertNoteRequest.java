package com.example.inventory.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * baseUpdatedAt is the updatedAt the client last saw for this note - null when creating a note for
 * the first time. On update, a mismatch against the note's current updatedAt means another device
 * changed it since, which NoteService treats as a conflict rather than an overwrite.
 */
public record UpsertNoteRequest(
    @Size(max = 200) String title, @NotBlank String content, Instant baseUpdatedAt) {}
