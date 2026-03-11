CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    gstin VARCHAR(64) UNIQUE,
    state_code VARCHAR(16),
    address VARCHAR(512),
    phone VARCHAR(64),
    email VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    state_code VARCHAR(16),
    location VARCHAR(255),
    code VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS tax_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rate NUMERIC NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL UNIQUE,
    hsn_code VARCHAR(64),
    tax_rate NUMERIC NOT NULL,
    unit_price NUMERIC NOT NULL,
    cogs NUMERIC,
    uom VARCHAR(64) NOT NULL,
    image_url VARCHAR(512),
    barcode VARCHAR(64) UNIQUE,
    vendor_id UUID,
    CONSTRAINT fk_items_vendor FOREIGN KEY (vendor_id) REFERENCES parties(id)
);

CREATE TABLE IF NOT EXISTS sales_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL,
    party_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    total_taxable_amount NUMERIC NOT NULL,
    total_tax_amount NUMERIC NOT NULL,
    total_cgst_amount NUMERIC NOT NULL,
    total_sgst_amount NUMERIC NOT NULL,
    total_igst_amount NUMERIC NOT NULL,
    grand_total NUMERIC NOT NULL,
    paid_amount NUMERIC NOT NULL DEFAULT 0,
    payment_status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_sales_invoice_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_sales_invoice_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE IF NOT EXISTS sales_invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_invoice_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity NUMERIC NOT NULL,
    unit_price NUMERIC NOT NULL,
    tax_rate NUMERIC NOT NULL,
    tax_amount NUMERIC NOT NULL,
    cgst_amount NUMERIC NOT NULL,
    sgst_amount NUMERIC NOT NULL,
    igst_amount NUMERIC NOT NULL,
    line_total NUMERIC NOT NULL,
    CONSTRAINT fk_sales_invoice_lines_invoice FOREIGN KEY (sales_invoice_id) REFERENCES sales_invoices(id),
    CONSTRAINT fk_sales_invoice_lines_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS purchase_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    vendor_invoice_number VARCHAR(255),
    invoice_date DATE NOT NULL,
    party_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    total_taxable_amount NUMERIC NOT NULL,
    total_tax_amount NUMERIC NOT NULL,
    total_cgst_amount NUMERIC NOT NULL,
    total_sgst_amount NUMERIC NOT NULL,
    total_igst_amount NUMERIC NOT NULL,
    grand_total NUMERIC NOT NULL,
    paid_amount NUMERIC NOT NULL DEFAULT 0,
    payment_status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_purchase_invoice_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_purchase_invoice_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE IF NOT EXISTS purchase_invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_invoice_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity NUMERIC NOT NULL,
    unit_price NUMERIC NOT NULL,
    tax_rate NUMERIC NOT NULL,
    tax_amount NUMERIC NOT NULL,
    cgst_amount NUMERIC NOT NULL,
    sgst_amount NUMERIC NOT NULL,
    igst_amount NUMERIC NOT NULL,
    line_total NUMERIC NOT NULL,
    CONSTRAINT fk_purchase_invoice_lines_invoice FOREIGN KEY (purchase_invoice_id) REFERENCES purchase_invoices(id),
    CONSTRAINT fk_purchase_invoice_lines_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS sales_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number VARCHAR(255) NOT NULL UNIQUE,
    sales_invoice_id UUID,
    return_date DATE NOT NULL,
    party_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    total_taxable_amount NUMERIC NOT NULL,
    total_tax_amount NUMERIC NOT NULL,
    total_cgst_amount NUMERIC NOT NULL,
    total_sgst_amount NUMERIC NOT NULL,
    total_igst_amount NUMERIC NOT NULL,
    grand_total NUMERIC NOT NULL,
    CONSTRAINT fk_sales_return_sales_invoice FOREIGN KEY (sales_invoice_id) REFERENCES sales_invoices(id),
    CONSTRAINT fk_sales_return_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_sales_return_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE IF NOT EXISTS sales_return_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sales_return_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity NUMERIC NOT NULL,
    unit_price NUMERIC NOT NULL,
    tax_rate NUMERIC NOT NULL,
    tax_amount NUMERIC NOT NULL,
    cgst_amount NUMERIC NOT NULL,
    sgst_amount NUMERIC NOT NULL,
    igst_amount NUMERIC NOT NULL,
    line_total NUMERIC NOT NULL,
    CONSTRAINT fk_sales_return_lines_return FOREIGN KEY (sales_return_id) REFERENCES sales_returns(id),
    CONSTRAINT fk_sales_return_lines_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS purchase_returns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number VARCHAR(255) NOT NULL UNIQUE,
    purchase_invoice_id UUID,
    return_date DATE NOT NULL,
    party_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    total_taxable_amount NUMERIC NOT NULL,
    total_tax_amount NUMERIC NOT NULL,
    total_cgst_amount NUMERIC NOT NULL,
    total_sgst_amount NUMERIC NOT NULL,
    total_igst_amount NUMERIC NOT NULL,
    grand_total NUMERIC NOT NULL,
    CONSTRAINT fk_purchase_return_purchase_invoice FOREIGN KEY (purchase_invoice_id) REFERENCES purchase_invoices(id),
    CONSTRAINT fk_purchase_return_party FOREIGN KEY (party_id) REFERENCES parties(id),
    CONSTRAINT fk_purchase_return_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE IF NOT EXISTS purchase_return_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_return_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity NUMERIC NOT NULL,
    unit_price NUMERIC NOT NULL,
    tax_rate NUMERIC NOT NULL,
    tax_amount NUMERIC NOT NULL,
    cgst_amount NUMERIC NOT NULL,
    sgst_amount NUMERIC NOT NULL,
    igst_amount NUMERIC NOT NULL,
    line_total NUMERIC NOT NULL,
    CONSTRAINT fk_purchase_return_lines_return FOREIGN KEY (purchase_return_id) REFERENCES purchase_returns(id),
    CONSTRAINT fk_purchase_return_lines_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS stock_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adjustment_number VARCHAR(255) NOT NULL UNIQUE,
    adjustment_date DATE NOT NULL,
    warehouse_id UUID NOT NULL,
    item_id UUID NOT NULL,
    quantity NUMERIC NOT NULL,
    reason VARCHAR(255),
    CONSTRAINT fk_stock_adjustment_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_stock_adjustment_item FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS stock_ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    item_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    qty_in NUMERIC NOT NULL,
    qty_out NUMERIC NOT NULL,
    ref_type VARCHAR(64) NOT NULL,
    ref_id UUID NOT NULL,
    CONSTRAINT fk_stock_ledger_item FOREIGN KEY (item_id) REFERENCES items(id),
    CONSTRAINT fk_stock_ledger_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

