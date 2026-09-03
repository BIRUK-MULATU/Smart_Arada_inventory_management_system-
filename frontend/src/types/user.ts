export type Role = "ADMIN" | "EMPLOYEE";

export interface User {
  id: string;
  name: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface UpdateUserRequest {
  name: string;
  email: string;
  role: Role;
  active: boolean;
}
