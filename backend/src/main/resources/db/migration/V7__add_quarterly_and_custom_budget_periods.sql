-- Every budget now carries an explicit period_end instead of deriving it purely from period_type
-- and period_start. That derivation only worked for calendar months/years - it can't express an
-- admin-picked custom date range, and it can't express a quarter without new special-case code
-- for every reader of the table. Storing the end date explicitly (computed server-side for
-- MONTHLY/QUARTERLY/YEARLY, supplied by the admin for CUSTOM) makes every period type work the
-- same way from here on.
ALTER TABLE budgets ADD COLUMN period_end DATE;

UPDATE budgets SET period_end =
    CASE period_type
        WHEN 'MONTHLY' THEN (period_start + INTERVAL '1 month' - INTERVAL '1 day')::date
        WHEN 'YEARLY' THEN (period_start + INTERVAL '1 year' - INTERVAL '1 day')::date
    END;

ALTER TABLE budgets ALTER COLUMN period_end SET NOT NULL;
ALTER TABLE budgets ADD CONSTRAINT ck_budgets_period_end_not_before_start CHECK (period_end >= period_start);

ALTER TABLE budgets DROP CONSTRAINT budgets_period_type_check;
ALTER TABLE budgets ADD CONSTRAINT budgets_period_type_check
    CHECK (period_type IN ('MONTHLY', 'QUARTERLY', 'YEARLY', 'CUSTOM'));

-- period_start alone no longer uniquely identifies a scope's period once CUSTOM ranges exist (two
-- custom budgets in the same category could start on the same day but cover different lengths).
ALTER TABLE budgets DROP CONSTRAINT ux_budgets_scope;
ALTER TABLE budgets ADD CONSTRAINT ux_budgets_scope UNIQUE (category, period_type, period_start, period_end);
