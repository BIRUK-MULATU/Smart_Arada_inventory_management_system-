import { apiClient } from "./client";
import type { InventoryTransaction, Page, StockInRequest } from "../types/inventory";
import type { Product } from "../types/product";

export const inventoryApi = {
  async list(): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/inventory");
    return response.data;
  },

  async history(productId?: string, page = 0, size = 20): Promise<Page<InventoryTransaction>> {
    const response = await apiClient.get<Page<InventoryTransaction>>("/inventory/history", {
      params: { productId, page, size },
    });
    return response.data;
  },

  async stockIn(request: StockInRequest): Promise<InventoryTransaction> {
    const response = await apiClient.post<InventoryTransaction>("/inventory/stock-in", request);
    return response.data;
  },
};
