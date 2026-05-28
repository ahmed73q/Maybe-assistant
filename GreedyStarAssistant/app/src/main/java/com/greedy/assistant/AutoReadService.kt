package com.greedy.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import java.util.regex.Pattern

class AutoReadService : AccessibilityService() {
    companion object {
        var onNewResult: ((Int) -> Unit)? = null
        var onTimerChange: ((Int) -> Unit)? = null
        var onRoundStart: (() -> Unit)? = null
    }

    private var lastWinner = -1
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val allText = getAllText(root)

        // قراءة العداد
        val timer = extractTimer(allText)
        timer?.let { onTimerChange?.invoke(it) }

        // قراءة الفائز
        val winner = extractWinner(allText)
        if (winner != null && winner != lastWinner) {
            lastWinner = winner
            onNewResult?.invoke(winner)
        }

        // بداية جولة جديدة
        if (timer == 29) onRoundStart?.invoke()
    }

    private fun getAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (!text.isNullOrEmpty()) sb.append(text).append(" ")
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { sb.append(getAllText(it)) }
        }
        return sb.toString()
    }

    private fun extractTimer(text: String): Int? {
        val pattern = Pattern.compile("Select Time\\s+(\\d+)\\s*S", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val sec = matcher.group(1).toIntOrNull()
            if (sec != null && (sec == 29 || sec == 5 || sec in 0..60)) return sec
        }
        return null
    }

    private fun extractWinner(text: String): Int? {
        val pattern = Pattern.compile("(?:Winner|فائز|Win|فاز)\\s*:?\\s*\\b([0-7])\\b", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1).toIntOrNull()
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}