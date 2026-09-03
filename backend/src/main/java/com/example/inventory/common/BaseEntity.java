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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Implements {@link Persistable} because every entity's id is application-assigned (offline-first
 * clients must be able to generate an id before a row ever reaches the server), not database
 * generated. Spring Data JPA's default "is this new?" heuristic is "does getId() return null?",
 * which is wrong here: a service that needs to know an entity's id before it's flushed (e.g. to
 * reference it from a related row in the same transaction) sets the id up front, which would
 * otherwise make save() take the merge() path instead of persist() - silently skipping @PrePersist
 * callbacks (auditing timestamps, cascaded children's own id generation) since JPA would treat it
 * as an update to an existing row instead of an insert.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Persistable<UUID> {

  @Id private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

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

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
