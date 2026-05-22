package dev.shalaga44.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import kotlin.system.exitProcess

class CrashReporter(
    private val kord: Kord,
    private val ownerId: Long
) {

    fun coroutineExceptionHandler(): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            runBlockingReport(
                context =
                    """
                    Global coroutine exception
                    """.trimIndent(),
                throwable = throwable
            )
        }
    }

    fun installJvmHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runBlockingReport(
                context =
                    """
                    Uncaught JVM exception
                    Thread: ${thread.name}
                    """.trimIndent(),
                throwable = throwable
            )

            throwable.printStackTrace()
            exitProcess(1)
        }
    }

    suspend fun report(
        context: String,
        throwable: Throwable
    ) {
        sendCrashReport(
            context = context,
            throwable = throwable
        )
    }

    private fun runBlockingReport(
        context: String,
        throwable: Throwable
    ) {
        persistCrashLocally(
            context = context,
            throwable = throwable
        )

        runCatching {
            kotlinx.coroutines.runBlocking {
                sendCrashReport(
                    context = context,
                    throwable = throwable
                )
            }
        }.onFailure {
            println(
                "Crash reporter failed: ${it.message}"
            )

            it.printStackTrace()
        }
    }

    private suspend fun sendCrashReport(
        context: String,
        throwable: Throwable
    ) {
        runCatching {
            val owner =
                kord.getUser(Snowflake(ownerId))
                    ?: return

            val stackTrace =
                StringWriter().also { writer ->
                    throwable.printStackTrace(
                        PrintWriter(writer)
                    )
                }.toString()

            val recentLogs =
                readRecentLogs()
                    .take(3000)

            val message =
                buildString {
                    appendLine("Bot crash detected")
                    appendLine()

                    appendLine("Context:")
                    appendLine("```")
                    appendLine(context.take(1000))
                    appendLine("```")
                    appendLine()

                    appendLine("Error:")
                    appendLine("```")
                    appendLine(
                        throwable.message
                            ?: throwable::class.simpleName
                            ?: "Unknown error"
                    )
                    appendLine("```")
                    appendLine()

                    appendLine("Recent Logs:")
                    appendLine("```")
                    appendLine(recentLogs)
                    appendLine("```")
                    appendLine()

                    appendLine("Stack Trace:")
                    appendLine("```")
                    appendLine(
                        stackTrace.take(3500)
                    )
                    appendLine("```")
                }

            owner.getDmChannel()
                .createMessage(
                    content = message.take(1900)
                )
        }.onFailure {
            println(
                "Failed to send crash report: ${it.message}"
            )

            it.printStackTrace()
        }
    }

    private fun persistCrashLocally(
        context: String,
        throwable: Throwable
    ) {
        runCatching {
            val crashDirectory =
                File("logs/crashes")

            crashDirectory.mkdirs()

            val timestamp =
                Instant.now()
                    .toEpochMilli()

            val crashFile =
                File(
                    crashDirectory,
                    "crash-$timestamp.log"
                )

            val stackTrace =
                StringWriter().also { writer ->
                    throwable.printStackTrace(
                        PrintWriter(writer)
                    )
                }.toString()

            crashFile.writeText(
                buildString {
                    appendLine("Timestamp: ${Instant.now()}")
                    appendLine()

                    appendLine("Context:")
                    appendLine(context)
                    appendLine()

                    appendLine("Error:")
                    appendLine(
                        throwable.message
                            ?: throwable::class.simpleName
                            ?: "Unknown error"
                    )

                    appendLine()

                    appendLine("Recent Logs:")
                    appendLine(readRecentLogs())

                    appendLine()

                    appendLine("Stack Trace:")
                    appendLine(stackTrace)
                }
            )
        }.onFailure {
            println(
                "Failed to persist crash log: ${it.message}"
            )

            it.printStackTrace()
        }
    }

    private fun readRecentLogs(): String {
        return runCatching {
            val logFile = File("logs/bot.log")

            if (!logFile.exists()) {
                return "No log file found."
            }

            logFile
                .readLines()
                .takeLast(40)
                .joinToString("\n")
        }.getOrElse {
            "Failed to read logs: ${it.message}"
        }
    }
}