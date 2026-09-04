package com.example.inventory.dashboard;

import com.example.inventory.inventory.InventoryTransactionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
    long totalProducts,
    long totalStock,
    long totalEmployees,
    long lowStockCount,
    LocalDate periodFrom,
    LocalDate periodTo,
    long periodSalesCount,
    BigDecimal periodRevenue,
    // Point-in-time value of everything currently on the shelf - not period-bound, unlike
    // periodRevenue above. atBasePrice includes the selling margin; atCostPrice is the raw amount
    // spent acquiring it, so (atBasePrice - atCostPrice) is the profit still sitting in inventory.
    BigDecimal inventoryValueAtBasePrice,
    BigDecimal inventoryValueAtCostPrice,
    List<EmployeeSalesSummary> salesByEmployee,
    List<RecentSaleSummary> recentSales,
    List<InventoryTransactionResponse> recentInventoryMovements) {}
