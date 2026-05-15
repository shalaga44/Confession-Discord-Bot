package dev.shalaga44.storage.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object GuildSettingsTable : LongIdTable("guild_settings") {
    val confessionChannelId = long("confession_channel_id")
    val reviewEnabled = bool("review_enabled").default(true)
    val loggingChannelId = long("logging_channel_id").nullable()
    val moderatorRoleIds = text("moderator_role_ids").default("")
}