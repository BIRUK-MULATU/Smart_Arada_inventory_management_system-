import { apiClient } from "./client";
import type { CategoryBreakdown, DailySalesPoint, DashboardSummary, SalesGranularity, TopProduct } from "../types/dashboard";
import type { Product } from "../types/product";

export interface DateRangeParams {
  from?: string;
  to?: string;
}

export const dashboardApi = {
  async summary(range: DateRangeParams = {}): Promise<DashboardSummary> {
    const response = await apiClient.get<DashboardSummary>("/dashboard/summary", { params: range });
    return response.data;
  },

  async sales(range: DateRangeParams = {}, granularity: SalesGranularity = "DAILY"): Promise<DailySalesPoint[]> {
    const response = await apiClient.get<DailySalesPoint[]>("/dashboard/sales", {
      params: { ...range, granularity },
    });
    return response.data;
  },

  async categoryBreakdown(range: DateRangeParams = {}): Promise<CategoryBreakdown[]> {
    const response = await apiClient.get<CategoryBreakdown[]>("/dashboard/categories", { params: range });
    return response.data;
  },

  async lowStock(): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/dashboard/low-stock");
    return response.data;
  },

  async topProducts(range: DateRangeParams = {}, limit = 10): Promise<TopProduct[]> {
    const response = await apiClient.get<TopProduct[]>("/dashboard/top-products", {
      params: { ...range, limit },
    });
    return response.data;
  },
};
