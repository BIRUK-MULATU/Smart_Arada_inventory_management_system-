CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    email           TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
CREATE INDEX ix_users_role ON users (role);

CREATE TABLE products (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                TEXT NOT NULL,
    sku                 TEXT,
    image_url           TEXT,
    base_price          DECIMAL(12,2) NOT NULL CHECK (base_price >= 0),
    stock_quantity      INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 0 CHECK (low_stock_threshold >= 0),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_products_sku ON products (sku) WHERE sku IS NOT NULL;
CREATE INDEX ix_products_name ON products (name);
CREATE INDEX ix_products_low_stock ON products (id)
    WHERE is_active = TRUE AND stock_quantity <= low_stock_threshold;

CREATE TABLE sales (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id             UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    client_transaction_id   UUID NOT NULL,
    total_amount            DECIMAL(12,2) NOT NULL CHECK (total_amount >= 0),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_sales_client_transaction_id UNIQUE (client_transaction_id)
);

CREATE INDEX ix_sales_employee_id ON sales (employee_id);
CREATE INDEX ix_sales_created_at ON sales (created_at);

CREATE TABLE sale_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id         UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id      UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    selling_price   DECIMAL(12,2) NOT NULL CHECK (selling_price >= 0),
    subtotal        DECIMAL(12,2) NOT NULL CHECK (subtotal >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX ix_sale_items_product_id ON sale_items (product_id);

CREATE TABLE inventory_transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    type                TEXT NOT NULL CHECK (type IN ('STOCK_IN', 'STOCK_OUT')),
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    previous_quantity   INTEGER NOT NULL CHECK (previous_quantity >= 0),
    new_quantity        INTEGER NOT NULL CHECK (new_quantity >= 0),
    reference_type      TEXT,
    reference_id        UUID,
    performed_by        UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_inventory_transactions_ledger_consistent CHECK (
        (type = 'STOCK_IN'  AND new_quantity = previous_quantity + quantity) OR
        (type = 'STOCK_OUT' AND new_quantity = previous_quantity - quantity)
    )
);

CREATE INDEX ix_inventory_transactions_product_id ON inventory_transactions (product_id);
CREATE INDEX ix_inventory_transactions_created_at ON inventory_transactions (created_at);
CREATE INDEX ix_inventory_transactions_performed_by ON inventory_transactions (performed_by);
