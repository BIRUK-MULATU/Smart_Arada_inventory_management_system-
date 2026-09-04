package com.example.inventory.dashboard;

import com.example.inventory.inventory.InventoryTransactionRepository;
import com.example.inventory.inventory.InventoryTransactionResponse;
import com.example.inventory.product.ProductRepository;
import com.example.inventory.product.ProductResponse;
import com.example.inventory.sale.SaleItemRepository;
import com.example.inventory.sale.SaleRepository;
import com.example.inventory.user.Role;
import com.example.inventory.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private static final int RECENT_ITEMS_LIMIT = 10;
  private static final int DEFAULT_PERIOD_DAYS = 30;

  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final SaleRepository saleRepository;
  private final SaleItemRepository saleItemRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;

  public DashboardService(
      ProductRepository productRepository,
      UserRepository userRepository,
      SaleRepository saleRepository,
      SaleItemRepository saleItemRepository,
      InventoryTransactionRepository inventoryTransactionRepository) {
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.saleRepository = saleRepository;
    this.saleItemRepository = saleItemRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
  }

  @Transactional(readOnly = true)
  public DashboardSummaryResponse getSummary(LocalDate from, LocalDate to) {
    DateRange range = resolveRange(from, to);

    long totalProducts = productRepository.count();
    long totalStock = productRepository.sumStockQuantity();
    long totalEmployees = userRepository.countByRoleAndActiveTrue(Role.EMPLOYEE);
    long lowStockCount = productRepository.countLowStock();

    long periodSalesCount = saleRepository.countByCreatedAtBetween(range.from(), range.to());
    var periodRevenue = saleRepository.sumRevenueBetween(range.from(), range.to());

    List<EmployeeSalesSummary> salesByEmployee =
        saleRepository.findSalesByEmployee(range.from(), range.to()).stream()
            .map(EmployeeSalesSummary::from)
            .toList();

    List<RecentSaleSummary> recentSales =
        saleRepository.findRecentSales(PageRequest.of(0, RECENT_ITEMS_LIMIT)).stream()
            .map(RecentSaleSummary::from)
            .toList();

    List<InventoryTransactionResponse> recentInventoryMovements =
        inventoryTransactionRepository
            .findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_ITEMS_LIMIT))
            .map(InventoryTransactionResponse::from)
            .getContent();

    return new DashboardSummaryResponse(
        totalProducts,
        totalStock,
        totalEmployees,
        lowStockCount,
        range.fromDate(),
        range.toDate(),
        periodSalesCount,
        periodRevenue,
        salesByEmployee,
        recentSales,
        recentInventoryMovements);
  }

  @Transactional(readOnly = true)
  public List<DailySalesPoint> getSalesOverTime(
      LocalDate from, LocalDate to, SalesGranularity granularity) {
    DateRange range = resolveRange(from, to);
    return saleRepository
        .findSalesGroupedByPeriod(range.from(), range.to(), granularity.truncUnit())
        .stream()
        .map(DailySalesPoint::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> getLowStock() {
    return productRepository.findLowStock().stream().map(ProductResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<TopProductResponse> getTopProducts(LocalDate from, LocalDate to, int limit) {
    DateRange range = resolveRange(from, to);
    return saleItemRepository
        .findTopProducts(range.from(), range.to(), PageRequest.of(0, limit))
        .stream()
        .map(TopProductResponse::from)
        .toList();
  }

  /**
   * Revenue and units sold per category for the period, merged with each category's current
   * stock-on-hand (not period-bound - stock is a point-in-time figure). A category with stock but
   * no sales in the period, or sales but its products have since been deactivated, still appears.
   */
  @Transactional(readOnly = true)
  public List<CategoryBreakdownResponse> getCategoryBreakdown(LocalDate from, LocalDate to) {
    DateRange range = resolveRange(from, to);

    Map<UUID, CategoryAccumulator> byCategory = new LinkedHashMap<>();
    for (CategoryStockProjection stock : productRepository.sumStockByCategory()) {
      byCategory.put(
          stock.getCategoryId(),
          new CategoryAccumulator(
              stock.getCategoryName(), 0, BigDecimal.ZERO, stock.getStockOnHand()));
    }
    for (CategorySalesProjection sales :
        saleItemRepository.findSalesByCategory(range.from(), range.to())) {
      long stockOnHand =
          byCategory.containsKey(sales.getCategoryId())
              ? byCategory.get(sales.getCategoryId()).stockOnHand()
              : 0;
      byCategory.put(
          sales.getCategoryId(),
          new CategoryAccumulator(
              sales.getCategoryName(), sales.getUnitsSold(), sales.getRevenue(), stockOnHand));
    }

    return byCategory.entrySet().stream()
        .map(
            entry ->
                new CategoryBreakdownResponse(
                    entry.getKey(),
                    entry.getValue().categoryName(),
                    entry.getValue().unitsSold(),
                    entry.getValue().revenue(),
                    entry.getValue().stockOnHand()))
        .sorted(Comparator.comparing(CategoryBreakdownResponse::revenue).reversed())
        .toList();
  }

  private record CategoryAccumulator(
      String categoryName, long unitsSold, BigDecimal revenue, long stockOnHand) {}

  private DateRange resolveRange(LocalDate from, LocalDate to) {
    LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneOffset.UTC);
    LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_PERIOD_DAYS - 1L);
    Instant fromInstant = effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant toInstant = effectiveTo.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();
    return new DateRange(fromInstant, toInstant, effectiveFrom, effectiveTo);
  }

  private record DateRange(Instant from, Instant to, LocalDate fromDate, LocalDate toDate) {}
}
