export interface ConflictItem {
  productId: string;
  productName: string;
  quantityRequested: number;
  shortfall: number;
}

export interface Conflict {
  saleId: string;
  employeeId: string;
  employeeName: string;
  totalAmount: number;
  syncedAt: string;
  items: ConflictItem[];
}

export interface ConflictAdjustment {
  productId: string;
  restockQuantity: number;
}

export interface ResolveConflictRequest {
  adjustments: ConflictAdjustment[];
  note: string;
}
