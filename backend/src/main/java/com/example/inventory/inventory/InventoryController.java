package com.example.inventory.inventory;

import com.example.inventory.product.ProductResponse;
import com.example.inventory.product.ProductService;
import com.example.inventory.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

  private final InventoryService inventoryService;
  private final ProductService productService;

  public InventoryController(InventoryService inventoryService, ProductService productService) {
    this.inventoryService = inventoryService;
    this.productService = productService;
  }

  @GetMapping
  public List<ProductResponse> listInventory() {
    return productService.listProducts(true);
  }

  @GetMapping("/history")
  public Page<InventoryTransactionResponse> getHistory(
      @RequestParam(required = false) UUID productId,
      @PageableDefault(size = 20) Pageable pageable) {
    return inventoryService.getHistory(productId, pageable);
  }

  @PostMapping("/stock-in")
  public ResponseEntity<InventoryTransactionResponse> stockIn(
      @Valid @RequestBody StockInRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    InventoryTransactionResponse response = inventoryService.stockIn(request, principal.userId());
    return ResponseEntity.status(201).body(response);
  }
}
