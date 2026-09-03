export interface SaleItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  sellingPrice: number;
  subtotal: number;
}

export type SaleStatus = "COMPLETED" | "CONFLICT" | "RESOLVED";

export interface Sale {
  id: string;
  employeeId: string;
  employeeName: string;
  clientTransactionId: string;
  totalAmount: number;
  status: SaleStatus;
  resolvedByUserId: string | null;
  resolvedByName: string | null;
  resolvedAt: string | null;
  resolutionNote: string | null;
  items: SaleItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateSaleItemRequest {
  productId: string;
  quantity: number;
  sellingPrice: number;
}

export interface CreateSaleRequest {
  clientTransactionId: string;
  items: CreateSaleItemRequest[];
}
