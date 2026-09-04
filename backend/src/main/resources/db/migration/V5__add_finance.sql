-- Cost-of-goods basis for products, captured per sale line at the moment of sale (same pattern as
-- selling_price) so historical profit stays accurate even if a product's cost later changes.
ALTER TABLE products
    ADD COLUMN cost_price DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0);

ALTER TABLE sale_items
    ADD COLUMN cost_price DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (cost_price >= 0);

-- Expense and budget "category" is free text (admin-typed, e.g. "Rent", "Utilities",
-- "Restocking") - a distinct concept from the product categories added in V4, so it does not
-- reuse that table. 'Overall' is the sentinel for "not tied to a specific category", stored as a
-- literal value (not NULL) so the budgets uniqueness constraint below behaves as expected -
-- Postgres treats every NULL as distinct, which would silently defeat a UNIQUE constraint that
-- included a nullable column.
CREATE TABLE expenses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category        TEXT NOT NULL DEFAULT 'Overall',
    description     TEXT NOT NULL,
    amount          DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    incurred_on     DATE NOT NULL,
    recorded_by     UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_expenses_incurred_on ON expenses (incurred_on);
CREATE INDEX ix_expenses_category ON expenses (category);

CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category        TEXT NOT NULL DEFAULT 'Overall',
    period_type     TEXT NOT NULL CHECK (period_type IN ('MONTHLY', 'YEARLY')),
    period_start    DATE NOT NULL,
    amount          DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    created_by      UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_budgets_scope UNIQUE (category, period_type, period_start)
);

CREATE INDEX ix_budgets_period_start ON budgets (period_start);
