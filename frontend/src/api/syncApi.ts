import { apiClient } from "./client";
import type { Page } from "../types/inventory";
import type { CreateSaleRequest, Sale } from "../types/sale";
import type { Conflict, ResolveConflictRequest } from "../types/sync";

export const syncApi = {
  async syncSale(request: CreateSaleRequest): Promise<Sale> {
    const response = await apiClient.post<Sale>("/sync/sales", request);
    return response.data;
  },

  async listConflicts(page = 0, size = 20): Promise<Page<Conflict>> {
    const response = await apiClient.get<Page<Conflict>>("/sync/conflicts", { params: { page, size } });
    return response.data;
  },

  async resolveConflict(saleId: string, request: ResolveConflictRequest): Promise<Sale> {
    const response = await apiClient.post<Sale>(`/sync/conflicts/${saleId}/resolve`, request);
    return response.data;
  },
};
