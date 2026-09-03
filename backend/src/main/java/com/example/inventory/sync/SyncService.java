package com.example.inventory.sync;

import com.example.inventory.exception.BadRequestException;
import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.inventory.InventoryTransaction;
import com.example.inventory.inventory.InventoryTransactionRepository;
import com.example.inventory.inventory.TransactionType;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import com.example.inventory.sale.CreateSaleRequest;
import com.example.inventory.sale.Sale;
import com.example.inventory.sale.SaleCreationResult;
import com.example.inventory.sale.SaleRepository;
import com.example.inventory.sale.SaleResponse;
import com.example.inventory.sale.SaleStatus;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncService {

  private static final String SALE_REFERENCE_TYPE = "SALE";
  private static final String CONFLICT_RESOLUTION_REFERENCE_TYPE = "CONFLICT_RESOLUTION";

  private final SaleRepository saleRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final SyncSaleExecutor syncSaleExecutor;

  public SyncService(
      SaleRepository saleRepository,
      ProductRepository productRepository,
      UserRepository userRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      SyncSaleExecutor syncSaleExecutor) {
    this.saleRepository = saleRepository;
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.syncSaleExecutor = syncSaleExecutor;
  }

  /**
   * Syncs one offline sale. The client_transaction_id is checked first - if a sale with this id
   * already exists (this device retried, or the same request arrived twice), the original sale and
   * its result are returned unchanged; a second row is never created, no matter how many times this
   * is called.
   */
  public SaleCreationResult syncSale(CreateSaleRequest request, UUID employeeUserId) {
    var existing = saleRepository.findByClientTransactionId(request.clientTransactionId());
    if (existing.isPresent()) {
      return new SaleCreationResult(SaleResponse.from(existing.get()), false);
    }

    try {
      Sale sale = syncSaleExecutor.execute(request, employeeUserId);
      return new SaleCreationResult(SaleResponse.from(sale), true);
    } catch (DataIntegrityViolationException ex) {
      // Two syncs of the same client_transaction_id (e.g. a request retried after the response
      // was lost mid-flight) landed at the same instant and both passed the pre-check above; the
      // database's unique constraint is the real idempotency guarantee. Recover the row the other
      // request just committed instead of surfacing this as an error.
      return saleRepository
          .findByClientTransactionId(request.clientTransactionId())
          .map(sale -> new SaleCreationResult(SaleResponse.from(sale), false))
          .orElseThrow(() -> ex);
    }
  }

  @Transactional(readOnly = true)
  public Page<ConflictResponse> listOpenConflicts(Pageable pageable) {
    return saleRepository
        .findByStatusOrderByCreatedAtDesc(SaleStatus.CONFLICT, pageable)
        .map(
            sale ->
                ConflictResponse.from(
                    sale,
                    inventoryTransactionRepository.findByReferenceTypeAndReferenceId(
                        SALE_REFERENCE_TYPE, sale.getId())));
  }

  @Transactional
  public SaleResponse resolveConflict(
      UUID saleId, ResolveConflictRequest request, UUID adminUserId) {
    Sale sale =
        saleRepository
            .findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
    if (sale.getStatus() != SaleStatus.CONFLICT) {
      throw new ConflictException("Sale " + saleId + " is not an open conflict");
    }

    User admin =
        userRepository
            .findById(adminUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + adminUserId));

    Set<UUID> saleProductIds =
        sale.getItems().stream().map(item -> item.getProduct().getId()).collect(Collectors.toSet());
    for (ConflictAdjustment adjustment : request.adjustments()) {
      if (!saleProductIds.contains(adjustment.productId())) {
        throw new BadRequestException(
            "Product " + adjustment.productId() + " is not part of sale " + saleId);
      }
    }

    for (ConflictAdjustment adjustment : request.adjustments()) {
      if (adjustment.restockQuantity() <= 0) {
        continue;
      }
      Product product =
          productRepository
              .findByIdForUpdate(adjustment.productId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Product not found: " + adjustment.productId()));

      int previousQuantity = product.getStockQuantity();
      int newQuantity = previousQuantity + adjustment.restockQuantity();
      product.setStockQuantity(newQuantity);
      productRepository.save(product);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(product);
      transaction.setType(TransactionType.STOCK_IN);
      transaction.setQuantity(adjustment.restockQuantity());
      transaction.setPreviousQuantity(previousQuantity);
      transaction.setNewQuantity(newQuantity);
      transaction.setReferenceType(CONFLICT_RESOLUTION_REFERENCE_TYPE);
      transaction.setReferenceId(sale.getId());
      transaction.setReason(request.note());
      transaction.setPerformedBy(admin);
      inventoryTransactionRepository.save(transaction);
    }

    sale.resolveConflict(admin, Instant.now(), request.note());
    return SaleResponse.from(sale);
  }
}
