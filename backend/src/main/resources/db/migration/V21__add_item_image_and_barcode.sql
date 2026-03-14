ALTER TABLE items ADD COLUMN image_url VARCHAR(512);
ALTER TABLE items ADD COLUMN barcode VARCHAR(64);
CREATE UNIQUE INDEX idx_items_barcode ON items(barcode);
