CREATE TABLE sales_returns (
    id UUID PRIMARY KEY,
    return_number VARCHAR(255) NOT NULL UNIQUE,
    sales_invoice_id UUID REFERENCES sales_invoices(id),
    return_date DATE NOT NULL,
    party_id UUID NOT NULL REFERENCES parties(id),
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    total_taxable_amount DECIMAL(19, 2) NOT NULL,
    total_tax_amount DECIMAL(19, 2) NOT NULL,
    total_cgst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_sgst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_igst_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(19, 2) NOT NULL
);

CREATE TABLE sales_return_lines (
    id UUID PRIMARY KEY,
    sales_return_id UUID NOT NULL REFERENCES sales_returns(id),
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
