ALTER TABLE confessions
ADD COLUMN public_confession_id INTEGER;

ALTER TABLE guild_settings
ADD COLUMN next_confession_id INTEGER NOT NULL DEFAULT 1;

UPDATE confessions
SET public_confession_id = id
WHERE public_confession_id IS NULL
   OR public_confession_id = 0;

UPDATE guild_settings
SET next_confession_id = (
    SELECT COALESCE(MAX(public_confession_id), 0) + 1
    FROM confessions
    WHERE confessions.guild_id = guild_settings.id
);

UPDATE confessions
SET message_id = -id
WHERE message_id = 0
   OR message_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_confessions_guild_public
ON confessions(guild_id, public_confession_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_confessions_guild_message
ON confessions(guild_id, message_id);