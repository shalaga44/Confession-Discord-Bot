package dev.shalaga44.embeds

import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder

object HelpEmbedFactory {

    fun buildUserHelpEmbed(): EmbedBuilder.() -> Unit = {
        title = "Confessions Bot Help"
        description =
            "Anonymous confessions, replies, moderation, and image spidering."

        color = Color(0x5865F2)

        field {
            name = "/confess"
            value =
                "Submit an anonymous confession with optional image attachment."
        }

        field {
            name = "/reply"
            value =
                "Reply anonymously to an existing confession."
        }


        field {
            name = "/report"
            value =
                "Report a confession that violates Discord ToS or server rules."
        }

        footer {
            text = "Use /help admin for setup instructions."
        }
    }

    fun buildAdminHelpEmbed(): EmbedBuilder.() -> Unit = {
        title = "Confessions Bot Admin Setup"
        description =
            "Configuration and moderation reference for server administrators."

        color = Color(0xED4245)

        field {
            name = "Initial Setup"
            value =
                """
                1. Create a confession channel
                2. Create a moderator log channel
                3. Configure bot permissions
                4. Enable slash commands
                """.trimIndent()
        }

        field {
            name = "/config channel"
            value =
                "Assign the confession destination channel."
        }

        field {
            name = "/config logging"
            value =
                "Assign the moderator audit/logging channel."
        }

        field {
            name = "/config review"
            value =
                "Enable or disable confession review mode."
        }

        field {
            name = "/config addmodrole"
            value =
                "Grant bot administration access to a selected role."
        }

        field {
            name = "/config removemodrole"
            value =
                "Remove bot administration access from a selected role."
        }

        field {
            name = "/config listmodroles"
            value =
                "Display all configured moderator roles."
        }

        field {
            name = "Recommended Channels"
            value =
                """
                #confessions
                #confession-logs
                """.trimIndent()
        }

        field {
            name = "Security Notes"
            value =
                """
                • Keep moderator logs private
                • Do not expose author IDs
                • Restrict bot role permissions
                """.trimIndent()
        }
    }
}