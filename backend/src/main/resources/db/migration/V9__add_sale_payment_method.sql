ALTER TABLE sales
    ADD COLUMN payment_method TEXT NOT NULL DEFAULT 'CASH',
    ADD COLUMN bank_account   TEXT;

ALTER TABLE sales
    ADD CONSTRAINT ck_sales_payment_method CHECK (payment_method IN ('CASH', 'BANK'));
