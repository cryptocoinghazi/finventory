CREATE TABLE IF NOT EXISTS label_print_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status VARCHAR(32) NOT NULL,
    template_name VARCHAR(64) NOT NULL,
    barcode_format VARCHAR(32) NOT NULL,
    total_labels INT NOT NULL,
    details_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_label_print_jobs_created_at ON label_print_jobs(created_at);
CREATE INDEX IF NOT EXISTS idx_label_print_jobs_user_id ON label_print_jobs(user_id);
