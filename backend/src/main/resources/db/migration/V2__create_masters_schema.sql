-- V2__create_masters_schema.sql

CREATE TABLE parties (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    gstin VARCHAR(50) UNIQUE,
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(100)
);

CREATE TABLE items (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    hsn_code VARCHAR(50),
    tax_rate DECIMAL(19, 2) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    uom VARCHAR(50) NOT NULL
);

CREATE TABLE tax_slabs (
    id UUID PRIMARY KEY,
    rate DECIMAL(19, 2) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE warehouses (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    location TEXT
);

-- Seed initial Tax Slabs
INSERT INTO tax_slabs (id, rate, description) VALUES
    (gen_random_uuid(), 0.00, 'GST 0%'),
    (gen_random_uuid(), 5.00, 'GST 5%'),
    (gen_random_uuid(), 12.00, 'GST 12%'),
    (gen_random_uuid(), 18.00, 'GST 18%'),
    (gen_random_uuid(), 28.00, 'GST 28%');
