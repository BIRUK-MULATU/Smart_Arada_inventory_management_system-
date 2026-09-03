package com.example.inventory.sale;

import com.example.inventory.security.JwtUserPrincipal;
import com.example.inventory.user.Role;
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
@RequestMapping("/api/sales")
public class SaleController {

  private final SaleService saleService;

  public SaleController(SaleService saleService) {
    this.saleService = saleService;
  }

  @PostMapping
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<SaleResponse> createSale(
      @Valid @RequestBody CreateSaleRequest request,
      @AuthenticationPrincipal JwtUserPrincipal principal) {
    SaleCreationResult result = saleService.createSale(request, principal.userId());
    HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    ResponseEntity.BodyBuilder builder =
        result.created()
            ? ResponseEntity.status(status).location(URI.create("/api/sales/" + result.sale().id()))
            : ResponseEntity.status(status);
    return builder.body(result.sale());
  }

  @GetMapping("/{id}")
  public SaleResponse getSale(
      @PathVariable UUID id, @AuthenticationPrincipal JwtUserPrincipal principal) {
    return saleService.getSale(id, principal.userId(), principal.role() == Role.ADMIN);
  }

  @GetMapping
  public Page<SaleResponse> listSales(
      @AuthenticationPrincipal JwtUserPrincipal principal,
      @PageableDefault(size = 20) Pageable pageable) {
    return saleService.listSales(principal.userId(), principal.role() == Role.ADMIN, pageable);
  }
}
