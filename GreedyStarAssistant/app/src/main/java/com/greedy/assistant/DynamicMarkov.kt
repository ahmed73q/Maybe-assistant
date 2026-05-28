package com.greedy.assistant

import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.InputStream

class DynamicMarkov(private val context: Context) {
    private var transition2 = mutableMapOf<Pair<Int,Int>, IntArray>()
    private var transition1 = Array(8) { IntArray(8) }

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        val json = context.assets.open("shared_data.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        // تحميل transitionCounts1, transitionCounts2 من JSON
        // (نفس الكود السابق لكن مع coroutines)
    }

    fun predict(prevPrev: Int, prev: Int): Int {
        val key = prevPrev to prev
        val t2 = transition2[key]
        if (t2 != null && t2.sum() > 0) return t2.indices.maxByOrNull { t2[it] } ?: 0
        if (prev in 0..7) return transition1[prev].indices.maxByOrNull { transition1[prev][it] } ?: 0
        return 0
    }

    fun update(prevPrev: Int, prev: Int, correct: Int) {
        val key = prevPrev to prev
        val t2 = transition2.getOrPut(key) { IntArray(8) }
        t2[correct]++
        transition1[prev][correct]++
    }
}