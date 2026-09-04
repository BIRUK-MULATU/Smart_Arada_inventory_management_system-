import { apiClient } from "./client";
import type { Category, CreateCategoryRequest } from "../types/category";

export const categoriesApi = {
  async list(): Promise<Category[]> {
    const response = await apiClient.get<Category[]>("/categories");
    return response.data;
  },

  async create(request: CreateCategoryRequest): Promise<Category> {
    const response = await apiClient.post<Category>("/categories", request);
    return response.data;
  },

  async rename(id: string, request: CreateCategoryRequest): Promise<Category> {
    const response = await apiClient.put<Category>(`/categories/${id}`, request);
    return response.data;
  },

  async remove(id: string): Promise<void> {
    await apiClient.delete(`/categories/${id}`);
  },
};
