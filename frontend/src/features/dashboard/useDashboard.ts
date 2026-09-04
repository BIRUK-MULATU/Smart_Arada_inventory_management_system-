import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "../../api/dashboardApi";
import type { SalesGranularity } from "../../types/dashboard";

export function useDashboardSummary() {
  return useQuery({ queryKey: ["dashboard", "summary"], queryFn: () => dashboardApi.summary() });
}

export function useDashboardSales(granularity: SalesGranularity = "DAILY") {
  return useQuery({
    queryKey: ["dashboard", "sales", { granularity }],
    queryFn: () => dashboardApi.sales({}, granularity),
  });
}

export function useDashboardCategoryBreakdown() {
  return useQuery({
    queryKey: ["dashboard", "categories"],
    queryFn: () => dashboardApi.categoryBreakdown(),
  });
}

export function useDashboardTopProducts() {
  return useQuery({ queryKey: ["dashboard", "top-products"], queryFn: () => dashboardApi.topProducts() });
}

export function useDashboardLowStock() {
  return useQuery({ queryKey: ["dashboard", "low-stock"], queryFn: () => dashboardApi.lowStock() });
}
