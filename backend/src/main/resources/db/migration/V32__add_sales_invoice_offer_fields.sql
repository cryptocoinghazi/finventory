ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS offer_id UUID REFERENCES offers(id);
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS offer_code VARCHAR(64);
ALTER TABLE sales_invoices ADD COLUMN IF NOT EXISTS offer_discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_sales_invoices_offer_id ON sales_invoices(offer_id);
