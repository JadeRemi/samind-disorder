package com.samind.testfeed

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.ScrollView
import android.widget.TextView

/**
 * Test harness only: a separate app (separate package) that renders whatever
 * text it is handed. Behavior tests use it to put controlled text on a foreign
 * app's screen, instead of depending on how a stock app handles intent extras.
 *
 *   am start -n com.samind.testfeed/.FeedActivity --es text "..."
 */
class FeedActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        val view = TextView(this).apply {
            setText(text)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(32, 64, 32, 32)
            contentDescription = text
        }
        setContentView(ScrollView(this).apply { addView(view) })
        // lets the harness confirm what rendered without running uiautomator,
        // which hijacks accessibility and tears down the overlays under test
        Log.i(TAG, "showing: $text")
    }

    private companion object {
        const val EXTRA_TEXT = "text"
        const val TAG = "SamindTestFeed"
    }
}
