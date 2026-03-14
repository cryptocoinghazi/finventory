-- Promote 'demo' user to ADMIN role so it can manage users
UPDATE users 
SET role = 'ADMIN' 
WHERE username = 'demo';

-- Note: The default 'admin' user password is 'admin123' (hash: $2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica)
-- If you want to reset 'admin' password to 'admin', you would need a valid BCrypt hash for 'admin'.
