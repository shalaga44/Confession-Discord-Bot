package dev.shalaga44.moderation

data class ModerationResult(
    val accepted: Boolean,
    val reason: String? = null
)

object ContentScanner {

    private val blockedPatterns = listOf(
        "discord.gg/",
        "@everyone",
        "@here"
    )

    fun validate(content: String): ModerationResult {
        val normalized = content.lowercase()

        val violation = blockedPatterns.firstOrNull {
            normalized.contains(it)
        }

        return if (violation != null) {
            ModerationResult(
                accepted = false,
                reason = "Blocked pattern detected: $violation"
            )
        } else {
            ModerationResult(
                accepted = true
            )
        }
    }
}