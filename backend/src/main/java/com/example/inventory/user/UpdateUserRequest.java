package com.example.inventory.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotNull Role role,
    @NotNull Boolean active) {}
