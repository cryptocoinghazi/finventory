CREATE TABLE purchase_invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(255) NOT NULL UNIQUE,
    vendor_invoice_number VARCHAR(255),
    invoice_date DATE NOT NULL,
    party_id UUID NOT NULL REFERENCES parties(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    total_taxable_amount DECIMAL(19, 2) NOT NULL,
    total_tax_amount DECIMAL(19, 2) NOT NULL,
    total_cgst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_sgst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_igst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(19, 2) NOT NULL
);

CREATE TABLE purchase_invoice_lines (
    id UUID PRIMARY KEY,
    purchase_invoice_id UUID NOT NULL REFERENCES purchase_invoices(id),
    item_id UUID NOT NULL REFERENCES items(id),
    quantity DECIMAL(19, 2) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    tax_rate DECIMAL(19, 2) NOT NULL,
    tax_amount DECIMAL(19, 2) NOT NULL,
    cgst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    sgst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    igst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(19, 2) NOT NULL
);
