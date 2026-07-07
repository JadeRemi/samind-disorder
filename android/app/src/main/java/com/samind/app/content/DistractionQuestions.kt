package com.samind.app.content

object DistractionQuestions {

    private val questions = listOf(
        "What kind of weather do you like most?",
        "If you could teleport anywhere right now, where would you go?",
        "What song has been stuck in your head lately?",
        "What smell instantly reminds you of childhood?",
        "If animals could talk, which would be the rudest?",
        "What's the best gift you've ever received?",
        "Which fictional place would you move to?",
        "What tiny sound do you find weirdly satisfying?",
        "If your week had a color, what would it be?",
        "What's something you're curious about right now?",
        "Which board game could you play forever?",
        "What would your autobiography be called?",
        "If you had a boat, what would you name it?",
        "What's the strangest dream you remember?",
        "Sunrise or sunset — and why?",
        "What superpower would be useless but fun?",
        "What dish could you cook with your eyes closed?",
        "If today were a movie genre, which one?",
        "What's the softest thing you've ever touched?",
        "Which language would you learn overnight if you could?",
        "What game did you invent as a kid?",
        "If clouds had flavors, what would today's taste like?",
        "What's a word you love saying out loud?",
        "Which museum would you sleep over in?",
        "What would you ask a time traveler from 3026?",
        "What's your favorite sound in nature?",
        "If you opened a tiny shop, what would it sell?",
        "What's the best view you've ever seen?",
        "Which cartoon character would be your roommate?",
        "What hobby would you try if no one was watching?",
    )

    fun random(): String = questions.random()
}
