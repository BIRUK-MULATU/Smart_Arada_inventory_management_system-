package com.example.inventory.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.inventory.inventory.InventoryTransactionRepository;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
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

  @Autowired protected InventoryTransactionRepository inventoryTransactionRepository;

  @Autowired protected PasswordEncoder passwordEncoder;

  @Autowired protected ObjectMapper objectMapper;

  @BeforeEach
  void cleanDatabase() {
    // inventory_transactions has RESTRICT foreign keys to both products and users, so it must
    // be cleared first.
    inventoryTransactionRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
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

  protected Product persistProduct(String name, String sku, int stockQuantity, boolean active) {
    Product product = new Product();
    product.setName(name);
    product.setSku(sku);
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
