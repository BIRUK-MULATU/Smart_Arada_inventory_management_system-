import { apiClient } from "./client";
import type { CreateSaleRequest, Sale } from "../types/sale";
import type { Page } from "../types/inventory";

export const salesApi = {
  async list(page = 0, size = 20): Promise<Page<Sale>> {
    const response = await apiClient.get<Page<Sale>>("/sales", { params: { page, size } });
    return response.data;
  },

  async get(id: string): Promise<Sale> {
    const response = await apiClient.get<Sale>(`/sales/${id}`);
    return response.data;
  },

  async create(request: CreateSaleRequest): Promise<Sale> {
    const response = await apiClient.post<Sale>("/sales", request);
    return response.data;
  },
};
