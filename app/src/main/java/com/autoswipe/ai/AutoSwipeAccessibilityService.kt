package com.autoswipe.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects common, accessibility-exposed end-of-video labels in supported apps.
 * Only transiently checks visible labels for end markers; it does not collect,
 * log, or store screen text. Detection depends on each app exposing these labels.
 */
class AutoSwipeAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var gestureInProgress = false

    private val detectionLoop = object : Runnable {
        override fun run() {
            if (!isRunning || gestureInProgress) return

            val foregroundPackage = try {
                rootInActiveWindow?.packageName?.toString()
            } catch (_: Exception) {
                null
            }

            if (foregroundPackage !in SUPPORTED_PACKAGES) {
                handler.postDelayed(this, POLL_INTERVAL_MS)
                return
            }

            val finished = try {
                rootInActiveWindow?.let { root ->
                    try {
                        containsEndMarker(root, 0, intArrayOf(0))
                    } finally {
                        root.recycle()
                    }
                } ?: false
            } catch (_: Exception) {
                false
            }

            if (finished) {
                dispatchNextVideoSwipe()
            } else {
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Detection runs while explicitly started; event content is not retained.
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
        handler.removeCallbacks(detectionLoop)
        handler.post(detectionLoop)
    }

    private fun pauseInternal() {
        isRunning = false
        handler.removeCallbacks(detectionLoop)
    }

    private fun containsEndMarker(
        node: AccessibilityNodeInfo,
        depth: Int,
        visited: IntArray
    ): Boolean {
        if (depth > MAX_TREE_DEPTH || visited[0] >= MAX_NODES_TO_CHECK) return false
        visited[0]++

        val label = buildString {
            node.text?.let { append(it).append(' ') }
            node.contentDescription?.let { append(it) }
        }.lowercase()

        if (END_MARKERS.any { label.contains(it) }) return true

        for (index in 0 until node.childCount) {
            if (visited[0] >= MAX_NODES_TO_CHECK) break
            val child = try { node.getChild(index) } catch (_: Exception) { null } ?: continue
            val found = try {
                containsEndMarker(child, depth + 1, visited)
            } finally {
                child.recycle()
            }
            if (found) return true
        }
        return false
    }

    private fun dispatchNextVideoSwipe() {
        if (!isRunning || gestureInProgress) return
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val path = Path().apply {
            moveTo(x, metrics.heightPixels * 0.80f)
            lineTo(x, metrics.heightPixels * 0.27f)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, SWIPE_DURATION_MS))
            .build()

        gestureInProgress = true
        val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                gestureInProgress = false
                if (isRunning) handler.postDelayed(detectionLoop, POST_SWIPE_DELAY_MS)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                gestureInProgress = false
                if (isRunning) handler.postDelayed(detectionLoop, POLL_INTERVAL_MS)
            }
        }, handler)

        if (!accepted) {
            gestureInProgress = false
            handler.postDelayed(detectionLoop, POLL_INTERVAL_MS)
        }
    }

    companion object {
        private const val SWIPE_DURATION_MS = 380L
        private const val POLL_INTERVAL_MS = 700L
        private const val POST_SWIPE_DELAY_MS = 1800L
        private const val MAX_TREE_DEPTH = 18
        private const val MAX_NODES_TO_CHECK = 1200

        // Common English end-state labels exposed by some app versions.
        // A cue-based detector cannot guarantee support for every app/version/language.
        private val END_MARKERS = setOf(
            "watch again", "watch again", "replay video", "replay",
            "video ended", "video has ended", "play again", "rewatch",
            "try again", "video unavailable"
        )

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
        fun startSwiping() { instance?.startInternal() }
        fun pauseSwiping() { instance?.pauseInternal() }
        fun stopSwiping() { instance?.pauseInternal() }
    }
}
