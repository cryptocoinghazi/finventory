CREATE TABLE migration_runs (
    id UUID PRIMARY KEY,
    source_system VARCHAR(50) NOT NULL,
    source_reference VARCHAR(255),
    dry_run BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE,
    requested_by VARCHAR(100)
);

CREATE TABLE migration_stage_executions (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES migration_runs(id) ON DELETE CASCADE,
    stage_key VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE,
    stats_json TEXT,
    error_message TEXT,
    UNIQUE(run_id, stage_key)
);

CREATE TABLE migration_log_entries (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES migration_runs(id) ON DELETE CASCADE,
    stage_key VARCHAR(100),
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_migration_log_entries_run_id_created_at
    ON migration_log_entries(run_id, created_at);

CREATE TABLE migration_id_map (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    source_id BIGINT NOT NULL,
    target_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(source_system, entity_type, source_id),
    UNIQUE(entity_type, target_id)
);

CREATE INDEX idx_migration_id_map_target_id ON migration_id_map(target_id);
