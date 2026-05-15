package dev.shalaga44.util

import dev.kord.common.Color
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.effectiveName
import java.security.MessageDigest

fun User.stableHash(): ByteArray {
    val name = (this as? Member)?.effectiveName ?: this.effectiveName
    val numericRepresentation =
        name.fold(0L) { accumulator, character ->
            (accumulator * 31L) + character.code.toLong()
        }

    return MessageDigest.getInstance("SHA-256")
        .digest(numericRepresentation.toString().toByteArray())
}

fun User.generatedColor(): Color {
    val hash = stableHash()

    val red = hash[0].toInt() and 0xFF
    val green = hash[1].toInt() and 0xFF
    val blue = hash[2].toInt() and 0xFF

    return Color(red, green, blue)
}

private val identityEmojiPool = listOf(
    "😈", "👿", "👹", "👺", "💀", "☠️", "👻", "👽",
    "👾", "🤖", "🎃", "😺", "😸", "😹", "😻", "😼",
    "😽", "🙀", "😿", "😾", "🙈", "🙉", "🙊", "🐵",
    "🐶", "🐺", "🦊", "🐱", "🦁", "🐯", "🐴", "🦄",
    "🐮", "🐷", "🐗", "🐭", "🐹", "🐰", "🐻", "🐼",
    "🐨", "🐸", "🦖", "🦕", "🐲", "🐉", "🦎", "🐍",
    "🐢", "🐊", "🦈", "🐬", "🐳", "🐋", "🐙", "🦑",
    "🪼", "🦀", "🐡", "🐠", "🐟", "🐝", "🪲", "🐞",
    "🦋", "🕷️", "🦂", "🦟", "🪰", "🪱", "🐜", "🦗",
    "🌸", "🌺", "🌻", "🌷", "🌹", "🥀", "🌵", "🌴",
    "🌲", "🌳", "🍀", "🍁", "🍂", "🍄", "🌾", "💐",
    "🌞", "🌝", "🌛", "🌜", "🌚", "🌕", "🌖", "🌗",
    "🌘", "🌑", "🌒", "🌓", "🌔", "🌙", "⭐", "🌟",
    "✨", "⚡", "☄️", "💥", "🔥", "🌪️",  "🪐", "☀️",
    "⛈️", "❄️", "☃️", "🌊", "💧", "🧊", "🍎", "🍐",
    "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍒",
    "🥭", "🍍", "🥥", "🥝", "🍅", "🥑", "🥦", "🌽",
    "🥕", "🧄", "🧅", "🍄", "🥔", "🍠", "🥐", "🍞",
    "🥖", "🧀", "🍖", "🍗", "🥩", "🍔", "🍟", "🍕",
    "🌭", "🌮", "🌯", "🥙", "🍜", "🍣", "🍤", "🍙",
    "🍚", "🍛", "🍥", "🥟", "🍱", "🧁", "🍰", "🎂",
    "🍪", "🍩", "🍫", "🍬", "🍭", "🍯", "☕", "🍵",
    "🧃", "🥤", "🍺", "🍷", "🥂", "🍹", "🧋", "🎮",
    "🕹️", "🎲", "♟️", "🎯", "🎳", "🎭", "🎨", "🎬",
    "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎸", "🎺",
    "🪘", "🚗", "🏎️", "🚓", "🚑", "🚒", "🚜", "🏍️",
    "✈️", "🚀", "🛸", "🚁", "⛵", "🚤", "🛶", "🚂",
    "🏰", "🗿", "🗽", "🗼", "🎡", "🎢", "🏕️", "🏝️",
    "🏔️", "🌋", "🧭", "⌛", "⏳", "🕰️", "📡", "💡",
    "🔦", "🧨", "💎", "🔮", "🪄", "🧿", "⚔️", "🛡️",
    "🧠", "🫀", "🫁", "👁️", "🦾", "🦿", "🤝", "👏",
    "🙌", "🫶", "👍", "👎", "✌️", "🤟", "🤘", "👌",
    "🫵", "👑", "💍", "👓", "🕶️", "🥽", "🧢", "👕",
    "👽", "🤖", "💻", "⌨️", "🖥️", "📱", "🛰️", "🔋",
    "📀", "💿", "📼", "📷", "📸", "🎥", "📺", "📻",
    "📖", "📚", "✏️", "🖊️", "📎", "📌", "🧷", "✂️",
    "🔒", "🔓", "🗝️", "❤️", "🧡", "💛", "💚", "💙",
    "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞",
    "💓", "💗", "💖", "💘", "💝", "☢️", "☣️", "⚠️",
    "🚫", "❌", "⭕", "💯", "💢", "♾️", "🔱", "⚜️",
)

fun User.stableEmojiIdentity(
    length: Int = 6
): String {
    return stableHash()
        .take(length)
        .joinToString("") { byte ->
            val index =
                (byte.toInt() and 0xFF) % identityEmojiPool.size

            identityEmojiPool[index]
        }
}

suspend fun Member.displayColor(): Color {
    return asUser().generatedColor()
}