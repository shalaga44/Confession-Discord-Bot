package dev.shalaga44.services

import dev.shalaga44.models.Confession

class ReplyService(
    private val confessionService: ConfessionService = ConfessionService()
) {

    fun buildReply(
        confessionId: Int,
        content: String
    ): Pair<Confession, String> {
        val confession = confessionService.getConfession(confessionId)
            ?: error("Confession not found")

        return confession to content.trim()
    }
}