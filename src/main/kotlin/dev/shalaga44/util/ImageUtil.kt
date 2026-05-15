package dev.shalaga44.util

object ImageUtil {

    private val allowedMimeTypes = setOf(
        "image/png",
        "image/jpeg",
        "image/webp",
        "image/gif"
    )

    fun isSupportedMimeType(contentType: String?): Boolean {
        return contentType != null && allowedMimeTypes.contains(contentType)
    }

    fun spoilerFilename(
        prefix: String,
        id: Int,
        imageUrl: String
    ): String {
        val extension =
            imageUrl.substringAfterLast(".", "png")
                .substringBefore("?")
                .lowercase()

        return "SPOILER_${prefix}-${id}.$extension"
    }
}