package com.example.inventory.sync;

import com.example.inventory.exception.BadRequestException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.inventory.InventoryTransaction;
import com.example.inventory.inventory.InventoryTransactionRepository;
import com.example.inventory.inventory.TransactionType;
import com.example.inventory.product.Product;
import com.example.inventory.product.ProductRepository;
import com.example.inventory.sale.CreateSaleItemRequest;
import com.example.inventory.sale.CreateSaleRequest;
import com.example.inventory.sale.PaymentMethod;
import com.example.inventory.sale.Sale;
import com.example.inventory.sale.SaleItem;
import com.example.inventory.sale.SaleRepository;
import com.example.inventory.sale.SaleStatus;
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
 * Holds the atomic "sync an offline sale" transaction as its own bean, separate from {@link
 * SyncService}, for the same reason {@code SaleTransactionExecutor} is split from {@code
 * SaleService}: catching the unique-constraint violation thrown by a concurrent idempotent retry
 * only works if the call crosses a Spring proxy boundary.
 *
 * <p>Unlike the online sale path, this never rejects a sale for insufficient stock. A synced
 * offline sale represents a transaction that already happened - the goods already left the shop -
 * so it is always recorded. When stock is short, it is allowed to go negative and the sale is
 * flagged CONFLICT for admin review instead of being refused.
 */
@Service
class SyncSaleExecutor {

  private static final String SALE_REFERENCE_TYPE = "SALE";

  private final SaleRepository saleRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;

  SyncSaleExecutor(
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

    if (request.paymentMethod() == PaymentMethod.BANK
        && (request.bankAccount() == null || request.bankAccount().isBlank())) {
      throw new BadRequestException("Bank account is required when payment method is Bank");
    }

    // Lock products in a fixed order, same as the online path, so a sync and an online sale (or
    // two syncs) touching the same products can never deadlock waiting on each other's lock.
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

    // Determine conflict status up front so the sale's one and only insert already carries the
    // correct status - a product being inactive does not block the sync: the sale already
    // happened on the device before the product may have been deactivated, so only genuinely
    // invalid input (unknown product/user, bad price or quantity) is rejected here.
    boolean anyShortfall = false;
    for (Map.Entry<UUID, Integer> entry : requestedQuantityByProduct.entrySet()) {
      if (lockedProducts.get(entry.getKey()).getStockQuantity() < entry.getValue()) {
        anyShortfall = true;
      }
    }

    Sale sale = new Sale();
    sale.setId(UUID.randomUUID());
    sale.setEmployee(employee);
    sale.setClientTransactionId(request.clientTransactionId());
    sale.setPaymentMethod(request.paymentMethod());
    sale.setBankAccount(
        request.paymentMethod() == PaymentMethod.BANK ? request.bankAccount() : null);
    sale.setStatus(anyShortfall ? SaleStatus.CONFLICT : SaleStatus.COMPLETED);

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
      if (newQuantity < 0) {
        transaction.setReason(
            "Offline sync conflict: requested "
                + quantity
                + ", only "
                + previousQuantity
                + " in stock at sync time - short by "
                + (-newQuantity));
      }
      inventoryTransactionRepository.save(transaction);
    }

    return sale;
  }
}
