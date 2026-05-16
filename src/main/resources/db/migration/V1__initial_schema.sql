CREATE TABLE IF NOT EXISTS guild_settings (
    id INTEGER PRIMARY KEY,
    confession_channel_id INTEGER NOT NULL,
    review_enabled INTEGER NOT NULL DEFAULT 1,
    logging_channel_id INTEGER NULL,
    moderator_role_ids TEXT NOT NULL DEFAULT '',
    next_confession_id INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS confessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id INTEGER NOT NULL,
    public_confession_id INTEGER NOT NULL,
    channel_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    content TEXT NOT NULL,
    image_url TEXT NULL,
    parent_confession_id INTEGER NULL,
    message_id INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_confessions_guild_public
ON confessions(guild_id, public_confession_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_confessions_guild_message
ON confessions(guild_id, message_id);