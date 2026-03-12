CREATE TABLE IF NOT EXISTS offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64),
    discount_type VARCHAR(16) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    discount_value NUMERIC(12, 2) NOT NULL,
    item_id UUID REFERENCES items(id),
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    usage_limit INT,
    used_count INT NOT NULL DEFAULT 0,
    min_bill_amount NUMERIC(12, 2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_offers_code ON offers(LOWER(code));
CREATE INDEX IF NOT EXISTS idx_offers_active ON offers(active);
CREATE INDEX IF NOT EXISTS idx_offers_scope ON offers(scope);
