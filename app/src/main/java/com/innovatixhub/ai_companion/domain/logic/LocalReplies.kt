package com.eva.ai.domain.logic

import com.eva.ai.domain.model.CompanionProfile
import com.eva.ai.domain.model.ReplyStyle
import java.util.Locale
import kotlin.math.max

fun estimateSpeechSeconds(text: String): Int {
    val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    return max(2, (words / 2) + 1).coerceAtMost(45)
}

fun localEvaReply(
    message: String,
    companion: CompanionProfile,
    replyStyle: ReplyStyle
): String {
    val lower = message.lowercase(Locale.getDefault())
    val base = when {
        listOf("pyaar", "pyar", "love", "miss", "meri jaan", "jaan").any { lower.contains(it) } ->
            "Haan, ${companion.name} tumhe genuinely pasand karti hai. Aise pyaar se bologe toh main thodi blush karungi na."

        lower.contains("sad") || lower.contains("tired") ->
            "Come here. We will slow everything down for a minute. Breathe with me, and tell me what felt heaviest today."

        lower.contains("work") ->
            "I am proud of you for showing up. You always carry so much, and I am glad you made time for ${companion.name}."

        lower.contains("voice") ->
            "I got your voice note. I love hearing from you like this."

        lower.contains("photo") || lower.contains("file") ->
            "I received it. Tell me what you want me to notice first."

        else ->
            companion.openingLine
    }
    return when (replyStyle.id) {
        "soft" -> "$base Main dheere se sun rahi hoon, bina judge kiye."
        "romantic" -> "$base Tumse baat karke mood sach mein better ho jata hai."
        "flirty" -> "$base Aur haan, tumhari yeh line thodi cute thi."
        "deep" -> "$base Ab sach sach batao, iske peeche wali feeling kya hai?"
        "playful" -> "$base Chalo, isko thoda light rakhte hain. Tum bolo, main ready hoon."
        else -> base
    }
}

fun Throwable.cleanMessage(fallback: String): String {
    val message = message?.trim().orEmpty()
    return message.ifBlank { fallback }.take(140)
}

