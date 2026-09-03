package com.example.inventory.sale;

import com.example.inventory.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleService {

  private final SaleRepository saleRepository;
  private final SaleTransactionExecutor saleTransactionExecutor;

  public SaleService(
      SaleRepository saleRepository, SaleTransactionExecutor saleTransactionExecutor) {
    this.saleRepository = saleRepository;
    this.saleTransactionExecutor = saleTransactionExecutor;
  }

  public SaleCreationResult createSale(CreateSaleRequest request, UUID employeeUserId) {
    var existing = saleRepository.findByClientTransactionId(request.clientTransactionId());
    if (existing.isPresent()) {
      return new SaleCreationResult(SaleResponse.from(existing.get()), false);
    }

    try {
      Sale sale = saleTransactionExecutor.execute(request, employeeUserId);
      return new SaleCreationResult(SaleResponse.from(sale), true);
    } catch (DataIntegrityViolationException ex) {
      // Two identical retries landed at the same instant and both passed the pre-check
      // above; the database's unique constraint on client_transaction_id is the real
      // idempotency guarantee. Recover the row the other request just committed instead of
      // surfacing this as an error.
      return saleRepository
          .findByClientTransactionId(request.clientTransactionId())
          .map(sale -> new SaleCreationResult(SaleResponse.from(sale), false))
          .orElseThrow(() -> ex);
    }
  }

  @Transactional(readOnly = true)
  public SaleResponse getSale(UUID saleId, UUID requesterId, boolean requesterIsAdmin) {
    Sale sale =
        saleRepository
            .findById(saleId)
            .orElseThrow(() -> new ResourceNotFoundException("Sale not found: " + saleId));
    if (!requesterIsAdmin && !sale.getEmployee().getId().equals(requesterId)) {
      // Not this employee's sale - reported as not-found rather than forbidden, so an
      // employee probing sale IDs can't learn that another employee's sale exists.
      throw new ResourceNotFoundException("Sale not found: " + saleId);
    }
    return SaleResponse.from(sale);
  }

  @Transactional(readOnly = true)
  public Page<SaleResponse> listSales(
      UUID requesterId, boolean requesterIsAdmin, Pageable pageable) {
    Page<Sale> page =
        requesterIsAdmin
            ? saleRepository.findAllByOrderByCreatedAtDesc(pageable)
            : saleRepository.findByEmployeeIdOrderByCreatedAtDesc(requesterId, pageable);
    return page.map(SaleResponse::from);
  }
}
