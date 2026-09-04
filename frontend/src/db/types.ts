import type { CreateSaleRequest } from "../types/sale";
import type { UpsertNoteRequest } from "../types/note";

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

/**
 * baseUpdatedAt is the server's updatedAt as of the last successful sync - null for a note that
 * has never synced yet. updatedAtLocal drives sort order and is bumped on every local edit,
 * independent of whether that edit has synced.
 */
export interface LocalNote {
  id: string;
  title: string | null;
  content: string;
  baseUpdatedAt: string | null;
  syncStatus: SyncStatus;
  updatedAtLocal: string;
}

export type NoteSyncOperationType = "UPSERT_NOTE" | "DELETE_NOTE";

/** A separate queue from the sale sync queue above - notes have their own operations (including
 * delete) and a different conflict model, so reusing the sales queue's types would mean
 * shoehorning both into one union for no real benefit. */
export interface NoteSyncQueueEntry {
  id?: number;
  operationType: NoteSyncOperationType;
  entityId: string;
  payload: UpsertNoteRequest | null;
  status: SyncStatus;
  retryCount: number;
  lastAttemptAt: string | null;
  createdAt: string;
  errorMessage: string | null;
}
