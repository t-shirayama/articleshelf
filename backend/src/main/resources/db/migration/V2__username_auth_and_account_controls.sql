ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(32);
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_valid_after TIMESTAMP WITH TIME ZONE;

WITH generated AS (
    SELECT
        id,
        lower(regexp_replace(split_part(coalesce(email, id::text), '@', 1), '[^a-zA-Z0-9._-]', '-', 'g')) AS base_name,
        row_number() OVER (
            PARTITION BY lower(regexp_replace(split_part(coalesce(email, id::text), '@', 1), '[^a-zA-Z0-9._-]', '-', 'g'))
            ORDER BY created_at NULLS LAST, id
        ) AS duplicate_index
    FROM users
    WHERE username IS NULL OR username = ''
)
UPDATE users
SET username = left(
        CASE
            WHEN length(generated.base_name) < 3 THEN 'user-' || replace(users.id::text, '-', '')
            WHEN generated.duplicate_index = 1 THEN generated.base_name
            ELSE left(generated.base_name, 24) || '-' || generated.duplicate_index
        END,
        32
    )
FROM generated
WHERE users.id = generated.id;

UPDATE users
SET token_valid_after = '1970-01-01 00:00:00+00'
WHERE token_valid_after IS NULL;

DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT constraint_name
        FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND constraint_type = 'UNIQUE'
          AND constraint_name <> 'uk_users_username'
    LOOP
        EXECUTE format('ALTER TABLE users DROP CONSTRAINT %I', constraint_record.constraint_name);
    END LOOP;
END $$;

ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ALTER COLUMN token_valid_after SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_users_username'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uk_users_username UNIQUE (username);
    END IF;
END $$;
