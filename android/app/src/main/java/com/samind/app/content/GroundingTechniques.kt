package com.samind.app.content

data class GroundingTechnique(
    val id: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
)

object GroundingTechniques {

    val all = listOf(
        GroundingTechnique(
            id = "54321",
            title = "5-4-3-2-1 senses",
            summary = "Anchor yourself through your five senses.",
            steps = listOf(
                "Name 5 things you can see around you.",
                "Name 4 things you can physically feel.",
                "Name 3 things you can hear right now.",
                "Name 2 things you can smell.",
                "Name 1 thing you can taste.",
            ),
        ),
        GroundingTechnique(
            id = "box_breath",
            title = "Box breathing",
            summary = "Slow, even breaths to settle your body.",
            steps = listOf(
                "Breathe in through your nose for 4 counts.",
                "Hold your breath for 4 counts.",
                "Breathe out slowly for 4 counts.",
                "Hold empty for 4 counts.",
                "Repeat the square 4 more times.",
            ),
        ),
        GroundingTechnique(
            id = "cold_water",
            title = "Cool reset",
            summary = "Temperature shift interrupts the stress loop.",
            steps = listOf(
                "Go to the nearest sink.",
                "Run cool water over your wrists for 30 seconds.",
                "Notice the temperature changing on your skin.",
                "Pat your face gently with cool hands.",
            ),
        ),
        GroundingTechnique(
            id = "body_scan",
            title = "Feet on the floor",
            summary = "Return attention to physical support.",
            steps = listOf(
                "Press both feet firmly into the floor.",
                "Notice the weight of your body on the chair.",
                "Unclench your jaw and drop your shoulders.",
                "Push your palms together for 5 seconds, then release.",
            ),
        ),
        GroundingTechnique(
            id = "categories",
            title = "Category sprint",
            summary = "Give the racing mind a neutral job.",
            steps = listOf(
                "Pick a category: cities, animals, or films.",
                "Name one item for every letter from A to J.",
                "Stuck on a letter? Skip it, keep moving.",
                "Notice how your breathing slowed down.",
            ),
        ),
    )

    fun byId(id: String): GroundingTechnique? = all.find { it.id == id }
}
