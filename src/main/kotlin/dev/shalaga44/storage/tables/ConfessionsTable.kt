package dev.shalaga44.storage.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object ConfessionsTable : IntIdTable("confessions") {
    val guildId = long("guild_id")
    val publicConfessionId = integer("public_confession_id")
    val channelId = long("channel_id")
    val authorId = long("author_id")
    val content = text("content")
    val imageUrl = text("image_url").nullable()
    val parentConfessionId = integer("parent_confession_id").nullable()
    val messageId = long("message_id")
    val createdAt = long("created_at")
    val deleted = bool("deleted").default(false)

    init {
        index(
            isUnique = true,
            columns = arrayOf(guildId, publicConfessionId)
        )

        index(
            isUnique = true,
            columns = arrayOf(guildId, messageId)
        )
    }
}