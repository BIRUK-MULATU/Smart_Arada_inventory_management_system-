package com.example.inventory.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.inventory.inventory.InventoryTransactionRepository;
import com.example.inventory.product.Category;
import com.example.inventory.product.CategoryRepository;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import com.example.inventory.sale.SaleRepository;
import com.example.inventory.user.Role;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.math.BigDecimal;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  protected static final String RAW_PASSWORD = "correct-horse-battery";

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    var postgres = SharedPostgresContainer.instance();
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add(
        "app.jwt.secret",
        () ->
            Base64.getEncoder()
                .encodeToString("test-only-secret-key-material-32-bytes!!".getBytes()));
  }

  @Autowired protected MockMvc mockMvc;

  @Autowired protected UserRepository userRepository;

  @Autowired protected ProductRepository productRepository;

  @Autowired protected CategoryRepository categoryRepository;

  @Autowired protected InventoryTransactionRepository inventoryTransactionRepository;

  @Autowired protected SaleRepository saleRepository;

  @Autowired protected PasswordEncoder passwordEncoder;

  @Autowired protected ObjectMapper objectMapper;

  private Category cachedDefaultCategory;

  @BeforeEach
  void cleanDatabase() {
    // inventory_transactions and sales both have RESTRICT foreign keys to products/users, and
    // products has a RESTRICT foreign key to categories, so they must be cleared in this order.
    // sale_items cascades automatically when its parent sale is deleted.
    inventoryTransactionRepository.deleteAll();
    saleRepository.deleteAll();
    productRepository.deleteAll();
    categoryRepository.deleteAll();
    userRepository.deleteAll();
    cachedDefaultCategory = null;
  }

  protected User persistUser(String email, Role role, boolean active) {
    User user = new User();
    user.setName("Test " + role);
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
    user.setRole(role);
    user.setActive(active);
    return userRepository.save(user);
  }

  protected Category persistCategory(String name) {
    Category category = new Category();
    category.setName(name);
    return categoryRepository.save(category);
  }

  /** A category shared across every persistProduct() call within one test, created on first use. */
  protected Category defaultCategory() {
    if (cachedDefaultCategory == null) {
      cachedDefaultCategory = persistCategory("General");
    }
    return cachedDefaultCategory;
  }

  protected Product persistProduct(String name, String sku, int stockQuantity, boolean active) {
    Product product = new Product();
    product.setName(name);
    product.setSku(sku);
    product.setCategory(defaultCategory());
    product.setBasePrice(new BigDecimal("9.99"));
    product.setStockQuantity(stockQuantity);
    product.setLowStockThreshold(5);
    product.setActive(active);
    return productRepository.save(product);
  }

  protected String loginAndGetToken(String email, String password) throws Exception {
    String requestBody = objectMapper.writeValueAsString(new LoginPayload(email, password));
    String response =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("token").stringValue();
  }

  private record LoginPayload(String email, String password) {}
}
