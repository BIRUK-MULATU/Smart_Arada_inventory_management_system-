import { apiClient } from "./client";
import type { CreateUserRequest, UpdateUserRequest, User } from "../types/user";

export const usersApi = {
  async list(): Promise<User[]> {
    const response = await apiClient.get<User[]>("/users");
    return response.data;
  },

  async get(id: string): Promise<User> {
    const response = await apiClient.get<User>(`/users/${id}`);
    return response.data;
  },

  async create(request: CreateUserRequest): Promise<User> {
    const response = await apiClient.post<User>("/users", request);
    return response.data;
  },

  async update(id: string, request: UpdateUserRequest): Promise<User> {
    const response = await apiClient.put<User>(`/users/${id}`, request);
    return response.data;
  },

  async deactivate(id: string): Promise<void> {
    await apiClient.delete(`/users/${id}`);
  },
};
