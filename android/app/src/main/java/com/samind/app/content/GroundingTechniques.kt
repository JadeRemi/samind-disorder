package com.samind.app.content

import android.content.Context
import com.samind.app.R

data class GroundingTechnique(
    val id: String,
    val title: String,
    val summary: String,
    val steps: List<String>,
)

// text lives in res/values*/content.xml so it follows the user's language
object GroundingTechniques {

    private data class Ids(
        val id: String,
        val title: Int,
        val summary: Int,
        val steps: Int,
    )

    private val catalogue = listOf(
        Ids("54321", R.string.tech_54321_title, R.string.tech_54321_summary, R.array.tech_54321_steps),
        Ids("box_breath", R.string.tech_box_breath_title, R.string.tech_box_breath_summary, R.array.tech_box_breath_steps),
        Ids("cold_water", R.string.tech_cold_water_title, R.string.tech_cold_water_summary, R.array.tech_cold_water_steps),
        Ids("body_scan", R.string.tech_body_scan_title, R.string.tech_body_scan_summary, R.array.tech_body_scan_steps),
        Ids("categories", R.string.tech_categories_title, R.string.tech_categories_summary, R.array.tech_categories_steps),
    )

    fun all(context: Context): List<GroundingTechnique> = catalogue.map { it.resolve(context) }

    fun byId(context: Context, id: String): GroundingTechnique? =
        catalogue.find { it.id == id }?.resolve(context)

    private fun Ids.resolve(context: Context) = GroundingTechnique(
        id = id,
        title = context.getString(title),
        summary = context.getString(summary),
        steps = context.resources.getStringArray(steps).toList(),
    )
}
