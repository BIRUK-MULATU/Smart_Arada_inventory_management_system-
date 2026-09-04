package com.example.inventory.product;

import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  public ProductService(
      ProductRepository productRepository, CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> listProducts(boolean includeInactive, UUID categoryId) {
    List<Product> products;
    if (categoryId != null) {
      products =
          includeInactive
              ? productRepository.findByCategoryId(categoryId)
              : productRepository.findByActiveTrueAndCategoryId(categoryId);
    } else {
      products =
          includeInactive ? productRepository.findAll() : productRepository.findByActiveTrue();
    }
    return products.stream().map(ProductResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public ProductResponse getProduct(UUID id) {
    return ProductResponse.from(findProductOrThrow(id));
  }

  @Transactional
  public ProductResponse createProduct(CreateProductRequest request) {
    if (hasSkuConflict(request.sku(), null)) {
      throw new ConflictException("A product with this SKU already exists");
    }

    Product product = new Product();
    product.setName(request.name());
    product.setSku(request.sku());
    product.setImageUrl(request.imageUrl());
    product.setCategory(findCategoryOrThrow(request.categoryId()));
    product.setBasePrice(request.basePrice());
    product.setLowStockThreshold(request.lowStockThreshold());
    product.setActive(true);

    return ProductResponse.from(productRepository.save(product));
  }

  @Transactional
  public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
    Product product = findProductOrThrow(id);

    if (hasSkuConflict(request.sku(), id)) {
      throw new ConflictException("A product with this SKU already exists");
    }

    product.setName(request.name());
    product.setSku(request.sku());
    product.setImageUrl(request.imageUrl());
    product.setCategory(findCategoryOrThrow(request.categoryId()));
    product.setBasePrice(request.basePrice());
    product.setLowStockThreshold(request.lowStockThreshold());
    product.setActive(request.active());

    return ProductResponse.from(productRepository.save(product));
  }

  @Transactional
  public void deactivateProduct(UUID id) {
    Product product = findProductOrThrow(id);
    if (!product.isActive()) {
      return;
    }
    product.setActive(false);
    productRepository.save(product);
  }

  /** Used by {@link ProductImageController} after storing/deleting the file on disk. */
  @Transactional
  public ProductResponse setImageUrl(UUID id, String imageUrl) {
    Product product = findProductOrThrow(id);
    product.setImageUrl(imageUrl);
    return ProductResponse.from(productRepository.save(product));
  }

  private boolean hasSkuConflict(String sku, UUID excludingId) {
    if (sku == null || sku.isBlank()) {
      return false;
    }
    return excludingId == null
        ? productRepository.existsBySku(sku)
        : productRepository.existsBySkuAndIdNot(sku, excludingId);
  }

  private Product findProductOrThrow(UUID id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
  }

  private Category findCategoryOrThrow(UUID categoryId) {
    return categoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
  }
}
