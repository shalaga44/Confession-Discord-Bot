package dev.shalaga44.services

import dev.shalaga44.models.Confession
import dev.shalaga44.storage.tables.ConfessionsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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
            val statement = ConfessionsTable.insert {
                it[ConfessionsTable.guildId] = guildId
                it[ConfessionsTable.channelId] = channelId
                it[ConfessionsTable.authorId] = authorId
                it[ConfessionsTable.content] = content
                it[ConfessionsTable.imageUrl] = imageUrl
                it[ConfessionsTable.parentConfessionId] = parentConfessionId
                it[ConfessionsTable.messageId] = messageId
                it[ConfessionsTable.createdAt] = System.currentTimeMillis()
            }

            val id = statement[ConfessionsTable.id].value

            getConfession(id)
                ?: error("Failed to create confession")
        }
    }

    fun getConfession(id: Int): Confession? {
        return transaction {
            ConfessionsTable
                .selectAll()
                .where {
                    ConfessionsTable.id eq id
                }
                .firstOrNull()
                ?.toConfession()
        }
    }

    fun getConfessionByMessageId(
        messageId: Long
    ): Confession? {
        return transaction {
            ConfessionsTable
                .selectAll()
                .where {
                    ConfessionsTable.messageId eq messageId
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