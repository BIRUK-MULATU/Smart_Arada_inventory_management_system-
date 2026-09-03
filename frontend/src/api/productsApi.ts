import { apiClient } from "./client";
import type { CreateProductRequest, Product, UpdateProductRequest } from "../types/product";

export const productsApi = {
  async list(includeInactive = false): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/products", { params: { includeInactive } });
    return response.data;
  },

  async get(id: string): Promise<Product> {
    const response = await apiClient.get<Product>(`/products/${id}`);
    return response.data;
  },

  async create(request: CreateProductRequest): Promise<Product> {
    const response = await apiClient.post<Product>("/products", request);
    return response.data;
  },

  async update(id: string, request: UpdateProductRequest): Promise<Product> {
    const response = await apiClient.put<Product>(`/products/${id}`, request);
    return response.data;
  },

  async deactivate(id: string): Promise<void> {
    await apiClient.delete(`/products/${id}`);
  },
};
