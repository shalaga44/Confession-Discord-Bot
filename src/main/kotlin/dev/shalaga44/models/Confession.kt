package dev.shalaga44.models

data class Confession(
    val id: Int,
    val publicConfessionId: Int,
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val content: String,
    val imageUrl: String?,
    val parentConfessionId: Int?,
    val messageId: Long,
    val createdAt: Long,
    val deleted: Boolean
)