CREATE TABLE IF NOT EXISTS migration_stage_executions (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    stage_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE,
    stats_json TEXT,
    error_message TEXT,
    CONSTRAINT fk_migration_stage_executions_run_id
        FOREIGN KEY (run_id) REFERENCES migration_runs(id) ON DELETE CASCADE,
    CONSTRAINT uq_migration_stage_executions_run_id_stage_key
        UNIQUE (run_id, stage_key)
);

CREATE TABLE IF NOT EXISTS migration_log_entries (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL,
    stage_key VARCHAR(100),
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_migration_log_entries_run_id
        FOREIGN KEY (run_id) REFERENCES migration_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_migration_log_entries_run_id_created_at
    ON migration_log_entries(run_id, created_at);
