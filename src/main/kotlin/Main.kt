package dev.shalaga44

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.shalaga44.bot.CommandRegistrar
import dev.shalaga44.bot.DiscordBot
import dev.shalaga44.bot.EventRegistrar
import dev.shalaga44.config.ConfigLoader
import dev.shalaga44.storage.DatabaseFactory
import dev.shalaga44.util.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@OptIn(PrivilegedIntent::class)
suspend fun main() {
    val config = ConfigLoader.load()

    DatabaseFactory.initialize(config.databaseUrl)

    val kord = Kord(config.token)

    val crashReporter =
        CrashReporter(
            kord = kord,
            ownerId = config.ownerId.toLong()
        )

    crashReporter.installJvmHandler()

    CoroutineScope(
        SupervisorJob() +
            crashReporter.coroutineExceptionHandler()
    )

    val commandRegistrar = CommandRegistrar(kord)
    val eventRegistrar = EventRegistrar(
        kord = kord,
        ownerId = config.ownerId.toLong()
    )

    val bot = DiscordBot(
        kord = kord,
        commandRegistrar = commandRegistrar,
        eventRegistrar = eventRegistrar
    )

    bot.start(
        intents = listOf(
            Intent.Guilds,
            Intent.GuildMessages,
            Intent.MessageContent
        )
    )
}