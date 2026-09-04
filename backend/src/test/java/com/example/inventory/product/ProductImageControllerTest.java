package com.example.inventory.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageControllerTest extends AbstractIntegrationTest {

  private static final byte[] FAKE_JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02
  };

  @Test
  void adminCanUploadAndTheImageBecomesPubliclyReadable() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", FAKE_JPEG_BYTES);

    String response =
        mockMvc
            .perform(
                multipart("/api/products/" + product.getId() + "/image")
                    .file(file)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String imageUrl = objectMapper.readTree(response).get("imageUrl").stringValue();
    assertThat(imageUrl).contains("/api/products/" + product.getId() + "/image");

    // No Authorization header - a plain <img> tag never sends one.
    mockMvc
        .perform(get("/api/products/" + product.getId() + "/image"))
        .andExpect(status().isOk())
        .andExpect(
            result -> assertThat(result.getResponse().getContentType()).isEqualTo("image/jpeg"))
        .andExpect(
            result ->
                assertThat(result.getResponse().getContentAsByteArray())
                    .isEqualTo(FAKE_JPEG_BYTES));
  }

  @Test
  void employeeCannotUploadOrDeleteAnImage() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", FAKE_JPEG_BYTES);

    mockMvc
        .perform(
            multipart("/api/products/" + product.getId() + "/image")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/products/" + product.getId() + "/image")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void uploadingAnUnsupportedFileTypeIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    MockMultipartFile file =
        new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

    mockMvc
        .perform(
            multipart("/api/products/" + product.getId() + "/image")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void gettingAnImageForAProductThatHasNoneReturnsNotFound() throws Exception {
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    mockMvc
        .perform(get("/api/products/" + product.getId() + "/image"))
        .andExpect(status().isNotFound());
  }

  @Test
  void adminCanDeleteAnUploadedImage() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);
    MockMultipartFile file =
        new MockMultipartFile("file", "photo.jpg", "image/jpeg", FAKE_JPEG_BYTES);

    mockMvc.perform(
        multipart("/api/products/" + product.getId() + "/image")
            .file(file)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

    mockMvc
        .perform(
            delete("/api/products/" + product.getId() + "/image")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").doesNotExist());

    mockMvc
        .perform(get("/api/products/" + product.getId() + "/image"))
        .andExpect(status().isNotFound());
  }

  @Test
  void reuploadingReplacesThePreviousImage() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var product = persistProduct("Widget", "WIDGET-1", 10, true);

    MockMultipartFile first = new MockMultipartFile("file", "a.jpg", "image/jpeg", FAKE_JPEG_BYTES);
    mockMvc.perform(
        multipart("/api/products/" + product.getId() + "/image")
            .file(first)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

    byte[] pngBytes = {(byte) 0x89, 0x50, 0x4E, 0x47};
    MockMultipartFile second = new MockMultipartFile("file", "b.png", "image/png", pngBytes);
    mockMvc.perform(
        multipart("/api/products/" + product.getId() + "/image")
            .file(second)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

    mockMvc
        .perform(get("/api/products/" + product.getId() + "/image"))
        .andExpect(status().isOk())
        .andExpect(
            result -> assertThat(result.getResponse().getContentType()).isEqualTo("image/png"))
        .andExpect(
            result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(pngBytes));
  }
}
