package com.example.inventory.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A single Postgres container shared by every test class in the suite (Testcontainers' singleton
 * pattern), instead of each @SpringBootTest class starting its own. Started once on first access
 * and left running for the JVM's lifetime; Testcontainers' Ryuk reaper stops it on exit.
 */
public final class SharedPostgresContainer {

  private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:17");

  static {
    INSTANCE.start();
  }

  private SharedPostgresContainer() {}

  public static PostgreSQLContainer<?> instance() {
    return INSTANCE;
  }
}
