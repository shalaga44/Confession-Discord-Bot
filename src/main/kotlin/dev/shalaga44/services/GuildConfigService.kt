package dev.shalaga44.services

import dev.shalaga44.models.GuildSettings
import dev.shalaga44.storage.tables.GuildSettingsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class GuildConfigService {

    fun configureChannel(
        guildId: Long,
        channelId: Long
    ) {
        transaction {
            ensureGuild(guildId)

            GuildSettingsTable.update({
                GuildSettingsTable.id eq guildId
            }) {
                it[confessionChannelId] = channelId
            }
        }
    }

    fun configureLogging(
        guildId: Long,
        channelId: Long?
    ) {
        transaction {
            ensureGuild(guildId)

            GuildSettingsTable.update({
                GuildSettingsTable.id eq guildId
            }) {
                it[loggingChannelId] = channelId
            }
        }
    }

    fun setReviewMode(
        guildId: Long,
        enabled: Boolean
    ) {
        transaction {
            ensureGuild(guildId)

            GuildSettingsTable.update({
                GuildSettingsTable.id eq guildId
            }) {
                it[reviewEnabled] = enabled
            }
        }
    }

    fun addModeratorRole(
        guildId: Long,
        roleId: Long
    ) {
        transaction {
            ensureGuild(guildId)

            val currentRoles =
                getModeratorRolesInternal(guildId)
                    .toMutableSet()

            currentRoles.add(roleId)

            GuildSettingsTable.update({
                GuildSettingsTable.id eq guildId
            }) {
                it[moderatorRoleIds] =
                    currentRoles.joinToString(",")
            }
        }
    }

    fun removeModeratorRole(
        guildId: Long,
        roleId: Long
    ) {
        transaction {
            ensureGuild(guildId)

            val currentRoles =
                getModeratorRolesInternal(guildId)
                    .toMutableSet()

            currentRoles.remove(roleId)

            GuildSettingsTable.update({
                GuildSettingsTable.id eq guildId
            }) {
                it[moderatorRoleIds] =
                    currentRoles.joinToString(",")
            }
        }
    }

    fun getModeratorRoles(
        guildId: Long
    ): List<Long> {
        return transaction {
            ensureGuild(guildId)
            getModeratorRolesInternal(guildId)
        }
    }

    fun getSettings(guildId: Long): GuildSettings? {
        return transaction {
            GuildSettingsTable
                .selectAll()
                .where {
                    GuildSettingsTable.id eq guildId
                }
                .firstOrNull()
                ?.toSettings()
        }
    }

    private fun ensureGuild(guildId: Long) {
        GuildSettingsTable.insertIgnore {
            it[id] = guildId
            it[confessionChannelId] = guildId
            it[moderatorRoleIds] = ""
        }
    }

    private fun getModeratorRolesInternal(
        guildId: Long
    ): List<Long> {
        val row =
            GuildSettingsTable
                .selectAll()
                .where {
                    GuildSettingsTable.id eq guildId
                }
                .firstOrNull()
                ?: return emptyList()

        return row[GuildSettingsTable.moderatorRoleIds]
            .split(",")
            .mapNotNull {
                it.trim()
                    .takeIf(String::isNotBlank)
                    ?.toLongOrNull()
            }
    }

    private fun ResultRow.toSettings(): GuildSettings {
        return GuildSettings(
            guildId = this[GuildSettingsTable.id].value,
            confessionChannelId = this[GuildSettingsTable.confessionChannelId],
            reviewEnabled = this[GuildSettingsTable.reviewEnabled],
            loggingChannelId = this[GuildSettingsTable.loggingChannelId],
            moderatorRoleIds =
                this[GuildSettingsTable.moderatorRoleIds]
                    .split(",")
                    .mapNotNull {
                        it.trim()
                            .takeIf(String::isNotBlank)
                            ?.toLongOrNull()
                    }
        )
    }
}