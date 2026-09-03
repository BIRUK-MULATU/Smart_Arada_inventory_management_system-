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
    List<EmployeeSalesSummary> salesByEmployee,
    List<RecentSaleSummary> recentSales,
    List<InventoryTransactionResponse> recentInventoryMovements) {}
