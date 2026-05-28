package com.greedy.assistant

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class TapService : AccessibilityService() {
    companion object {
        var instance: TapService? = null
        fun tap(x: Int, y: Int, duration: Long = 100) {
            instance?.performGlobalClick(x, y, duration)
        }
    }

    override fun onServiceConnected() {
        instance = this
    }

    private fun performGlobalClick(x: Int, y: Int, duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}