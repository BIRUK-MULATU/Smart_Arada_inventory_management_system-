package com.example.inventory.product;

import com.example.inventory.exception.BadRequestException;
import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  public CategoryService(
      CategoryRepository categoryRepository, ProductRepository productRepository) {
    this.categoryRepository = categoryRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> listCategories() {
    return categoryRepository.findAllByOrderByNameAsc().stream()
        .map(CategoryResponse::from)
        .toList();
  }

  @Transactional
  public CategoryResponse createCategory(CreateCategoryRequest request) {
    if (categoryRepository.existsByNameIgnoreCase(request.name())) {
      throw new ConflictException("A category with this name already exists");
    }
    Category category = new Category();
    category.setName(request.name());
    return CategoryResponse.from(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse renameCategory(UUID id, CreateCategoryRequest request) {
    Category category = findCategoryOrThrow(id);
    if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new ConflictException("A category with this name already exists");
    }
    category.setName(request.name());
    return CategoryResponse.from(categoryRepository.save(category));
  }

  @Transactional
  public void deleteCategory(UUID id) {
    Category category = findCategoryOrThrow(id);
    if (productRepository.existsByCategoryId(id)) {
      throw new BadRequestException(
          "This category still has products assigned to it - move them to another category first");
    }
    categoryRepository.delete(category);
  }

  private Category findCategoryOrThrow(UUID id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
  }
}
