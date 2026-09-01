package com.samind.app.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.samind.app.SamindApp
import com.samind.app.data.Prefs
import com.samind.app.data.db.TriggerEvent
import com.samind.app.ml.TriggerClassifier
import com.samind.app.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScreenReaderService : AccessibilityService() {

    private lateinit var classifier: TriggerClassifier
    private lateinit var overlay: OverlayController
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var cooldownUntil = 0L
    private var lastAnalyzedHash = 0

    override fun onServiceConnected() {
        // never let setup kill the service: a dead service silently disables
        // itself in system settings and the user is left unprotected
        try {
            classifier = TriggerClassifier(this)
            overlay = OverlayController(this)
            if (Prefs.monitoringEnabled(this)) overlay.showMascot()
        } catch (e: Exception) {
            Log.e(TAG, "service setup failed", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!Prefs.monitoringEnabled(this)) return

        val source = event.packageName?.toString() ?: return
        if (source == packageName || source in IGNORED_PACKAGES) return

        val now = SystemClock.elapsedRealtime()
        if (now < cooldownUntil || overlay.isQuestionShowing) return

        val root = rootInActiveWindow ?: return
        val pieces = collectText(root)
        val chunks = chunk(pieces)
        if (chunks.isEmpty()) return

        val hash = chunks.hashCode()
        if (hash == lastAnalyzedHash) return
        lastAnalyzedHash = hash

        scope.launch {
            try {
                // per-chunk, worst score wins: scoring the whole screen as one blob
                // lets surrounding UI text dilute a real trigger below the threshold
                val result = chunks
                    .map { classifier.classify(it) }
                    .maxByOrNull { it.score } ?: return@launch
                Log.d(TAG, "score=${result.score} risky=${result.risky} pkg=$source chunks=${chunks.size}")
                if (result.risky) {
                    cooldownUntil = SystemClock.elapsedRealtime() + COOLDOWN_MS
                    overlay.showQuestion()
                    SamindApp.instance.database.triggerEvents().insert(
                        TriggerEvent(
                            timestamp = System.currentTimeMillis(),
                            sourcePackage = source,
                            score = result.score,
                        )
                    )
                }
            } catch (e: Exception) {
                // one bad event must never kill the monitoring service
                Log.e(TAG, "trigger handling failed", e)
            }
        }
    }

    private fun collectText(
        node: AccessibilityNodeInfo,
        out: MutableList<String> = mutableListOf(),
    ): MutableList<String> {
        if (out.sumOf { it.length } > MAX_TEXT) return out
        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out)
            child.recycle()
        }
        return out
    }

    // merge consecutive fragments into a few sentence-sized chunks, so one post's
    // text stays together while unrelated UI labels don't drown it
    private fun chunk(pieces: List<String>): List<String> {
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (piece in pieces) {
            if (current.isNotEmpty() && current.length + piece.length > CHUNK_CHARS) {
                chunks.add(current.toString())
                current.clear()
                if (chunks.size >= MAX_CHUNKS) break
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(piece)
        }
        if (current.isNotEmpty() && chunks.size < MAX_CHUNKS) chunks.add(current.toString())
        return chunks.filter { it.length >= MIN_CHUNK_CHARS }
    }

    override fun onInterrupt() {
        overlay.hideAll()
    }

    override fun onDestroy() {
        overlay.hideAll()
        classifier.close()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ScreenReaderService"
        private const val COOLDOWN_MS = 45_000L
        private const val MAX_TEXT = 4_000
        private const val CHUNK_CHARS = 200
        private const val MIN_CHUNK_CHARS = 12
        private const val MAX_CHUNKS = 8
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.launcher3",
        )
    }
}
