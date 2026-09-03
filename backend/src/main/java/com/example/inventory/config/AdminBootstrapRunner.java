package com.example.inventory.config;

import com.example.inventory.user.Role;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String bootstrapEmail;
  private final String bootstrapPassword;

  public AdminBootstrapRunner(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.admin-bootstrap.email:}") String bootstrapEmail,
      @Value("${app.admin-bootstrap.password:}") String bootstrapPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.bootstrapEmail = bootstrapEmail;
    this.bootstrapPassword = bootstrapPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (userRepository.countByRoleAndActiveTrue(Role.ADMIN) > 0) {
      return;
    }

    if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
      log.warn(
          "No active admin account exists and ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD are not"
              + " set. Skipping admin bootstrap - create the first admin manually.");
      return;
    }

    if (userRepository.existsByEmailIgnoreCase(bootstrapEmail)) {
      log.warn(
          "A user with email {} already exists but is not an active admin. Skipping admin bootstrap.",
          bootstrapEmail);
      return;
    }

    User admin = new User();
    admin.setName("Administrator");
    admin.setEmail(bootstrapEmail);
    admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
    admin.setRole(Role.ADMIN);
    admin.setActive(true);
    userRepository.save(admin);

    log.info("Bootstrapped initial admin account: {}", bootstrapEmail);
  }
}
