package dev.shalaga44.config

import io.github.cdimascio.dotenv.dotenv

object ConfigLoader {

    fun load(): BotConfig {
        val env = dotenv {
            ignoreIfMissing = false
        }

        val token = env["DISCORD_TOKEN"]
            ?: error("DISCORD_TOKEN is missing")

        val ownerId = env["OWNER_ID"]
            ?: error("OWNER_ID is missing")

        val databaseUrl = env["DATABASE_URL"]
            ?: "jdbc:sqlite:data/confessions.db"

        return BotConfig(
            token = token,
            ownerId = ownerId,
            databaseUrl = databaseUrl
        )
    }
}