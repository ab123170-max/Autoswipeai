package com.autoswipe.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Sends an upward swipe only after the user explicitly presses Start.
 * It does not inspect, copy, or store visible screen text or personal content.
 */
class AutoSwipeAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var gestureInProgress = false

    private val swipeLoop = object : Runnable {
        override fun run() {
            if (!isRunning || gestureInProgress) return

            val foregroundPackage = try {
                rootInActiveWindow?.packageName?.toString()
            } catch (_: Exception) {
                null
            }

            if (foregroundPackage !in SUPPORTED_PACKAGES) {
                handler.postDelayed(this, 1000L)
                return
            }

            val metrics = resources.displayMetrics
            val x = metrics.widthPixels * 0.5f
            val startY = metrics.heightPixels * 0.80f
            val endY = metrics.heightPixels * 0.27f
            val swipePath = Path().apply {
                moveTo(x, startY)
                lineTo(x, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        swipePath,
                        0L,
                        SWIPE_DURATION_MS
                    )
                )
                .build()

            gestureInProgress = true
            val accepted = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        gestureInProgress = false
                        if (isRunning) handler.postDelayed(swipeLoop, SWIPE_INTERVAL_MS)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        gestureInProgress = false
                        if (isRunning) handler.postDelayed(swipeLoop, 1500L)
                    }
                },
                handler
            )

            if (!accepted) {
                gestureInProgress = false
                handler.postDelayed(this, 1500L)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not inspect or store accessibility event content.
    }

    override fun onInterrupt() {
        pauseInternal()
    }

    override fun onDestroy() {
        pauseInternal()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun startInternal() {
        if (isRunning) return
        isRunning = true
        handler.removeCallbacks(swipeLoop)
        handler.postDelayed(swipeLoop, 800L)
    }

    private fun pauseInternal() {
        isRunning = false
        handler.removeCallbacks(swipeLoop)
    }

    companion object {
        private const val SWIPE_DURATION_MS = 380L
        private const val SWIPE_INTERVAL_MS = 5000L

        const val COMPONENT_NAME =
            "com.autoswipe.ai/com.autoswipe.ai.AutoSwipeAccessibilityService"

        private val SUPPORTED_PACKAGES = setOf(
            "com.zhiliaoapp.musically",
            "com.zhiliaoapp.musically.go",
            "com.instagram.android",
            "com.google.android.youtube",
            "com.facebook.katana"
        )

        @Volatile
        private var instance: AutoSwipeAccessibilityService? = null

        fun isConnected(): Boolean = instance != null

        fun startSwiping() {
            instance?.startInternal()
        }

        fun pauseSwiping() {
            instance?.pauseInternal()
        }

        fun stopSwiping() {
            instance?.pauseInternal()
        }
    }
}
