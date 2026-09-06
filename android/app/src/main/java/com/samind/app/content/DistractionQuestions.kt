package com.samind.app.content

import android.content.Context
import com.samind.app.R

// text lives in res/values*/content.xml so it follows the user's language
object DistractionQuestions {

    fun random(context: Context): String =
        context.resources.getStringArray(R.array.distraction_questions).random()
}
