package com.example.inventory.dashboard;

/** How sales-over-time points are bucketed. Maps directly to Postgres's date_trunc() units. */
public enum SalesGranularity {
  DAILY("day"),
  MONTHLY("month"),
  YEARLY("year");

  private final String truncUnit;

  SalesGranularity(String truncUnit) {
    this.truncUnit = truncUnit;
  }

  public String truncUnit() {
    return truncUnit;
  }
}
