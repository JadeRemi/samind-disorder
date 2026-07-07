package com.samind.app.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.samind.app.MainActivity
import com.samind.app.R
import com.samind.app.content.DistractionQuestions

class OverlayController(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mascotView: View? = null
    private var questionView: View? = null

    val isQuestionShowing: Boolean
        get() = questionView != null

    fun showMascot() = mainHandler.post {
        if (mascotView != null) return@post
        val view = LayoutInflater.from(service).inflate(R.layout.overlay_mascot, null)
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
        makeDraggable(view, params)
        view.setOnClickListener { openApp("chat") }
        windowManager.addView(view, params)
        mascotView = view
    }

    fun showQuestion() = mainHandler.post {
        if (questionView != null) return@post
        val view = LayoutInflater.from(service).inflate(R.layout.overlay_question, null)
        view.findViewById<TextView>(R.id.question_text).text = DistractionQuestions.random()
        view.findViewById<Button>(R.id.dismiss_button).setOnClickListener { hideQuestion() }
        view.findViewById<Button>(R.id.open_button).setOnClickListener {
            hideQuestion()
            openApp("ground")
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(view, params)
        questionView = view
    }

    fun hideQuestion() = mainHandler.post {
        questionView?.let { windowManager.removeView(it) }
        questionView = null
    }

    fun hideMascot() = mainHandler.post {
        mascotView?.let { windowManager.removeView(it) }
        mascotView = null
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

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX - (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(event.rawX - touchX) > 24 ||
                        kotlin.math.abs(event.rawY - touchY) > 24
                    ) {
                        moved = true
                    }
                    windowManager.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) v.performClick()
                    true
                }
                else -> false
            }
        }
    }
}
