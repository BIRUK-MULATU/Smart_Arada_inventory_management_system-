import { apiClient } from "./client";
import type { AuthResponse, LoginRequest } from "../types/auth";
import type { User } from "../types/user";

export const authApi = {
  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>("/auth/login", request);
    return response.data;
  },

  async logout(): Promise<void> {
    await apiClient.post("/auth/logout");
  },

  async me(): Promise<User> {
    const response = await apiClient.get<User>("/auth/me");
    return response.data;
  },
};
