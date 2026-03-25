-- Create the schema dbo
CREATE SCHEMA IF NOT EXISTS dbo;

-- Set search path for this script execution
SET search_path TO dbo;

-- Create table for users
CREATE TABLE users
(
    id                  SERIAL PRIMARY KEY,
    username            VARCHAR(30) UNIQUE NOT NULL CHECK (LENGTH(username) BETWEEN 1 AND 30),
    password_validation VARCHAR(255)       NOT NULL
);

-- Create table for channels
CREATE TABLE channels
(
    id        SERIAL PRIMARY KEY,
    name      VARCHAR(30) UNIQUE NOT NULL CHECK (LENGTH(name) BETWEEN 1 AND 30),
    owner_id  INT                NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    is_public BOOLEAN DEFAULT TRUE
);

-- Create table for messages
CREATE TABLE messages
(
    id         SERIAL PRIMARY KEY,
    content    TEXT NOT NULL CHECK (LENGTH(content) BETWEEN 1 AND 1000),
    user_id    INT  REFERENCES users (id) ON DELETE SET NULL,
    channel_id INT  NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Create table for invitations
CREATE TABLE invitations
(
    id          SERIAL PRIMARY KEY,
    token       VARCHAR(100) UNIQUE NOT NULL,
    created_by  INT                 NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel_id  INT                 NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    access_type VARCHAR(20)         NOT NULL CHECK (access_type IN ('READ_ONLY', 'READ_WRITE')),
    expires_at  TIMESTAMPTZ         NOT NULL CHECK (expires_at > CURRENT_TIMESTAMP),
    status      VARCHAR(20)         NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')) DEFAULT 'PENDING'
);

-- Create table for membership
CREATE TABLE channel_members
(
    id          SERIAL PRIMARY KEY,
    user_id     INT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    channel_id  INT         NOT NULL REFERENCES channels (id) ON DELETE CASCADE,
    access_type VARCHAR(20) NOT NULL CHECK (access_type IN ('READ_ONLY', 'READ_WRITE')),
    UNIQUE (user_id, channel_id)
);

-- Create table for the token blacklist
CREATE TABLE token_blacklist
(
    jti        VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Add indexes for performance
CREATE INDEX idx_channels_owner_id ON channels(owner_id);
CREATE INDEX idx_messages_user_id ON messages(user_id);
CREATE INDEX idx_messages_channel_id ON messages(channel_id);
CREATE INDEX idx_invitations_created_by ON invitations(created_by);
CREATE INDEX idx_invitations_channel_id ON invitations(channel_id);
CREATE INDEX idx_channels_name_trgm ON channels USING gin (name gin_trgm_ops);