CREATE TABLE IF NOT EXISTS gl_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    ref_type VARCHAR(64) NOT NULL,
    ref_id UUID NOT NULL,
    party_id UUID,
    description VARCHAR(255),
    CONSTRAINT fk_gl_transaction_party FOREIGN KEY (party_id) REFERENCES parties(id)
);

CREATE TABLE IF NOT EXISTS gl_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    account_head VARCHAR(255) NOT NULL,
    debit NUMERIC NOT NULL,
    credit NUMERIC NOT NULL,
    CONSTRAINT fk_gl_line_transaction FOREIGN KEY (transaction_id) REFERENCES gl_transactions(id)
);

CREATE TABLE IF NOT EXISTS organization_profile (
    id BIGINT PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    pincode VARCHAR(32),
    email VARCHAR(255),
    phone VARCHAR(64),
    gstin VARCHAR(64),
    website VARCHAR(255),
    logo_url TEXT
);

CREATE TABLE IF NOT EXISTS migration_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_system VARCHAR(255) NOT NULL,
    source_reference VARCHAR(1024),
    dry_run BOOLEAN NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    requested_by VARCHAR(255),
    scope_source_id_min BIGINT,
    scope_source_id_max BIGINT,
    scope_limit INTEGER
);

CREATE TABLE IF NOT EXISTS migration_stage_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL,
    stage_key VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    stats_json TEXT,
    error_message TEXT,
    CONSTRAINT uk_migration_stage_exec_run_stage UNIQUE (run_id, stage_key),
    CONSTRAINT fk_migration_stage_exec_run FOREIGN KEY (run_id) REFERENCES migration_runs(id)
);

CREATE TABLE IF NOT EXISTS migration_log_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL,
    stage_key VARCHAR(64),
    level VARCHAR(16) NOT NULL,
    message TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_migration_log_run FOREIGN KEY (run_id) REFERENCES migration_runs(id)
);

CREATE TABLE IF NOT EXISTS migration_id_map (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    source_id BIGINT NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_migration_id_map_source UNIQUE (source_system, entity_type, source_id),
    CONSTRAINT uk_migration_id_map_target UNIQUE (entity_type, target_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_sequences_type_wh_fy
    ON document_sequences (sequence_type, warehouse_id, financial_year);
