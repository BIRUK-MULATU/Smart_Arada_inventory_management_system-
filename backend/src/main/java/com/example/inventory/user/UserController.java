package com.example.inventory.user;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public List<UserResponse> listUsers() {
    return userService.listUsers();
  }

  @GetMapping("/{id}")
  public UserResponse getUser(@PathVariable UUID id) {
    return userService.getUser(id);
  }

  @PostMapping
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    UserResponse created = userService.createUser(request);
    return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  public UserResponse updateUser(
      @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    return userService.updateUser(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
    userService.deactivateUser(id);
    return ResponseEntity.noContent().build();
  }
}
