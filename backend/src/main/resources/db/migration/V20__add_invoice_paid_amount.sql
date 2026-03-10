ALTER TABLE sales_invoices
    ADD COLUMN paid_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE purchase_invoices
    ADD COLUMN paid_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;

