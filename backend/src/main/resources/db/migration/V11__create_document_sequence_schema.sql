CREATE TABLE document_sequences (
    id UUID PRIMARY KEY,
    sequence_type VARCHAR(50) NOT NULL,
    warehouse_id UUID NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    current_value BIGINT NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    suffix VARCHAR(20),
    CONSTRAINT uk_sequence_type_warehouse_fy UNIQUE (sequence_type, warehouse_id, financial_year),
    CONSTRAINT fk_document_sequence_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);
