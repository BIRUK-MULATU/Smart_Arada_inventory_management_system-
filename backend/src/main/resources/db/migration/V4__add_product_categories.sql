CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_categories_name_lower ON categories (lower(name));

INSERT INTO categories (id, name) VALUES (gen_random_uuid(), 'Uncategorized');

ALTER TABLE products
    ADD COLUMN category_id UUID REFERENCES categories(id) ON DELETE RESTRICT;

UPDATE products
    SET category_id = (SELECT id FROM categories WHERE name = 'Uncategorized');

ALTER TABLE products
    ALTER COLUMN category_id SET NOT NULL;

CREATE INDEX ix_products_category_id ON products (category_id);
