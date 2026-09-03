package com.example.inventory.product;

import com.example.inventory.security.JwtUserPrincipal;
import com.example.inventory.user.Role;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping
  public List<ProductResponse> listProducts(
      @RequestParam(defaultValue = "false") boolean includeInactive,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    boolean effectiveIncludeInactive = includeInactive && principal.role() == Role.ADMIN;
    return productService.listProducts(effectiveIncludeInactive);
  }

  @GetMapping("/{id}")
  public ProductResponse getProduct(@PathVariable UUID id) {
    return productService.getProduct(id);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProductResponse> createProduct(
      @Valid @RequestBody CreateProductRequest request) {
    ProductResponse created = productService.createProduct(request);
    return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse updateProduct(
      @PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
    return productService.updateProduct(id, request);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deactivateProduct(@PathVariable UUID id) {
    productService.deactivateProduct(id);
    return ResponseEntity.noContent().build();
  }
}
