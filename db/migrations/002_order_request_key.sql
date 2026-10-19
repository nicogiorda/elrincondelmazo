BEGIN;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS request_key uuid;
CREATE UNIQUE INDEX IF NOT EXISTS orders_user_request_uq ON orders (user_id, request_key);
COMMIT;
