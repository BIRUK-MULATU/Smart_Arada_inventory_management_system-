package com.example.inventory.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceSummaryResponse(
    LocalDate from,
    LocalDate to,
    BigDecimal revenue,
    BigDecimal costOfGoodsSold,
    BigDecimal grossProfit,
    BigDecimal totalExpenses,
    BigDecimal netProfit) {}
