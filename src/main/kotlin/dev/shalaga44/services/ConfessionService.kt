package dev.shalaga44.services

import dev.shalaga44.models.Confession
import dev.shalaga44.storage.tables.ConfessionsTable
import dev.shalaga44.storage.tables.GuildSettingsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ConfessionService {

    fun createConfession(
        guildId: Long,
        channelId: Long,
        authorId: Long,
        content: String,
        imageUrl: String?,
        parentConfessionId: Int? = null,
        messageId: Long
    ): Confession {
        return transaction {
            val currentCounter =
                GuildSettingsTable
                    .select(GuildSettingsTable.nextConfessionId)
                    .where {
                        GuildSettingsTable.id eq guildId
                    }
                    .firstOrNull()
                    ?.get(GuildSettingsTable.nextConfessionId)
                    ?: error(
                        "Guild is not configured. Run /config channel first."
                    )

            GuildSettingsTable.update({
                GuildSettingsTable.id eq guildId
            }) {
                it[nextConfessionId] = currentCounter + 1
            }

            val createdAt = System.currentTimeMillis()

            val persistedMessageId =
                if (messageId == 0L) {
                    -createdAt
                } else {
                    messageId
                }

            ConfessionsTable.insert {
                it[ConfessionsTable.guildId] = guildId
                it[publicConfessionId] = currentCounter
                it[ConfessionsTable.channelId] = channelId
                it[ConfessionsTable.authorId] = authorId
                it[ConfessionsTable.content] = content
                it[ConfessionsTable.imageUrl] = imageUrl
                it[ConfessionsTable.parentConfessionId] = parentConfessionId
                it[ConfessionsTable.messageId] = persistedMessageId
                it[ConfessionsTable.createdAt] = createdAt
            }

            getConfession(
                guildId = guildId,
                publicConfessionId = currentCounter
            )
                ?: error("Failed to create confession")
        }
    }

    fun getConfession(
        guildId: Long,
        publicConfessionId: Int
    ): Confession? {
        return transaction {
            ConfessionsTable
                .selectAll()
                .where {
                    (ConfessionsTable.guildId eq guildId) and
                        (ConfessionsTable.publicConfessionId eq publicConfessionId)
                }
                .firstOrNull()
                ?.toConfession()
        }
    }

    fun getConfessionByMessageId(
        guildId: Long,
        messageId: Long
    ): Confession? {
        return transaction {
            ConfessionsTable
                .selectAll()
                .where {
                    (ConfessionsTable.guildId eq guildId) and
                        (ConfessionsTable.messageId eq messageId)
                }
                .firstOrNull()
                ?.toConfession()
        }
    }

    fun getConfessionById(
        confessionId: Int
    ): Confession? {
        return transaction {
            ConfessionsTable
                .selectAll()
                .where {
                    ConfessionsTable.id eq confessionId
                }
                .firstOrNull()
                ?.toConfession()
        }
    }

    fun updateMessageId(
        confessionId: Int,
        messageId: Long
    ) {
        transaction {
            ConfessionsTable.update({
                ConfessionsTable.id eq confessionId
            }) {
                it[ConfessionsTable.messageId] = messageId
            }
        }
    }

    fun recent(limit: Int = 20): List<Confession> {
        return transaction {
            ConfessionsTable
                .selectAll()
                .orderBy(ConfessionsTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map {
                    it.toConfession()
                }
        }
    }

    private fun ResultRow.toConfession(): Confession {
        return Confession(
            id = this[ConfessionsTable.id].value,
            publicConfessionId = this[ConfessionsTable.publicConfessionId],
            guildId = this[ConfessionsTable.guildId],
            channelId = this[ConfessionsTable.channelId],
            authorId = this[ConfessionsTable.authorId],
            content = this[ConfessionsTable.content],
            imageUrl = this[ConfessionsTable.imageUrl],
            parentConfessionId = this[ConfessionsTable.parentConfessionId],
            messageId = this[ConfessionsTable.messageId],
            createdAt = this[ConfessionsTable.createdAt],
            deleted = this[ConfessionsTable.deleted]
        )
    }
}