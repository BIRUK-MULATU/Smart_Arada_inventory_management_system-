package com.example.inventory.inventory;

import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

  private static final String MANUAL_STOCK_IN_REFERENCE = "MANUAL_STOCK_IN";

  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final InventoryTransactionRepository transactionRepository;

  public InventoryService(
      ProductRepository productRepository,
      UserRepository userRepository,
      InventoryTransactionRepository transactionRepository) {
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.transactionRepository = transactionRepository;
  }

  @Transactional
  public InventoryTransactionResponse stockIn(StockInRequest request, UUID performedByUserId) {
    Product product =
        productRepository
            .findByIdForUpdate(request.productId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Product not found: " + request.productId()));
    User performedBy =
        userRepository
            .findById(performedByUserId)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found: " + performedByUserId));

    int previousQuantity = product.getStockQuantity();
    int newQuantity = previousQuantity + request.quantity();
    product.setStockQuantity(newQuantity);
    productRepository.save(product);

    InventoryTransaction transaction = new InventoryTransaction();
    transaction.setProduct(product);
    transaction.setType(TransactionType.STOCK_IN);
    transaction.setQuantity(request.quantity());
    transaction.setPreviousQuantity(previousQuantity);
    transaction.setNewQuantity(newQuantity);
    transaction.setReferenceType(MANUAL_STOCK_IN_REFERENCE);
    transaction.setReason(request.reason());
    transaction.setPerformedBy(performedBy);

    return InventoryTransactionResponse.from(transactionRepository.save(transaction));
  }

  @Transactional(readOnly = true)
  public Page<InventoryTransactionResponse> getHistory(UUID productId, Pageable pageable) {
    Page<InventoryTransaction> page =
        productId == null
            ? transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
            : transactionRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    return page.map(InventoryTransactionResponse::from);
  }
}
