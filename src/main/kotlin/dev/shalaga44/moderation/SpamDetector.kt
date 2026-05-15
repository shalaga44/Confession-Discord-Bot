package dev.shalaga44.moderation

object SpamDetector {

    fun isSpam(content: String): Boolean {
        return content.length > 4000
    }
}