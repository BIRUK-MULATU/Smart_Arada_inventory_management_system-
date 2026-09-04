package com.example.inventory.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class NoteControllerTest extends AbstractIntegrationTest {

  private MockHttpServletRequestBuilder putNote(
      UUID id, String token, String title, String content, String baseUpdatedAt) {
    String baseUpdatedAtJson = baseUpdatedAt == null ? "null" : "\"" + baseUpdatedAt + "\"";
    return put("/api/notes/" + id)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"title":%s,"content":"%s","baseUpdatedAt":%s}
            """
                .formatted(
                    title == null ? "null" : "\"" + title + "\"", content, baseUpdatedAtJson));
  }

  @Test
  void creatingANoteWithAClientGeneratedIdIsIdempotentOnRetry() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    UUID noteId = UUID.randomUUID();

    mockMvc
        .perform(putNote(noteId, token, "Shopping list", "Milk, eggs", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conflicted").value(false))
        .andExpect(jsonPath("$.note.id").value(noteId.toString()))
        .andExpect(jsonPath("$.note.content").value("Milk, eggs"));

    mockMvc
        .perform(putNote(noteId, token, "Shopping list", "Milk, eggs", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conflicted").value(false));

    assertThat(noteRepository.count()).isEqualTo(1);
  }

  @Test
  void updatingWithTheCorrectBaseUpdatedAtOverwritesInPlace() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    UUID noteId = UUID.randomUUID();

    String created =
        mockMvc
            .perform(putNote(noteId, token, "Title", "First draft", null))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String updatedAt = objectMapper.readTree(created).get("note").get("updatedAt").stringValue();

    mockMvc
        .perform(putNote(noteId, token, "Title", "Final draft", updatedAt))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conflicted").value(false))
        .andExpect(jsonPath("$.note.id").value(noteId.toString()))
        .andExpect(jsonPath("$.note.content").value("Final draft"));

    assertThat(noteRepository.count()).isEqualTo(1);
    assertThat(noteRepository.findById(noteId).orElseThrow().getContent()).isEqualTo("Final draft");
  }

  @Test
  void editingWithAStaleBaseUpdatedAtSavesAConflictCopyInsteadOfOverwriting() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    UUID noteId = UUID.randomUUID();

    mockMvc.perform(putNote(noteId, token, "Title", "Device A's edit", null));

    // Device B never learned about device A's edit, so it submits the id + an already-stale
    // baseUpdatedAt (null, as if it never synced since creation).
    mockMvc
        .perform(putNote(noteId, token, "Title", "Device B's edit", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conflicted").value(true))
        .andExpect(jsonPath("$.note.id").value(org.hamcrest.Matchers.not(noteId.toString())))
        .andExpect(jsonPath("$.note.title").value("Title (conflicting edit)"))
        .andExpect(jsonPath("$.note.content").value("Device B's edit"));

    assertThat(noteRepository.count()).isEqualTo(2);
    assertThat(noteRepository.findById(noteId).orElseThrow().getContent())
        .isEqualTo("Device A's edit");
  }

  @Test
  void aUserCannotSeeEditOrDeleteAnotherUsersNote() throws Exception {
    persistUser("alice@example.com", Role.EMPLOYEE, true);
    persistUser("bob@example.com", Role.EMPLOYEE, true);
    String aliceToken = loginAndGetToken("alice@example.com", RAW_PASSWORD);
    String bobToken = loginAndGetToken("bob@example.com", RAW_PASSWORD);
    UUID noteId = UUID.randomUUID();

    mockMvc.perform(putNote(noteId, aliceToken, "Alice's note", "Private", null));

    mockMvc
        .perform(get("/api/notes").header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // Bob crafting a PUT with Alice's note id doesn't overwrite it - it's reported as "not found"
    // (a brand new note for Bob), never as Alice's data.
    mockMvc
        .perform(putNote(noteId, bobToken, "Not Alice's note", "Bob tries to overwrite", null))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/notes/" + noteId).header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
        .andExpect(status().isNoContent());

    assertThat(noteRepository.findById(noteId).orElseThrow().getContent()).isEqualTo("Private");
  }

  @Test
  void adminCannotSeeAnEmployeesNotes() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    persistUser("admin@example.com", Role.ADMIN, true);
    String employeeToken = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    String adminToken = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    mockMvc.perform(
        putNote(UUID.randomUUID(), employeeToken, "Personal", "Not for admin eyes", null));

    mockMvc
        .perform(get("/api/notes").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void listReturnsOnlyTheCallersNotesNewestFirst() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc.perform(putNote(UUID.randomUUID(), token, "First", "First note", null));
    Thread.sleep(5);
    mockMvc.perform(putNote(UUID.randomUUID(), token, "Second", "Second note", null));

    mockMvc
        .perform(get("/api/notes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("Second"))
        .andExpect(jsonPath("$[1].title").value("First"));
  }

  @Test
  void deletingAnAlreadyDeletedNoteIsANoOp() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    UUID noteId = UUID.randomUUID();

    mockMvc.perform(putNote(noteId, token, "Title", "Content", null));

    mockMvc
        .perform(
            delete("/api/notes/" + noteId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            delete("/api/notes/" + noteId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    assertThat(noteRepository.count()).isEqualTo(0);
  }

  @Test
  void blankContentIsRejected() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);

    mockMvc
        .perform(putNote(UUID.randomUUID(), token, "Title", "", null))
        .andExpect(status().isBadRequest());
  }
}
