export interface Product {
  id: string;
  name: string;
  sku: string | null;
  imageUrl: string | null;
  categoryId: string;
  categoryName: string;
  basePrice: number;
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
  lowStockThreshold: number;
}

export interface UpdateProductRequest {
  name: string;
  sku?: string;
  imageUrl?: string;
  categoryId: string;
  basePrice: number;
  lowStockThreshold: number;
  active: boolean;
}
