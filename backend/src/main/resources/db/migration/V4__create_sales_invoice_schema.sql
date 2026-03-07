CREATE TABLE sales_invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL,
    party_id UUID NOT NULL REFERENCES parties(id),
    total_taxable_amount DECIMAL(19, 2) NOT NULL,
    total_tax_amount DECIMAL(19, 2) NOT NULL,
    grand_total DECIMAL(19, 2) NOT NULL
);

CREATE TABLE sales_invoice_lines (
    id UUID PRIMARY KEY,
    sales_invoice_id UUID NOT NULL REFERENCES sales_invoices(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES items(id),
    quantity DECIMAL(19, 2) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    tax_rate DECIMAL(19, 2) NOT NULL,
    tax_amount DECIMAL(19, 2) NOT NULL,
    line_total DECIMAL(19, 2) NOT NULL
);

CREATE INDEX idx_sales_invoice_date ON sales_invoices(invoice_date);
CREATE INDEX idx_sales_invoice_party ON sales_invoices(party_id);
