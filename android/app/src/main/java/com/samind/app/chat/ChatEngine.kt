package com.samind.app.chat

data class ChatMessage(val fromUser: Boolean, val text: String)

// Rule-based support companion. Deliberately conservative: it validates,
// grounds and points to real help; it never gives dietary advice.
class ChatEngine {

    private data class Rule(val patterns: List<Regex>, val responses: List<String>)

    private val crisis = Rule(
        patterns = listOf(
            Regex("(hurt|harm|kill)\\w* (myself|me)"),
            Regex("\\bsuicid\\w*"),
            Regex("don'?t want to (live|be here)"),
        ),
        responses = listOf(
            "I'm really glad you told me. This is bigger than what I can hold, and you deserve real support. " +
                "Please reach out right now to someone you trust or a crisis line in your country — " +
                "you can find one at findahelpline.com. I'll stay here with you meanwhile.",
        ),
    )

    private val rules = listOf(
        Rule(
            listOf(Regex("anxi\\w*"), Regex("panic"), Regex("overwhelm\\w*"), Regex("scared")),
            listOf(
                "That sounds really heavy. Let's slow things down together — try breathing in for 4 counts and out for 6. Want a grounding exercise?",
                "Anxiety lies about how urgent everything is. You're safe in this moment. Can you name 3 things you can see right now?",
            ),
        ),
        Rule(
            listOf(Regex("trigger\\w*"), Regex("saw a post"), Regex("that content")),
            listOf(
                "It makes sense that it shook you — that content is designed to hook. You noticed it, and that's the skill that matters. Shall we do a quick reset?",
                "Good on you for stepping away. The urge fades faster when we give attention somewhere else. What's one thing nearby that feels pleasant to look at?",
            ),
        ),
        Rule(
            listOf(Regex("guilt\\w*"), Regex("ashamed"), Regex("hate (myself|my body)")),
            listOf(
                "Guilt after eating is a symptom talking, not the truth. Food is not a moral act. You did something your body needed.",
                "You deserve kindness from yourself, especially now. If a friend said this about themselves, what would you tell them?",
            ),
        ),
        Rule(
            listOf(Regex("\\burge\\b"), Regex("want to (restrict|skip|purge)")),
            listOf(
                "Thank you for saying it out loud — urges lose power when they're named. They peak and pass, usually within 20-30 minutes. Can we ride this one out together?",
                "An urge is a wave, not a command. Let's find something for your hands: tea, a shower, a message to a friend?",
            ),
        ),
        Rule(
            listOf(Regex("\\b(hi|hello|hey)\\b"), Regex("good (morning|evening)")),
            listOf(
                "Hey, good to see you. How are you feeling right now, honestly?",
                "Hi! I'm here. What's going on today?",
            ),
        ),
        Rule(
            listOf(Regex("thank\\w*"), Regex("better now")),
            listOf(
                "I'm glad. You did the work — I just kept you company.",
                "Any time. Checking in like this is a real recovery skill.",
            ),
        ),
    )

    private val fallback = listOf(
        "I hear you. Tell me a bit more about what that feels like?",
        "That matters. What do you think your body is asking for right now — rest, warmth, company?",
        "I'm listening. Would a grounding exercise help while we talk?",
    )

    fun reply(userText: String): String {
        val text = userText.lowercase()
        if (crisis.patterns.any { it.containsMatchIn(text) }) return crisis.responses.first()
        for (rule in rules) {
            if (rule.patterns.any { it.containsMatchIn(text) }) return rule.responses.random()
        }
        return fallback.random()
    }
}
