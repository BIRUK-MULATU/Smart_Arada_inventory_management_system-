import type { CreateSaleRequest } from "../types/sale";

/**
 * PENDING: written locally, not yet attempted.
 * SYNCING: an in-flight sync attempt is running (set by the Phase 10 sync engine).
 * SYNCED: the server has confirmed this record - only the server confirming makes something
 *   "recorded"; everything before that is "saved on this device".
 * FAILED: a sync attempt errored and can be retried (Phase 10).
 * CONFLICT: the server accepted the sale but flagged it (e.g. stock went negative) for admin
 *   review - per the accepted strategy, the sale is never deleted or reversed once this happens.
 */
export type SyncStatus = "PENDING" | "SYNCING" | "SYNCED" | "FAILED" | "CONFLICT";

export interface LocalProduct {
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
}

/** Keyed by clientTransactionId itself - before sync, that's the only identity a sale has. */
export interface LocalSale {
  clientTransactionId: string;
  employeeId: string;
  totalAmount: number;
  syncStatus: SyncStatus;
  createdAt: string;
}

export interface LocalSaleItem {
  id: string;
  saleClientTransactionId: string;
  productId: string;
  productName: string;
  quantity: number;
  sellingPrice: number;
  subtotal: number;
}

/** Singleton row recording when the product cache was last refreshed from the server. */
export interface InventorySnapshotRecord {
  id: "current";
  fetchedAt: string;
  productCount: number;
}

export type SyncOperationType = "CREATE_SALE";
export type SyncEntityType = "SALE";

export interface SyncQueueEntry {
  id?: number;
  operationType: SyncOperationType;
  entityType: SyncEntityType;
  entityId: string;
  clientTransactionId: string;
  payload: CreateSaleRequest;
  status: SyncStatus;
  retryCount: number;
  lastAttemptAt: string | null;
  createdAt: string;
  errorMessage: string | null;
}

/**
 * Reserved for the Phase 10 sync engine's own bookkeeping (e.g. last queue-drain run). Schema
 * only in Phase 9 - nothing writes to this table yet.
 */
export interface SyncMetadataRecord {
  id: "sync";
  lastSyncAttemptAt?: string;
}
