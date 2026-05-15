package dev.shalaga44.models

data class GuildSettings(
    val guildId: Long,
    val confessionChannelId: Long,
    val reviewEnabled: Boolean,
    val loggingChannelId: Long?,
    val moderatorRoleIds: List<Long>
)