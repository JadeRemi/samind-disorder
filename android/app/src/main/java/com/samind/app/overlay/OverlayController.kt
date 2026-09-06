package com.samind.app.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.samind.app.MainActivity
import com.samind.app.content.DistractionQuestions

/**
 * Owns the two overlay windows. Both are Compose, built from the shared UI kit
 * — nothing here styles anything on its own.
 */
class OverlayController(private val service: AccessibilityService) {

    // API 30+ needs a window context for TYPE_ACCESSIBILITY_OVERLAY, and that
    // context can only come from a display context.
    private val overlayContext: Context = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = service.getSystemService(DisplayManager::class.java)
                .getDisplay(Display.DEFAULT_DISPLAY)
            service.createDisplayContext(display).createWindowContext(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                null,
            )
        } else {
            service
        }
    } catch (e: Exception) {
        Log.e(TAG, "window context unavailable, using service context", e)
        service
    }

    private val windowManager = overlayContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mascotView: View? = null
    private var questionView: View? = null
    private var mascotHost: OverlayHost? = null
    private var questionHost: OverlayHost? = null

    val isQuestionShowing: Boolean get() = questionView != null

    fun showMascot() = mainHandler.post {
        if (mascotView != null) return@post
        val host = OverlayHost()
        val view = host.createView(overlayContext) {
            MascotBubble(onTap = { openApp(MainActivity.CHAT) })
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 240
        }
        try {
            windowManager.addView(view, params)
            mascotView = view
            mascotHost = host
        } catch (e: Exception) {
            host.destroy()
            Log.e(TAG, "could not add mascot overlay", e)
        }
    }

    fun showQuestion() = mainHandler.post {
        if (questionView != null) return@post
        val host = OverlayHost()
        val question = DistractionQuestions.random(overlayContext)
        val view = host.createView(overlayContext) {
            InterventionOverlay(
                question = question,
                onRefocus = {
                    hideQuestion()
                    openApp(MainActivity.PRACTICES)
                },
                onDismiss = { hideQuestion() },
            )
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        )
        try {
            windowManager.addView(view, params)
            questionView = view
            questionHost = host
            Log.i(TAG, "question overlay shown")
        } catch (e: Exception) {
            host.destroy()
            Log.e(TAG, "could not add question overlay", e)
        }
    }

    fun hideQuestion() = mainHandler.post {
        questionView?.let { runCatching { windowManager.removeView(it) } }
        questionHost?.destroy()
        questionView = null
        questionHost = null
    }

    fun hideMascot() = mainHandler.post {
        mascotView?.let { runCatching { windowManager.removeView(it) } }
        mascotHost?.destroy()
        mascotView = null
        mascotHost = null
    }

    fun hideAll() {
        hideQuestion()
        hideMascot()
    }

    private fun openApp(destination: String) {
        val intent = Intent(service, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_DESTINATION, destination)
        }
        service.startActivity(intent)
    }

    private companion object {
        const val TAG = "OverlayController"
    }
}
