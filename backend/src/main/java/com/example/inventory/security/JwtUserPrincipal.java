package com.example.inventory.security;

import com.example.inventory.user.Role;
import java.util.UUID;

public record JwtUserPrincipal(UUID userId, String email, Role role) {}
