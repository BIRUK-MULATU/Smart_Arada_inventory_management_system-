package com.example.inventory.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    Role role,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getRole(),
        user.isActive(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
