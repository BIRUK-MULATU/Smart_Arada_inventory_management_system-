package com.example.inventory.note;

import com.example.inventory.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notes are private to the authenticated user - ownership always comes from the JWT, never from the
 * request body, so nobody (including an admin) can list, edit, or delete another user's notes
 * through this API.
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @GetMapping
  public List<NoteResponse> listNotes(@AuthenticationPrincipal JwtUserPrincipal principal) {
    return noteService.listNotes(principal.userId());
  }

  @PutMapping("/{id}")
  public NoteUpsertResponse upsertNote(
      @PathVariable UUID id,
      @Valid @RequestBody UpsertNoteRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    return noteService.upsertNote(id, request, principal.userId());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteNote(
      @PathVariable UUID id, @AuthenticationPrincipal JwtUserPrincipal principal) {
    noteService.deleteNote(id, principal.userId());
  }
}
