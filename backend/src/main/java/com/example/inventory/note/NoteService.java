package com.example.inventory.note;

import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

  private final NoteRepository noteRepository;
  private final UserRepository userRepository;

  public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
    this.noteRepository = noteRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<NoteResponse> listNotes(UUID userId) {
    return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
        .map(NoteResponse::from)
        .toList();
  }

  /**
   * The note id is client-generated (offline-first, same as a sale's client transaction id), so
   * this is a create-or-update: a first sync creates the row, a retry or a later edit updates it. A
   * stale baseUpdatedAt - another device changed the note since this client last saw it - is never
   * overwritten; the incoming edit is saved as a separate note instead, so neither version is
   * silently lost.
   */
  @Transactional
  public NoteUpsertResponse upsertNote(UUID id, UpsertNoteRequest request, UUID userId) {
    Optional<Note> existing = noteRepository.findById(id);

    if (existing.isEmpty()) {
      Note note = new Note();
      note.setId(id);
      note.setUser(userRepository.getReferenceById(userId));
      note.setTitle(request.title());
      note.setContent(request.content());
      return new NoteUpsertResponse(NoteResponse.from(noteRepository.save(note)), false);
    }

    Note current = existing.get();
    if (!current.getUser().getId().equals(userId)) {
      // A client-generated id colliding with another user's note is practically impossible with
      // random UUIDs, but if it ever happened, this is reported identically to "not found" so a
      // crafted request can't confirm another user's note exists.
      throw new ResourceNotFoundException("Note not found: " + id);
    }

    if (Objects.equals(current.getTitle(), request.title())
        && current.getContent().equals(request.content())) {
      // Nothing to apply - this is either a genuine no-op save or a retry of a write that already
      // landed (e.g. the response to the first attempt was lost). Treating it as a conflict just
      // because baseUpdatedAt is stale/missing would create a needless duplicate on every retry.
      return new NoteUpsertResponse(NoteResponse.from(current), false);
    }

    boolean conflict =
        request.baseUpdatedAt() == null
            || !sameInstant(request.baseUpdatedAt(), current.getUpdatedAt());
    if (conflict) {
      Note conflictCopy = new Note();
      conflictCopy.setUser(current.getUser());
      conflictCopy.setTitle(conflictTitle(request.title()));
      conflictCopy.setContent(request.content());
      return new NoteUpsertResponse(NoteResponse.from(noteRepository.save(conflictCopy)), true);
    }

    current.setTitle(request.title());
    current.setContent(request.content());
    return new NoteUpsertResponse(NoteResponse.from(current), false);
  }

  /**
   * Idempotent: deleting a note that's already gone (or never belonged to this user) is a no-op.
   */
  @Transactional
  public void deleteNote(UUID id, UUID userId) {
    noteRepository
        .findById(id)
        .filter(note -> note.getUser().getId().equals(userId))
        .ifPresent(noteRepository::delete);
  }

  /**
   * Postgres TIMESTAMPTZ rounds (not truncates) to microsecond precision, while Instant.now() (used
   * for updatedAt in memory) is nanosecond precision - so an Instant round-tripped through the
   * database can land one microsecond away from the in-memory value it was written from, even when
   * nothing changed (e.g. .552917 in memory vs .553000 once rounded by the database). Both sides
   * are truncated to milliseconds - well past that rounding error - before comparing, so a
   * correctly-supplied baseUpdatedAt is never mistaken for a stale one.
   */
  private static boolean sameInstant(Instant a, Instant b) {
    return a.truncatedTo(ChronoUnit.MILLIS).equals(b.truncatedTo(ChronoUnit.MILLIS));
  }

  private static String conflictTitle(String title) {
    String base = (title == null || title.isBlank()) ? "Untitled" : title;
    return base + " (conflicting edit)";
  }
}
