CREATE TABLE audit_log_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(255),
    actor_role VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id UUID,
    details TEXT
);

CREATE INDEX idx_audit_log_entries_created_at ON audit_log_entries(created_at);
CREATE INDEX idx_audit_log_entries_action_created_at ON audit_log_entries(action, created_at);
