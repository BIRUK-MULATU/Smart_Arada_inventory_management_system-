package com.example.inventory.finance;

import com.example.inventory.sale.SaleItemRepository;
import com.example.inventory.sale.SaleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {

  private static final int DEFAULT_PERIOD_DAYS = 30;

  private final SaleRepository saleRepository;
  private final SaleItemRepository saleItemRepository;
  private final ExpenseRepository expenseRepository;

  public FinanceService(
      SaleRepository saleRepository,
      SaleItemRepository saleItemRepository,
      ExpenseRepository expenseRepository) {
    this.saleRepository = saleRepository;
    this.saleItemRepository = saleItemRepository;
    this.expenseRepository = expenseRepository;
  }

  @Transactional(readOnly = true)
  public FinanceSummaryResponse getSummary(LocalDate from, LocalDate to) {
    LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneOffset.UTC);
    LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_PERIOD_DAYS - 1L);
    Instant fromInstant = effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant toInstant = effectiveTo.atTime(LocalTime.MAX).atZone(ZoneOffset.UTC).toInstant();

    var revenue = saleRepository.sumRevenueBetween(fromInstant, toInstant);
    var costOfGoodsSold = saleItemRepository.sumCostOfGoodsSoldBetween(fromInstant, toInstant);
    var grossProfit = revenue.subtract(costOfGoodsSold);
    var totalExpenses = expenseRepository.sumAmountBetween(effectiveFrom, effectiveTo);
    var netProfit = grossProfit.subtract(totalExpenses);

    return new FinanceSummaryResponse(
        effectiveFrom,
        effectiveTo,
        revenue,
        costOfGoodsSold,
        grossProfit,
        totalExpenses,
        netProfit);
  }
}
