package com.example.inventory.sale;

import com.example.inventory.exception.BadRequestException;
import com.example.inventory.exception.ConflictException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.inventory.InventoryTransaction;
import com.example.inventory.inventory.InventoryTransactionRepository;
import com.example.inventory.inventory.TransactionType;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import com.example.inventory.user.User;
import com.example.inventory.user.UserRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds the atomic "create a sale" transaction as its own bean, separate from {@link SaleService},
 * so {@code SaleService} can safely catch the unique-constraint violation this throws on a
 * concurrent idempotent retry. Catching an exception thrown by a @Transactional method only works
 * correctly if the call crosses a Spring proxy boundary - calling a @Transactional method on `this`
 * from within the same class bypasses the proxy entirely and the annotation has no effect.
 */
@Service
class SaleTransactionExecutor {

  private static final String SALE_REFERENCE_TYPE = "SALE";

  private final SaleRepository saleRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;

  SaleTransactionExecutor(
      SaleRepository saleRepository,
      ProductRepository productRepository,
      UserRepository userRepository,
      InventoryTransactionRepository inventoryTransactionRepository) {
    this.saleRepository = saleRepository;
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
  }

  @Transactional
  Sale execute(CreateSaleRequest request, UUID employeeUserId) {
    User employee =
        userRepository
            .findById(employeeUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + employeeUserId));

    Map<UUID, Integer> requestedQuantityByProduct = new LinkedHashMap<>();
    for (CreateSaleItemRequest item : request.items()) {
      Integer previous = requestedQuantityByProduct.putIfAbsent(item.productId(), item.quantity());
      if (previous != null) {
        throw new BadRequestException(
            "Duplicate product in sale items: "
                + item.productId()
                + " - combine into one line item");
      }
    }

    // Lock products in a fixed order (sorted by id) across every concurrent sale, regardless
    // of the order they appear in the request, so two sales that touch the same two products
    // can never deadlock waiting on each other's lock.
    Map<UUID, Product> lockedProducts = new LinkedHashMap<>();
    requestedQuantityByProduct.keySet().stream()
        .sorted(Comparator.naturalOrder())
        .forEach(
            productId ->
                lockedProducts.put(
                    productId,
                    productRepository
                        .findByIdForUpdate(productId)
                        .orElseThrow(
                            () ->
                                new ResourceNotFoundException("Product not found: " + productId))));

    for (Map.Entry<UUID, Integer> entry : requestedQuantityByProduct.entrySet()) {
      Product product = lockedProducts.get(entry.getKey());
      int requestedQuantity = entry.getValue();
      if (!product.isActive()) {
        throw new ConflictException("Product is not available for sale: " + product.getId());
      }
      if (product.getStockQuantity() < requestedQuantity) {
        throw new ConflictException(
            "Insufficient stock for product "
                + product.getId()
                + ": requested "
                + requestedQuantity
                + ", available "
                + product.getStockQuantity());
      }
    }

    Sale sale = new Sale();
    sale.setId(UUID.randomUUID());
    sale.setEmployee(employee);
    sale.setClientTransactionId(request.clientTransactionId());

    BigDecimal totalAmount = BigDecimal.ZERO;
    for (CreateSaleItemRequest itemRequest : request.items()) {
      Product product = lockedProducts.get(itemRequest.productId());
      BigDecimal subtotal =
          itemRequest.sellingPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
      totalAmount = totalAmount.add(subtotal);

      SaleItem saleItem = new SaleItem();
      saleItem.setProduct(product);
      saleItem.setQuantity(itemRequest.quantity());
      saleItem.setSellingPrice(itemRequest.sellingPrice());
      saleItem.setCostPrice(product.getCostPrice());
      saleItem.setSubtotal(subtotal);
      sale.addItem(saleItem);
    }
    sale.setTotalAmount(totalAmount);
    saleRepository.save(sale);

    for (Map.Entry<UUID, Integer> entry : requestedQuantityByProduct.entrySet()) {
      Product product = lockedProducts.get(entry.getKey());
      int quantity = entry.getValue();
      int previousQuantity = product.getStockQuantity();
      int newQuantity = previousQuantity - quantity;
      product.setStockQuantity(newQuantity);
      productRepository.save(product);

      InventoryTransaction transaction = new InventoryTransaction();
      transaction.setProduct(product);
      transaction.setType(TransactionType.STOCK_OUT);
      transaction.setQuantity(quantity);
      transaction.setPreviousQuantity(previousQuantity);
      transaction.setNewQuantity(newQuantity);
      transaction.setReferenceType(SALE_REFERENCE_TYPE);
      transaction.setReferenceId(sale.getId());
      transaction.setPerformedBy(employee);
      inventoryTransactionRepository.save(transaction);
    }

    return sale;
  }
}
