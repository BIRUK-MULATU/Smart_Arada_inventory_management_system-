package com.example.inventory.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base for append-only / audit-log entities whose tables have no {@code updated_at} column.
 *
 * <p>Implements {@link Persistable} for the same reason as {@link BaseEntity}: ids are
 * application-assigned, and a service that sets one up front (to reference it from a related row
 * before this entity is flushed) would otherwise make save() wrongly take the merge() path - see
 * {@link BaseEntity} for the full explanation.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class ImmutableEntity implements Persistable<UUID> {

  @Id private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Transient private boolean persisted = false;

  @PrePersist
  protected void ensureId() {
    if (id == null) {
      id = UUID.randomUUID();
    }
  }

  @PostPersist
  @PostLoad
  protected void markPersisted() {
    persisted = true;
  }

  @Override
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  @Override
  public boolean isNew() {
    return !persisted;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
