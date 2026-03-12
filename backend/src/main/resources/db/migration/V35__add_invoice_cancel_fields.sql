ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS cancel_reason TEXT;

ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE purchase_invoices ADD COLUMN IF NOT EXISTS cancel_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_sales_invoices_deleted_at ON sales_invoices(deleted_at);
CREATE INDEX IF NOT EXISTS idx_purchase_invoices_deleted_at ON purchase_invoices(deleted_at);
