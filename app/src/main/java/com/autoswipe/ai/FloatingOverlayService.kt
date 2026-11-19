package com.autoswipe.ai

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color as AndroidColor

class FloatingOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showBubble()
    }
    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun showBubble() {
        if (bubble != null) return
        val button = TextView(this).apply {
            text = "AS"; textSize = 17f; setTextColor(AndroidColor.WHITE)
            gravity = Gravity.CENTER; setBackgroundColor(AndroidColor.rgb(40, 120, 240)); elevation = 12f
        }
        val params = WindowManager.LayoutParams(dp(58), dp(58), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(18); y = dp(180)
        }
        button.setOnTouchListener(object : View.OnTouchListener {
            private var originX = 0; private var originY = 0
            private var downX = 0f; private var downY = 0f; private var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { originX=params.x; originY=params.y; downX=event.rawX; downY=event.rawY; moved=false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx=(event.rawX-downX).toInt(); val dy=(event.rawY-downY).toInt()
                        if (kotlin.math.abs(dx)>5 || kotlin.math.abs(dy)>5) moved=true
                        params.x=originX+dx; params.y=originY+dy; windowManager.updateViewLayout(button,params); return true
                    }
                    MotionEvent.ACTION_UP -> { if (!moved) togglePanel(); return true }
                }
                return false
            }
        })
        windowManager.addView(button,params); bubble=button; bubbleParams=params
    }

    private fun togglePanel() {
        if (panel != null) { removePanel(); return }
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(12),dp(14),dp(12)); setBackgroundColor(AndroidColor.rgb(16,35,63)); elevation=16f }
        val title=TextView(this).apply { text="AutoSwipe AI"; textSize=16f; setTextColor(AndroidColor.WHITE); setPadding(0,0,0,dp(8)) }
        val status=TextView(this).apply { text="Ready — 5-second interval"; textSize=13f; setTextColor(AndroidColor.LTGRAY); setPadding(0,0,0,dp(8)) }
        box.addView(title); box.addView(status)
        fun control(label:String, action:()->Unit) {
            val b=Button(this).apply { text=label; setOnClickListener { action() } }
            box.addView(b,LinearLayout.LayoutParams(dp(190),dp(48)))
        }
        control("Start auto-swipe") {
            if (AutoSwipeAccessibilityService.isConnected()) {
                AutoSwipeAccessibilityService.startSwiping(); status.text="Running — supported apps only"
            } else status.text="Enable AutoSwipe AI in Accessibility settings first"
        }
        control("Pause") { AutoSwipeAccessibilityService.pauseSwiping(); status.text="Paused" }
        control("Stop") { AutoSwipeAccessibilityService.stopSwiping(); status.text="Stopped" }
        control("Close panel") { removePanel() }
        control("Remove floating button") { AutoSwipeAccessibilityService.stopSwiping(); stopSelf() }
        val params=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,overlayType(),WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT).apply {
            gravity=Gravity.TOP or Gravity.START; x=bubbleParams?.x?:dp(18); y=(bubbleParams?.y?:dp(180))+dp(68)
        }
        windowManager.addView(box,params); panel=box
    }
    private fun removePanel() { panel?.let { try { windowManager.removeView(it) } catch (_:Exception) {} }; panel=null }
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    override fun onDestroy() {
        AutoSwipeAccessibilityService.stopSwiping(); removePanel()
        bubble?.let { try { windowManager.removeView(it) } catch (_:Exception) {} }; bubble=null
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}