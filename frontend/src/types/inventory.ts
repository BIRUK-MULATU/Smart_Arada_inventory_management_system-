export type TransactionType = "STOCK_IN" | "STOCK_OUT";

export interface InventoryTransaction {
  id: string;
  productId: string;
  productName: string;
  type: TransactionType;
  quantity: number;
  previousQuantity: number;
  newQuantity: number;
  referenceType: string | null;
  referenceId: string | null;
  reason: string | null;
  performedByUserId: string;
  performedByName: string;
  createdAt: string;
}

export interface StockInRequest {
  productId: string;
  quantity: number;
  reason?: string;
}

export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
