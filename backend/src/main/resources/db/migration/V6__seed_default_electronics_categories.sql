-- This shop's catalog is primarily organized into two top-level categories: Electronics and
-- Non-Electronics. Rename the V4 bootstrap default (used only to backfill pre-existing products
-- with a valid category_id) to "Non-Electronics" so it doubles as the catch-all bucket for
-- everything that isn't electronics, rather than leaving it as a generic "Uncategorized" label.
-- Existing products keep their assignment - only the category's name changes.
UPDATE categories SET name = 'Non-Electronics'
WHERE lower(name) = 'uncategorized'
  AND NOT EXISTS (SELECT 1 FROM categories c2 WHERE lower(c2.name) = 'non-electronics');

-- Ensure "Electronics" exists too, without erroring if an admin already created it by hand -
-- matches the app's own case-insensitive uniqueness check on category names.
INSERT INTO categories (id, name)
SELECT gen_random_uuid(), 'Electronics'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE lower(name) = 'electronics');
