package com.example.inventory.product;

import com.example.inventory.dashboard.CategoryStockProjection;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  // Sorted with LOWER(name) rather than a plain derived-method OrderByName: seeded/real product
  // names mix case ("TV", "stov", ...) and the database's default collation may sort
  // uppercase-first, which would scatter lowercase-led names out of the alphabetical order a user
  // actually expects.
  @Query("SELECT p FROM Product p ORDER BY LOWER(p.name)")
  List<Product> findAllOrderedByName();

  @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY LOWER(p.name)")
  List<Product> findByActiveTrueOrderedByName();

  @Query(
      "SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId ORDER BY LOWER(p.name)")
  List<Product> findByActiveTrueAndCategoryIdOrderedByName(UUID categoryId);

  @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId ORDER BY LOWER(p.name)")
  List<Product> findByCategoryIdOrderedByName(UUID categoryId);

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, UUID id);

  boolean existsByCategoryId(UUID categoryId);

  /**
   * Locks the product row for the duration of the caller's transaction, so concurrent stock
   * mutations (stock-in, and later sales-driven stock-out) serialize instead of racing on a stale
   * in-memory quantity.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Product p WHERE p.id = :id")
  Optional<Product> findByIdForUpdate(UUID id);

  @Query("SELECT COALESCE(SUM(p.stockQuantity), 0) FROM Product p")
  long sumStockQuantity();

  /**
   * Current stock valued at selling price - what all of it would bring in if sold, margin included.
   */
  @Query("SELECT COALESCE(SUM(p.stockQuantity * p.basePrice), 0) FROM Product p")
  BigDecimal sumInventoryValueAtBasePrice();

  /** Current stock valued at what it cost to acquire - no markup, the raw cost basis on hand. */
  @Query("SELECT COALESCE(SUM(p.stockQuantity * p.costPrice), 0) FROM Product p")
  BigDecimal sumInventoryValueAtCostPrice();

  @Query(
      "SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity <= p.lowStockThreshold")
  long countLowStock();

  @Query(
      "SELECT p FROM Product p WHERE p.active = true AND p.stockQuantity <= p.lowStockThreshold ORDER BY p.name")
  List<Product> findLowStock();

  @Query(
      "SELECT p.category.id AS categoryId, p.category.name AS categoryName, "
          + "COALESCE(SUM(p.stockQuantity), 0) AS stockOnHand FROM Product p "
          + "WHERE p.active = true GROUP BY p.category.id, p.category.name")
  List<CategoryStockProjection> sumStockByCategory();
}
