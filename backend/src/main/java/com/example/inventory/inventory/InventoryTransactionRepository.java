package com.example.inventory.inventory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

  Page<InventoryTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

  List<InventoryTransaction> findByReferenceTypeAndReferenceId(
      String referenceType, UUID referenceId);
}
