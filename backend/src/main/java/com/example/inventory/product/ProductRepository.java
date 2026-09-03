package com.example.inventory.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  List<Product> findByActiveTrue();

  boolean existsBySku(String sku);

  boolean existsBySkuAndIdNot(String sku, UUID id);
}
