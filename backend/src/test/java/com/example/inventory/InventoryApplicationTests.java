package com.example.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.inventory.support.SharedPostgresContainer;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class InventoryApplicationTests {

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

  @Test
  void contextLoads(ApplicationContext context) {
    assertThat(context).isNotNull();
  }
}
