export interface SaleItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  sellingPrice: number;
  subtotal: number;
}

export interface Sale {
  id: string;
  employeeId: string;
  employeeName: string;
  clientTransactionId: string;
  totalAmount: number;
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
