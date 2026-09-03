package com.example.inventory.auth;

import com.example.inventory.exception.InvalidCredentialsException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.security.JwtService;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import com.example.inventory.user.UserResponse;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailIgnoreCase(request.email())
            .filter(User::isActive)
            .filter(
                candidate ->
                    passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .orElseThrow(InvalidCredentialsException::new);

    String token = jwtService.generateToken(user);
    return new AuthResponse(token, UserResponse.from(user));
  }

  @Transactional(readOnly = true)
  public UserResponse currentUser(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .filter(User::isActive)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    return UserResponse.from(user);
  }
}
