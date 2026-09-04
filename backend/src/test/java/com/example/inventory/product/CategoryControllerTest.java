package com.example.inventory.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.support.AbstractIntegrationTest;
import com.example.inventory.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class CategoryControllerTest extends AbstractIntegrationTest {

  @Test
  void anyAuthenticatedRoleCanListCategories() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    persistCategory("Electronics");

    mockMvc
        .perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Electronics"));
  }

  @Test
  void noTokenIsUnauthorizedOnListCategories() throws Exception {
    mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
  }

  @Test
  void employeeCannotCreateRenameOrDeleteCategories() throws Exception {
    persistUser("employee@example.com", Role.EMPLOYEE, true);
    String token = loginAndGetToken("employee@example.com", RAW_PASSWORD);
    var category = persistCategory("Electronics");

    mockMvc
        .perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Household"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            put("/api/categories/" + category.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Renamed"}
                    """))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/api/categories/" + category.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanCreateRenameAndDeleteAnUnusedCategory() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);

    String created =
        mockMvc
            .perform(
                post("/api/categories")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Glassware"}
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String categoryId = objectMapper.readTree(created).get("id").stringValue();

    mockMvc
        .perform(
            put("/api/categories/" + categoryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Glasses"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Glasses"));

    mockMvc
        .perform(
            delete("/api/categories/" + categoryId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void creatingADuplicateCategoryNameIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    persistCategory("Electronics");

    mockMvc
        .perform(
            post("/api/categories")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"electronics"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void deletingACategoryStillInUseIsRejected() throws Exception {
    persistUser("admin@example.com", Role.ADMIN, true);
    String token = loginAndGetToken("admin@example.com", RAW_PASSWORD);
    var category = persistCategory("Electronics");
    var product = new Product();
    product.setName("TV");
    product.setCategory(category);
    product.setBasePrice(new java.math.BigDecimal("100.00"));
    product.setLowStockThreshold(1);
    productRepository.save(product);

    mockMvc
        .perform(
            delete("/api/categories/" + category.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest());
  }
}
