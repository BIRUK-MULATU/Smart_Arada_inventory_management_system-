-- Offline sync (Phase 10): an offline sale represents a transaction that already happened - goods
-- already left the shop - so the sync path must be able to record it even when stock is short,
-- rather than rejecting it the way the online sale path does. A database-level CHECK constraint
-- can't distinguish which code path is writing, so the >= 0 floor on stock_quantity and on the
-- inventory ledger's previous/new quantity moves from the database to the application layer: the
-- online path (SaleTransactionExecutor) still rejects insufficient stock in code, unchanged.
ALTER TABLE products
    DROP CONSTRAINT products_stock_quantity_check;

ALTER TABLE inventory_transactions
    DROP CONSTRAINT inventory_transactions_previous_quantity_check,
    DROP CONSTRAINT inventory_transactions_new_quantity_check;

ALTER TABLE sales
    ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('COMPLETED', 'CONFLICT', 'RESOLVED')),
    ADD COLUMN resolved_by UUID REFERENCES users(id) ON DELETE RESTRICT,
    ADD COLUMN resolved_at TIMESTAMPTZ,
    ADD COLUMN resolution_note TEXT,
    ADD CONSTRAINT ck_sales_resolution_consistent CHECK (
        (status = 'RESOLVED' AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL) OR
        (status <> 'RESOLVED' AND resolved_by IS NULL AND resolved_at IS NULL)
    );

CREATE INDEX ix_sales_status ON sales (status) WHERE status <> 'COMPLETED';
