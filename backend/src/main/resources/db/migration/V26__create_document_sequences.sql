-- V26__create_document_sequences.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS document_sequences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sequence_type VARCHAR(50) NOT NULL,
    warehouse_id UUID NOT NULL,
    financial_year VARCHAR(20) NOT NULL,
    current_value BIGINT NOT NULL,
    prefix VARCHAR(20) NOT NULL,
    suffix VARCHAR(20),
    CONSTRAINT uk_sequence_type_warehouse_fy UNIQUE (sequence_type, warehouse_id, financial_year)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_sequences_type_wh_fy
    ON document_sequences (sequence_type, warehouse_id, financial_year);
