import Dexie, { type EntityTable } from "dexie";
import type {
  InventorySnapshotRecord,
  LocalProduct,
  LocalSale,
  LocalSaleItem,
  SyncMetadataRecord,
  SyncQueueEntry,
} from "./types";

export class AppDatabase extends Dexie {
  products!: EntityTable<LocalProduct, "id">;
  sales!: EntityTable<LocalSale, "clientTransactionId">;
  saleItems!: EntityTable<LocalSaleItem, "id">;
  inventorySnapshot!: EntityTable<InventorySnapshotRecord, "id">;
  syncQueue!: EntityTable<SyncQueueEntry, "id">;
  syncMetadata!: EntityTable<SyncMetadataRecord, "id">;

  constructor(name = "inventory-sales") {
    super(name);
    // Version 1: initial offline schema (Phase 9). Future schema changes must add a new
    // .version(N) block rather than editing this one, the same discipline as the backend's
    // Flyway migrations - though Dexie versions restate the full cumulative schema rather than
    // incremental DDL, with an optional upgrade() for data transforms.
    this.version(1).stores({
      products: "id, name, sku, active",
      sales: "clientTransactionId, employeeId, createdAt, syncStatus",
      saleItems: "id, saleClientTransactionId, productId",
      inventorySnapshot: "id",
      syncQueue: "++id, entityType, entityId, clientTransactionId, status, createdAt",
      syncMetadata: "id",
    });
  }
}

export const db = new AppDatabase();
