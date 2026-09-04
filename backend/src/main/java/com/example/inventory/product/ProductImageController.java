package com.example.inventory.product;

import com.example.inventory.exception.ResourceNotFoundException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/products/{id}/image")
public class ProductImageController {

  private final ProductRepository productRepository;
  private final ProductService productService;
  private final ProductImageStorageService storageService;

  public ProductImageController(
      ProductRepository productRepository,
      ProductService productService,
      ProductImageStorageService storageService) {
    this.productRepository = productRepository;
    this.productService = productService;
    this.storageService = storageService;
  }

  /**
   * Public and unauthenticated: browsers render {@code <img src="...">} without an Authorization
   * header, and a product photo of a household item isn't sensitive business data the way the rest
   * of this API is.
   */
  @GetMapping
  public ResponseEntity<Resource> getImage(@PathVariable UUID id) {
    Path path =
        storageService
            .find(id)
            .orElseThrow(() -> new ResourceNotFoundException("No image for product: " + id));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(storageService.contentTypeFor(path)))
        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
        .body(new FileSystemResource(path));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse uploadImage(@PathVariable UUID id, @RequestParam MultipartFile file) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Product not found: " + id);
    }
    storageService.store(id, file);

    String imageUrl =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/products/")
            .path(id.toString())
            .path("/image")
            .toUriString();
    return productService.setImageUrl(id, imageUrl);
  }

  @DeleteMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse deleteImage(@PathVariable UUID id) {
    if (!productRepository.existsById(id)) {
      throw new ResourceNotFoundException("Product not found: " + id);
    }
    storageService.delete(id);
    return productService.setImageUrl(id, null);
  }
}
