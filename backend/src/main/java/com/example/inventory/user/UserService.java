package com.example.inventory.user;

import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<UserResponse> listUsers() {
    return userRepository.findAll().stream().map(UserResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public UserResponse getUser(UUID id) {
    return UserResponse.from(findUserOrThrow(id));
  }

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new ConflictException("A user with this email already exists");
    }

    User user = new User();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(request.role());
    user.setActive(true);

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public UserResponse updateUser(UUID id, UpdateUserRequest request) {
    User user = findUserOrThrow(id);

    if (!user.getEmail().equalsIgnoreCase(request.email())
        && userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new ConflictException("A user with this email already exists");
    }

    boolean losingAdminStatus =
        user.getRole() == Role.ADMIN
            && user.isActive()
            && (request.role() != Role.ADMIN || !request.active());
    if (losingAdminStatus) {
      requireAnotherActiveAdmin(id);
    }

    user.setName(request.name());
    user.setEmail(request.email());
    user.setRole(request.role());
    user.setActive(request.active());

    return UserResponse.from(userRepository.save(user));
  }

  @Transactional
  public void deactivateUser(UUID id) {
    User user = findUserOrThrow(id);
    if (!user.isActive()) {
      return;
    }
    if (user.getRole() == Role.ADMIN) {
      requireAnotherActiveAdmin(id);
    }
    user.setActive(false);
    userRepository.save(user);
  }

  private void requireAnotherActiveAdmin(UUID excludingUserId) {
    long otherActiveAdmins =
        userRepository.findByRoleAndActiveTrue(Role.ADMIN).stream()
            .filter(admin -> !admin.getId().equals(excludingUserId))
            .count();
    if (otherActiveAdmins == 0) {
      throw new ConflictException("Cannot remove the last active admin account");
    }
  }

  private User findUserOrThrow(UUID id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
  }
}
