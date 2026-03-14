CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY,
    adjustment_number VARCHAR(255) NOT NULL UNIQUE,
    adjustment_date DATE NOT NULL,
    quantity DECIMAL(19, 2) NOT NULL,
    reason VARCHAR(255),
    warehouse_id UUID NOT NULL,
    item_id UUID NOT NULL,
    CONSTRAINT fk_stock_adjustments_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_stock_adjustments_item FOREIGN KEY (item_id) REFERENCES items(id)
);
