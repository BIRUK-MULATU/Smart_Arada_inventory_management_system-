package com.example.inventory.sale;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

  /**
   * Eagerly fetches employee, items, and each item's product so the result is safe to read
   * (including by {@code SaleResponse.from}) outside of any transaction - this is used from {@code
   * SaleService.createSale}'s idempotency pre-check and race-condition fallback, both of which run
   * without an open Hibernate session by the time the result is converted to a DTO.
   */
  @Query(
      "SELECT DISTINCT s FROM Sale s "
          + "JOIN FETCH s.employee "
          + "LEFT JOIN FETCH s.items i "
          + "LEFT JOIN FETCH i.product "
          + "WHERE s.clientTransactionId = :clientTransactionId")
  Optional<Sale> findByClientTransactionId(@Param("clientTransactionId") UUID clientTransactionId);

  Page<Sale> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<Sale> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId, Pageable pageable);
}
