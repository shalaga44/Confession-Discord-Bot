package dev.shalaga44.bot

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.rest.builder.interaction.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

class CommandRegistrar(
    private val kord: Kord
) {

    private val cacheFile =
        File("data/command-fingerprint.json")

    suspend fun registerGlobalCommands() {
        val fingerprint = buildFingerprint()

        if (isFingerprintCurrent(fingerprint)) {
            println("Slash commands unchanged. Skipping registration.")
            return
        }

        println("Slash command changes detected. Synchronizing commands.")

        registerConfess()
        registerReply()
        registerReport()
        registerConfig()
        registerHelp()

        persistFingerprint(fingerprint)
    }

    private suspend fun registerConfess() {
        kord.createGlobalChatInputCommand(
            name = "confess",
            description = "Submit an anonymous confession"
        ) {
            string(
                name = "message",
                description = "Confession content"
            ) {
                required = true
            }

            attachment(
                name = "image",
                description = "Optional image attachment"
            ) {
                required = false
            }
        }
    }

    private suspend fun registerReply() {
        kord.createGlobalChatInputCommand(
            name = "reply",
            description = "Reply anonymously to a confession"
        ) {
            integer(
                name = "confession_id",
                description = "Confession identifier"
            ) {
                required = true
            }

            string(
                name = "reply",
                description = "Reply content"
            ) {
                required = true
            }
        }
    }


    private suspend fun registerReport() {
        kord.createGlobalChatInputCommand(
            name = "report",
            description = "Report a confession"
        ) {
            integer(
                name = "confession_id",
                description = "Confession identifier"
            ) {
                required = true
            }
        }
    }

    private suspend fun registerConfig() {
        kord.createGlobalChatInputCommand(
            name = "config",
            description = "Configure confession bot"
        ) {
            defaultMemberPermissions =
                Permissions(Permission.Administrator)

            subCommand(
                name = "channel",
                description = "Configure confession channel"
            )

            subCommand(
                name = "review",
                description = "Toggle review mode"
            )

            subCommand(
                name = "logging",
                description = "Configure logging"
            )

            subCommand(
                name = "addmodrole",
                description = "Grant bot admin access to a role"
            ) {
                role(
                    name = "role",
                    description = "Role to authorize"
                ) {
                    required = true
                }
            }

            subCommand(
                name = "removemodrole",
                description = "Remove bot admin access from a role"
            ) {
                role(
                    name = "role",
                    description = "Role to remove"
                ) {
                    required = true
                }
            }

            subCommand(
                name = "listmodroles",
                description = "List authorized moderator roles"
            )

        }
    }


    private suspend fun registerHelp() {
        kord.createGlobalChatInputCommand(
            name = "help",
            description = "Show bot help and setup instructions"
        ) {
            string(
                name = "section",
                description = "Help category"
            ) {
                required = false

                choice(
                    name = "user",
                    value = "user"
                )

                choice(
                    name = "admin",
                    value = "admin"
                )
            }
        }
    }

    private fun isFingerprintCurrent(
        fingerprint: String
    ): Boolean {
        if (!cacheFile.exists()) {
            return false
        }

        return cacheFile.readText() == fingerprint
    }

    private fun persistFingerprint(
        fingerprint: String
    ) {
        cacheFile.parentFile.mkdirs()
        cacheFile.writeText(fingerprint)
    }
    private fun buildFingerprint(): String {
        return Json.encodeToString(
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("name", "confess")
                        put(
                            "description",
                            "Submit an anonymous confession"
                        )

                        put(
                            "options",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("name", "report")
                                        put(
                                            "description",
                                            "Report a confession"
                                        )
                                    }
                                )

                                add(
                                    buildJsonObject {
                                        put("name", "config")
                                        put(
                                            "description",
                                            "Configure confession bot"
                                        )
                                    }
                                )

                                add(
                                    buildJsonObject {
                                        put("name", "report")
                                        put(
                                            "description",
                                            "Report a confession"
                                        )
                                    }
                                )

                                add(
                                    buildJsonObject {
                                        put("name", "config")
                                        put(
                                            "description",
                                            "Configure confession bot"
                                        )
                                    }
                                )

                                add(
                                    buildJsonObject {
                                        put("name", "confessban")
                                        put(
                                            "description",
                                            "Ban a user from confessing"
                                        )
                                    }
                                )

                                add(
                                    buildJsonObject {
                                        put("name", "help")
                                        put(
                                            "description",
                                            "Show bot help and setup instructions"
                                        )
                                    }
                                )
                            }
                        )
                    })
            })
    }

}