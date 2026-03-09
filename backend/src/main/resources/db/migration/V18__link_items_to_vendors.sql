-- Ensure at least one vendor exists
DO $$
DECLARE
    default_vendor_id UUID;
BEGIN
    -- Check if any vendor exists
    SELECT id INTO default_vendor_id FROM parties WHERE type = 'VENDOR' LIMIT 1;

    -- If no vendor exists, create a default 'General Vendor'
    IF default_vendor_id IS NULL THEN
        default_vendor_id := gen_random_uuid();
        INSERT INTO parties (id, name, type, gstin, state_code, address, phone, email)
        VALUES (default_vendor_id, 'General Vendor', 'VENDOR', NULL, NULL, 'Default Address', NULL, NULL);
    END IF;

    -- Update all items with NULL vendor_id to use the found/created vendor ID
    UPDATE items SET vendor_id = default_vendor_id WHERE vendor_id IS NULL;

    -- Enforce NOT NULL constraint on vendor_id
    ALTER TABLE items ALTER COLUMN vendor_id SET NOT NULL;
END $$;
