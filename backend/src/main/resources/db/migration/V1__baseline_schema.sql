CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    replaced_by_token_id UUID,
    user_agent VARCHAR(512),
    ip_address VARCHAR(128),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_hash VARCHAR(255);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS family_id UUID;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by_token_id UUID;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS ip_address VARCHAR(128);

CREATE TABLE IF NOT EXISTS articles (
    id UUID PRIMARY KEY,
    user_id UUID,
    url VARCHAR(2048) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    thumbnail_url VARCHAR(2048),
    status VARCHAR(255) NOT NULL,
    read_date DATE,
    favorite BOOLEAN NOT NULL DEFAULT FALSE,
    rating INTEGER,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE articles ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE articles ADD COLUMN IF NOT EXISTS summary TEXT;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(2048);
ALTER TABLE articles ADD COLUMN IF NOT EXISTS status VARCHAR(255);
ALTER TABLE articles ADD COLUMN IF NOT EXISTS read_date DATE;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS favorite BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS rating INTEGER;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS tags (
    id UUID PRIMARY KEY,
    user_id UUID,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE tags ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS article_tags (
    article_id UUID NOT NULL,
    tag_id UUID NOT NULL
);

ALTER TABLE article_tags ADD COLUMN IF NOT EXISTS user_id UUID;

DO $$
DECLARE
    fallback_owner UUID;
BEGIN
    SELECT id
    INTO fallback_owner
    FROM users
    ORDER BY created_at NULLS LAST, id
    LIMIT 1;

    IF fallback_owner IS NOT NULL THEN
        UPDATE tags
        SET user_id = fallback_owner
        WHERE user_id IS NULL;

        UPDATE articles
        SET user_id = fallback_owner
        WHERE user_id IS NULL;
    END IF;
END $$;

UPDATE article_tags AS article_tag
SET user_id = articles.user_id
FROM articles
WHERE article_tag.article_id = articles.id
  AND article_tag.user_id IS NULL;

DELETE FROM article_tags AS article_tag
USING articles, tags
WHERE article_tag.article_id = articles.id
  AND article_tag.tag_id = tags.id
  AND (
      article_tag.user_id IS NULL
      OR articles.user_id IS NULL
      OR tags.user_id IS NULL
      OR article_tag.user_id <> articles.user_id
      OR article_tag.user_id <> tags.user_id
  );

DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints AS tc
        JOIN information_schema.constraint_column_usage AS ccu
          ON tc.constraint_name = ccu.constraint_name
         AND tc.table_schema = ccu.table_schema
        WHERE tc.table_schema = current_schema()
          AND tc.table_name = 'articles'
          AND tc.constraint_type = 'UNIQUE'
          AND ccu.column_name = 'url'
          AND tc.constraint_name <> 'uk_articles_user_url'
    LOOP
        EXECUTE format('ALTER TABLE articles DROP CONSTRAINT %I', constraint_record.constraint_name);
    END LOOP;

    FOR constraint_record IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints AS tc
        JOIN information_schema.constraint_column_usage AS ccu
          ON tc.constraint_name = ccu.constraint_name
         AND tc.table_schema = ccu.table_schema
        WHERE tc.table_schema = current_schema()
          AND tc.table_name = 'tags'
          AND tc.constraint_type = 'UNIQUE'
          AND ccu.column_name = 'name'
          AND tc.constraint_name <> 'uk_tags_user_name'
    LOOP
        EXECUTE format('ALTER TABLE tags DROP CONSTRAINT %I', constraint_record.constraint_name);
    END LOOP;

    FOR constraint_record IN
        SELECT constraint_name
        FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'article_tags'
          AND constraint_type = 'FOREIGN KEY'
    LOOP
        EXECUTE format('ALTER TABLE article_tags DROP CONSTRAINT %I', constraint_record.constraint_name);
    END LOOP;
END $$;

ALTER TABLE article_tags DROP CONSTRAINT IF EXISTS article_tags_pkey;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_articles_user'
    ) THEN
        ALTER TABLE articles
            ADD CONSTRAINT fk_articles_user FOREIGN KEY (user_id) REFERENCES users (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_tags_user'
    ) THEN
        ALTER TABLE tags
            ADD CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_refresh_tokens_user'
    ) THEN
        ALTER TABLE refresh_tokens
            ADD CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_articles_user_url'
    ) THEN
        ALTER TABLE articles
            ADD CONSTRAINT uk_articles_user_url UNIQUE (user_id, url);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_articles_user_id'
    ) THEN
        ALTER TABLE articles
            ADD CONSTRAINT uk_articles_user_id UNIQUE (user_id, id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_tags_user_name'
    ) THEN
        ALTER TABLE tags
            ADD CONSTRAINT uk_tags_user_name UNIQUE (user_id, name);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_tags_user_id'
    ) THEN
        ALTER TABLE tags
            ADD CONSTRAINT uk_tags_user_id UNIQUE (user_id, id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'article_tags_pkey'
    ) THEN
        ALTER TABLE article_tags
            ADD CONSTRAINT article_tags_pkey PRIMARY KEY (user_id, article_id, tag_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_article_tags_article'
    ) THEN
        ALTER TABLE article_tags
            ADD CONSTRAINT fk_article_tags_article FOREIGN KEY (user_id, article_id) REFERENCES articles (user_id, id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_article_tags_tag'
    ) THEN
        ALTER TABLE article_tags
            ADD CONSTRAINT fk_article_tags_tag FOREIGN KEY (user_id, tag_id) REFERENCES tags (user_id, id) ON DELETE CASCADE;
    END IF;
END $$;

ALTER TABLE articles ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tags ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE article_tags ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_articles_user_status ON articles (user_id, status);
CREATE INDEX IF NOT EXISTS idx_articles_user_favorite ON articles (user_id, favorite);
CREATE INDEX IF NOT EXISTS idx_articles_user_created_at ON articles (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_tags_user_name ON tags (user_id, name);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_family ON refresh_tokens (user_id, family_id);
