package com.example.inventory.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryBreakdownResponse(
    UUID categoryId, String categoryName, long unitsSold, BigDecimal revenue, long stockOnHand) {}
