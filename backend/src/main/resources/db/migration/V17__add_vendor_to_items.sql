ALTER TABLE items ADD COLUMN vendor_id UUID;
ALTER TABLE items ADD CONSTRAINT fk_items_vendor FOREIGN KEY (vendor_id) REFERENCES parties(id);
