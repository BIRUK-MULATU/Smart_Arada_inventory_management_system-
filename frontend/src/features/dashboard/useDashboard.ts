import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "../../api/dashboardApi";

export function useDashboardSummary() {
  return useQuery({ queryKey: ["dashboard", "summary"], queryFn: () => dashboardApi.summary() });
}

export function useDashboardSales() {
  return useQuery({ queryKey: ["dashboard", "sales"], queryFn: () => dashboardApi.sales() });
}

export function useDashboardTopProducts() {
  return useQuery({ queryKey: ["dashboard", "top-products"], queryFn: () => dashboardApi.topProducts() });
}

export function useDashboardLowStock() {
  return useQuery({ queryKey: ["dashboard", "low-stock"], queryFn: () => dashboardApi.lowStock() });
}
