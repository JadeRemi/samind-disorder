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
        classifier = TriggerClassifier(this)
        overlay = OverlayController(this)
        if (Prefs.monitoringEnabled(this)) overlay.showMascot()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (!Prefs.monitoringEnabled(this)) return

        val source = event.packageName?.toString() ?: return
        if (source == packageName || source in IGNORED_PACKAGES) return

        val now = SystemClock.elapsedRealtime()
        if (now < cooldownUntil || overlay.isQuestionShowing) return

        val root = rootInActiveWindow ?: return
        val text = collectText(root)
        if (text.length < 12) return

        val hash = text.hashCode()
        if (hash == lastAnalyzedHash) return
        lastAnalyzedHash = hash

        scope.launch {
            try {
                val result = classifier.classify(text)
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

    private fun collectText(node: AccessibilityNodeInfo, budget: StringBuilder = StringBuilder()): String {
        if (budget.length > MAX_TEXT) return budget.toString()
        node.text?.let { budget.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, budget)
            child.recycle()
        }
        return budget.toString()
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
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.launcher3",
        )
    }
}
