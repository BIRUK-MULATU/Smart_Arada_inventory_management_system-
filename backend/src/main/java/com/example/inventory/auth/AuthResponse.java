package com.example.inventory.auth;

import com.example.inventory.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {}
