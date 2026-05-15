package dev.shalaga44.services

import dev.shalaga44.moderation.ContentScanner
import dev.shalaga44.moderation.ModerationResult
import dev.shalaga44.moderation.SpamDetector

class ModerationService {

    fun moderate(content: String): ModerationResult {
        if (SpamDetector.isSpam(content)) {
            return ModerationResult(
                accepted = false,
                reason = "Message exceeded size limits"
            )
        }

        return ContentScanner.validate(content)
    }
}