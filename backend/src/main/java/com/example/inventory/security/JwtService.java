package com.example.inventory.security;

import com.example.inventory.user.Role;
import com.example.inventory.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final String ROLE_CLAIM = "role";
  private static final String EMAIL_CLAIM = "email";

  private final SecretKey signingKey;
  private final long expiryMinutes;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("app.jwt.secret must be configured");
    }
    this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    this.expiryMinutes = expiryMinutes;
  }

  public String generateToken(User user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim(EMAIL_CLAIM, user.getEmail())
        .claim(ROLE_CLAIM, user.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expiryMinutes, ChronoUnit.MINUTES)))
        .signWith(signingKey)
        .compact();
  }

  public JwtUserPrincipal parseToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    UUID userId = UUID.fromString(claims.getSubject());
    String email = claims.get(EMAIL_CLAIM, String.class);
    Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
    return new JwtUserPrincipal(userId, email, role);
  }
}
