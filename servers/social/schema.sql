-- Nuvio Pro social server (Cloudflare D1). Idempotent: safe to apply again.

-- Verified account tokens, keyed by SHA-256 of the token (the token itself is never stored).
CREATE TABLE IF NOT EXISTS sessions (
    token_hash TEXT PRIMARY KEY,
    account TEXT NOT NULL,
    expires_at INTEGER NOT NULL
);

-- One row per Nuvio profile: "nuvio:<account>" for profile 1, "nuvio:<account>:<n>" otherwise.
CREATE TABLE IF NOT EXISTS people (
    id TEXT PRIMARY KEY,
    account TEXT NOT NULL,
    profile INTEGER NOT NULL,
    name TEXT NOT NULL DEFAULT '',
    avatar TEXT,
    friend_code TEXT NOT NULL UNIQUE,
    -- 0: activity hidden from everyone, 1: friends see it.
    sharing INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS friend_requests (
    from_id TEXT NOT NULL,
    to_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (from_id, to_id)
);
CREATE INDEX IF NOT EXISTS friend_requests_to ON friend_requests (to_id);

-- Stored both ways (a→b and b→a) so "my friends" is a single indexed lookup.
CREATE TABLE IF NOT EXISTS friendships (
    person_id TEXT NOT NULL,
    friend_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (person_id, friend_id)
);

CREATE TABLE IF NOT EXISTS recommendations (
    id TEXT PRIMARY KEY,
    from_id TEXT NOT NULL,
    to_id TEXT NOT NULL,
    content_type TEXT NOT NULL,
    content_id TEXT NOT NULL,
    title TEXT NOT NULL,
    poster TEXT,
    note TEXT,
    reaction TEXT,
    reply TEXT,
    created_at INTEGER NOT NULL,
    replied_at INTEGER,
    seen_at INTEGER
);
CREATE INDEX IF NOT EXISTS recommendations_to ON recommendations (to_id, created_at DESC);
CREATE INDEX IF NOT EXISTS recommendations_from ON recommendations (from_id, created_at DESC);

-- What each person watched: a "watching" row is refreshed by playback heartbeats and turns
-- into "finished" when the title ends.
CREATE TABLE IF NOT EXISTS activity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id TEXT NOT NULL,
    kind TEXT NOT NULL,
    content_type TEXT NOT NULL,
    content_id TEXT NOT NULL,
    title TEXT NOT NULL,
    poster TEXT,
    season INTEGER,
    episode INTEGER,
    episode_title TEXT,
    progress REAL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS activity_person ON activity (person_id, updated_at DESC);

-- In-app notices: friend requests, accepted requests, recommendations and replies.
CREATE TABLE IF NOT EXISTS notifications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    person_id TEXT NOT NULL,
    kind TEXT NOT NULL,
    actor_id TEXT NOT NULL,
    content_type TEXT,
    content_id TEXT,
    title TEXT,
    poster TEXT,
    text TEXT,
    created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS notifications_person ON notifications (person_id, id DESC);
