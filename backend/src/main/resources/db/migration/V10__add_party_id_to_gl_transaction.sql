ALTER TABLE gl_transactions ADD COLUMN party_id UUID REFERENCES parties(id);
