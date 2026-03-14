ALTER TABLE migration_runs
    ADD COLUMN scope_source_id_min BIGINT;

ALTER TABLE migration_runs
    ADD COLUMN scope_source_id_max BIGINT;

ALTER TABLE migration_runs
    ADD COLUMN scope_limit INTEGER;

