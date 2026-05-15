package dev.shalaga44.embeds

import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder

object ConfessionEmbedFactory {

    fun buildConfessionEmbed(
        confessionId: Int,
        content: String,
        authorId: Long,
        titleText: String = "Anonymous Confession (#$confessionId)",
        embedColor: Color? = null
    ): EmbedBuilder.() -> Unit = {
        title = titleText
        description = content
        color = embedColor ?: Color(
            ((authorId shr 16) and 0xFF).toInt(),
            ((authorId shr 8) and 0xFF).toInt(),
            (authorId and 0xFF).toInt()
        )
    }

    fun buildReplyEmbed(
        confessionId: Int,
        content: String,
        authorId: Long,
        titleText: String = "Anonymous Reply (#$confessionId)",
        embedColor: Color? = null
    ): EmbedBuilder.() -> Unit = {
        title = titleText
        description = content
        color = embedColor ?: Color(
            ((authorId shr 16) and 0xFF).toInt(),
            ((authorId shr 8) and 0xFF).toInt(),
            (authorId and 0xFF).toInt()
        )
    }

    fun buildModerationEmbed(
        reason: String
    ): EmbedBuilder.() -> Unit = {
        title = "Moderation Action"
        description = reason
        color = Color(0xED4245)
    }

}