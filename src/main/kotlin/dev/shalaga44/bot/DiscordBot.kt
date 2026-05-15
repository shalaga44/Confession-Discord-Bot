package dev.shalaga44.bot

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents

class DiscordBot(
    private val kord: Kord,
    private val commandRegistrar: CommandRegistrar,
    private val eventRegistrar: EventRegistrar
) {

    suspend fun start(intents: List<Intent>) {
        commandRegistrar.registerGlobalCommands()
        eventRegistrar.register()

        kord.login {
            this.intents = Intents(intents)
        }
    }
}