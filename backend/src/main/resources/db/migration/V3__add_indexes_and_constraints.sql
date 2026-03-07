-- V3__add_indexes_and_constraints.sql

-- Add Indexes for Performance
CREATE INDEX idx_parties_name ON parties(name);
CREATE INDEX idx_parties_gstin ON parties(gstin); -- Even though unique, good for searching
CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_hsn ON items(hsn_code);

-- Add Check Constraints for Data Integrity
ALTER TABLE items ADD CONSTRAINT check_unit_price_positive CHECK (unit_price >= 0);
ALTER TABLE items ADD CONSTRAINT check_tax_rate_positive CHECK (tax_rate >= 0);
ALTER TABLE tax_slabs ADD CONSTRAINT check_tax_slab_rate_positive CHECK (rate >= 0);
ALTER TABLE parties ADD CONSTRAINT check_party_type_valid CHECK (type IN ('CUSTOMER', 'VENDOR'));
