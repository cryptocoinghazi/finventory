ALTER TABLE parties ADD COLUMN state_code VARCHAR(255);
ALTER TABLE warehouses ADD COLUMN state_code VARCHAR(255);

ALTER TABLE sales_invoices ADD COLUMN total_cgst_amount DECIMAL(19, 2) DEFAULT 0 NOT NULL;
ALTER TABLE sales_invoices ADD COLUMN total_sgst_amount DECIMAL(19, 2) DEFAULT 0 NOT NULL;
ALTER TABLE sales_invoices ADD COLUMN total_igst_amount DECIMAL(19, 2) DEFAULT 0 NOT NULL;

ALTER TABLE sales_invoice_lines ADD COLUMN cgst_amount DECIMAL(19, 2) DEFAULT 0 NOT NULL;
ALTER TABLE sales_invoice_lines ADD COLUMN sgst_amount DECIMAL(19, 2) DEFAULT 0 NOT NULL;
ALTER TABLE sales_invoice_lines ADD COLUMN igst_amount DECIMAL(19, 2) DEFAULT 0 NOT NULL;
