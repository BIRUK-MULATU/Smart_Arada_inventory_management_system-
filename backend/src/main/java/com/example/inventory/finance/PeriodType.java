package com.example.inventory.finance;

public enum PeriodType {
  MONTHLY,
  QUARTERLY,
  YEARLY,
  /** period_start and period_end are taken from the request as-is, with no normalization. */
  CUSTOM
}
