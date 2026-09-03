package com.example.inventory.sale;

import com.example.inventory.dashboard.TopProductProjection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

  @Query(
      "SELECT i.product.id AS productId, i.product.name AS productName, "
          + "SUM(i.quantity) AS unitsSold, COALESCE(SUM(i.subtotal), 0) AS revenue "
          + "FROM SaleItem i WHERE i.createdAt BETWEEN :from AND :to "
          + "GROUP BY i.product.id, i.product.name ORDER BY SUM(i.quantity) DESC")
  List<TopProductProjection> findTopProducts(
      @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
