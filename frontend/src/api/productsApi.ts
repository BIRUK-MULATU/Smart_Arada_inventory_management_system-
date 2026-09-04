import { apiClient } from "./client";
import type { CreateProductRequest, Product, UpdateProductRequest } from "../types/product";

export const productsApi = {
  async list(includeInactive = false, categoryId?: string): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/products", { params: { includeInactive, categoryId } });
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

  async uploadImage(id: string, file: File): Promise<Product> {
    const formData = new FormData();
    formData.append("file", file);
    const response = await apiClient.post<Product>(`/products/${id}/image`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  async removeImage(id: string): Promise<Product> {
    const response = await apiClient.delete<Product>(`/products/${id}/image`);
    return response.data;
  },
};
