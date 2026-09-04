export interface Product {
  id: string;
  name: string;
  sku: string | null;
  imageUrl: string | null;
  categoryId: string;
  categoryName: string;
  basePrice: number;
  // Admin-only - the server redacts this to null for an EMPLOYEE, so it is never actually present
  // in a response the employee-facing UI reads.
  costPrice: number | null;
  stockQuantity: number;
  lowStockThreshold: number;
  lowStock: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  name: string;
  sku?: string;
  imageUrl?: string;
  categoryId: string;
  basePrice: number;
  costPrice?: number;
  lowStockThreshold: number;
}

export interface UpdateProductRequest {
  name: string;
  sku?: string;
  imageUrl?: string;
  categoryId: string;
  basePrice: number;
  costPrice?: number;
  lowStockThreshold: number;
  active: boolean;
}
