package com.samind.app.chat

import android.content.Context
import com.samind.app.R

data class ChatMessage(val fromUser: Boolean, val text: String)

// Rule-based support companion. Deliberately conservative: it validates,
// grounds and points to real help; it never gives dietary advice.
// Replies live in res/values*/content.xml so they follow the user's language;
// patterns are matched per language, since users type in their own.
class ChatEngine(private val context: Context) {

    private class Rule(val patterns: List<Regex>, val repliesRes: Int)

    private val crisisPatterns = listOf(
        Regex("(hurt|harm|kill)\\w* (myself|me)"),
        Regex("\\bsuicid\\w*"),
        Regex("don'?t want to (live|be here)"),
        Regex("(поврежу|наврежу|убью) себ", RegexOption.IGNORE_CASE),
        Regex("суицид", RegexOption.IGNORE_CASE),
        Regex("не хочу (жить|быть здесь)", RegexOption.IGNORE_CASE),
    )

    private val rules = listOf(
        Rule(
            listOf(
                Regex("anxi\\w*"), Regex("panic"), Regex("overwhelm\\w*"), Regex("scared"),
                Regex("тревог"), Regex("паник"), Regex("страшно"), Regex("накрыва"),
            ),
            R.array.chat_anxiety,
        ),
        Rule(
            listOf(
                Regex("trigger\\w*"), Regex("saw a post"), Regex("that content"),
                Regex("триггер"), Regex("увидел\\w* пост"), Regex("этот контент"),
            ),
            R.array.chat_trigger,
        ),
        Rule(
            listOf(
                Regex("guilt\\w*"), Regex("ashamed"), Regex("hate (myself|my body)"),
                Regex("вина|виноват"), Regex("стыдно"), Regex("ненавижу (себя|своё тело)"),
            ),
            R.array.chat_guilt,
        ),
        Rule(
            listOf(
                Regex("\\burge\\b"), Regex("want to (restrict|skip|purge)"),
                Regex("тян[ае]т"), Regex("хочу (ограничить|пропустить|очистить)"), Regex("срыв"),
            ),
            R.array.chat_urge,
        ),
        Rule(
            listOf(
                Regex("\\b(hi|hello|hey)\\b"), Regex("good (morning|evening)"),
                Regex("\\b(привет|здравствуй|хай)\\b"),
            ),
            R.array.chat_greeting_reply,
        ),
        Rule(
            listOf(
                Regex("thank\\w*"), Regex("better now"),
                Regex("спасибо"), Regex("(стало|мне) лучше"),
            ),
            R.array.chat_thanks,
        ),
    )

    fun reply(userText: String): String {
        val text = userText.lowercase()
        if (crisisPatterns.any { it.containsMatchIn(text) }) {
            return context.getString(R.string.chat_crisis)
        }
        for (rule in rules) {
            if (rule.patterns.any { it.containsMatchIn(text) }) return pick(rule.repliesRes)
        }
        return pick(R.array.chat_fallback)
    }

    private fun pick(arrayRes: Int): String =
        context.resources.getStringArray(arrayRes).random()
}
