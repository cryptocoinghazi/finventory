-- Create organization_profile table
CREATE TABLE organization_profile (
    id BIGINT PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    email VARCHAR(100),
    phone VARCHAR(20),
    gstin VARCHAR(20),
    website VARCHAR(255),
    logo_url TEXT
);

-- Seed default organization data
INSERT INTO organization_profile (
    id, company_name, address_line1, city, state, pincode, email, phone, gstin
) VALUES (
    1, 
    'Finventory Demo Company', 
    '123 Business Park, Tech Hub', 
    'Bangalore', 
    'Karnataka', 
    '560001', 
    'contact@finventory.com', 
    '+91 9876543210',
    '29AAAAA0000A1Z5'
);
