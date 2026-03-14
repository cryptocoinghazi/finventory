ALTER TABLE sales_invoices
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE purchase_invoices
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

