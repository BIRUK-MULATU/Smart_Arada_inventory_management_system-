package com.example.inventory.sync;

import com.example.inventory.sale.CreateSaleRequest;
import com.example.inventory.sale.SaleCreationResult;
import com.example.inventory.sale.SaleResponse;
import com.example.inventory.security.JwtUserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

  private final SyncService syncService;

  public SyncController(SyncService syncService) {
    this.syncService = syncService;
  }

  @PostMapping("/sales")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<SaleResponse> syncSale(
      @Valid @RequestBody CreateSaleRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    SaleCreationResult result = syncService.syncSale(request, principal.userId());
    HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    ResponseEntity.BodyBuilder builder =
        result.created()
            ? ResponseEntity.status(status).location(URI.create("/api/sales/" + result.sale().id()))
            : ResponseEntity.status(status);
    return builder.body(result.sale());
  }

  @GetMapping("/conflicts")
  @PreAuthorize("hasRole('ADMIN')")
  public Page<ConflictResponse> listOpenConflicts(@PageableDefault(size = 20) Pageable pageable) {
    return syncService.listOpenConflicts(pageable);
  }

  @PostMapping("/conflicts/{saleId}/resolve")
  @PreAuthorize("hasRole('ADMIN')")
  public SaleResponse resolveConflict(
      @PathVariable UUID saleId,
      @Valid @RequestBody ResolveConflictRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    return syncService.resolveConflict(saleId, request, principal.userId());
  }
}
