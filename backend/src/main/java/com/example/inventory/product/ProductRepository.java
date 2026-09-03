package com.example.inventory.product;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  List<Product> findByActiveTrue();

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, UUID id);

  /**
   * Locks the product row for the duration of the caller's transaction, so concurrent stock
   * mutations (stock-in, and later sales-driven stock-out) serialize instead of racing on a stale
   * in-memory quantity.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id = :id")
  Optional<Product> findByIdForUpdate(UUID id);
}
