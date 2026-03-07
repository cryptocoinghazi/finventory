-- V5__create_posting_schema_and_update_sales_invoice.sql

-- 1. Add warehouse_id to sales_invoices
-- Note: We assume there is at least one warehouse or the table is empty. 
-- If table has data, we might need a default value. For now, we assume dev environment.
ALTER TABLE sales_invoices ADD COLUMN warehouse_id UUID;
-- Make it not null after potentially filling it, but here we just add constraint assuming empty or reset
ALTER TABLE sales_invoices ADD CONSTRAINT fk_sales_invoice_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

-- 2. Create Stock Ledger Entries table
CREATE TABLE stock_ledger_entries (
    id UUID PRIMARY KEY,
    date DATE NOT NULL,
    item_id UUID NOT NULL REFERENCES items(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    qty_in DECIMAL(19, 2) NOT NULL,
    qty_out DECIMAL(19, 2) NOT NULL,
    ref_type VARCHAR(50) NOT NULL,
    ref_id UUID NOT NULL
);

CREATE INDEX idx_stock_ledger_date ON stock_ledger_entries(date);
CREATE INDEX idx_stock_ledger_item ON stock_ledger_entries(item_id);
CREATE INDEX idx_stock_ledger_warehouse ON stock_ledger_entries(warehouse_id);

-- 3. Create GL Transactions table
CREATE TABLE gl_transactions (
    id UUID PRIMARY KEY,
    date DATE NOT NULL,
    ref_type VARCHAR(50) NOT NULL,
    ref_id UUID NOT NULL,
    description VARCHAR(255)
);

CREATE INDEX idx_gl_transaction_date ON gl_transactions(date);
CREATE INDEX idx_gl_transaction_ref ON gl_transactions(ref_type, ref_id);

-- 4. Create GL Lines table
CREATE TABLE gl_lines (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES gl_transactions(id) ON DELETE CASCADE,
    account_head VARCHAR(100) NOT NULL,
    debit DECIMAL(19, 2) NOT NULL,
    credit DECIMAL(19, 2) NOT NULL
);

CREATE INDEX idx_gl_lines_transaction ON gl_lines(transaction_id);
CREATE INDEX idx_gl_lines_account ON gl_lines(account_head);
