-- +goose Up
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(254)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,  -- bcrypt hash
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_users_email ON users (email);

-- +goose Down
DROP TABLE IF EXISTS users;
