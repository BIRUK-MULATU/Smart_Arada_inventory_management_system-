package com.example.inventory.dashboard;

import com.example.inventory.product.ProductResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

  private static final int DEFAULT_TOP_PRODUCTS_LIMIT = 10;

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/summary")
  public DashboardSummaryResponse getSummary(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return dashboardService.getSummary(from, to);
  }

  @GetMapping("/sales")
  public List<DailySalesPoint> getSales(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "DAILY") SalesGranularity granularity) {
    return dashboardService.getSalesOverTime(from, to, granularity);
  }

  @GetMapping("/categories")
  public List<CategoryBreakdownResponse> getCategoryBreakdown(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return dashboardService.getCategoryBreakdown(from, to);
  }

  @GetMapping("/low-stock")
  public List<ProductResponse> getLowStock() {
    return dashboardService.getLowStock();
  }

  @GetMapping("/top-products")
  public List<TopProductResponse> getTopProducts(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "" + DEFAULT_TOP_PRODUCTS_LIMIT) int limit) {
    return dashboardService.getTopProducts(from, to, limit);
  }
}
