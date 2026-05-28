package com.greedy.assistant

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*

class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var tvPrediction: TextView
    private lateinit var btnApprove: Button
    private lateinit var btnWrong: Button
    private lateinit var btnCalibrate: Button
    private lateinit var model: DynamicMarkov
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentPrediction = 0
    private var prevPrev = 0
    private var prev = 0
    private var isCalibrating = false
    private val coords = IntArray(16) // x0,y0 ... x7,y7

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        model = DynamicMarkov(applicationContext)
        serviceScope.launch { model.loadModel() }
        loadCoordinates()
        loadLastTwo()
        inflateFloatingWindow()
        updatePrediction()

        AutoReadService.onNewResult = { winner ->
            prevPrev = prev
            prev = winner
            saveLastTwo()
            updatePrediction()
        }
        AutoReadService.onRoundStart = { }
    }

    private fun inflateFloatingWindow() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_window, null)
        tvPrediction = floatingView.findViewById(R.id.tvPrediction)
        btnApprove = floatingView.findViewById(R.id.btnApprove)
        btnWrong = floatingView.findViewById(R.id.btnWrong)
        btnCalibrate = floatingView.findViewById(R.id.btnCalibrate)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // السحب
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }

        btnApprove.setOnClickListener {
            if (!isCalibrating && currentPrediction >= 0) {
                val x = coords[currentPrediction * 2]
                val y = coords[currentPrediction * 2 + 1]
                if (x != 0 || y != 0) {
                    TapService.tap(x, y)
                    Toast.makeText(this, "تم النقر على العنصر", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "لم تتم معايرة هذا العنصر", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnWrong.setOnClickListener {
            if (!isCalibrating) showCorrectionDialog()
        }

        btnCalibrate.setOnClickListener { startCalibration() }

        windowManager.addView(floatingView, params)
    }

    private fun startCalibration() {
        isCalibrating = true
        Toast.makeText(this, "اضغط على العناصر بالترتيب: جزر، ذرة، طماطم، بروكلي، روبيان، دجاج، ستيك، سمك", Toast.LENGTH_LONG).show()

        val overlay = View(this).apply { setBackgroundColor(Color.argb(180, 0, 0, 0)) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlay, params)

        var index = 0
        val names = listOf("جزر", "ذرة", "طماطم", "بروكلي", "روبيان", "دجاج", "ستيك", "سمك")
        overlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && index < 8) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                coords[index * 2] = x
                coords[index * 2 + 1] = y
                Toast.makeText(this, "${names[index]} مسجل عند ($x,$y)", Toast.LENGTH_SHORT).show()
                index++
                if (index >= 8) {
                    saveCoordinates()
                    windowManager.removeView(overlay)
                    isCalibrating = false
                    Toast.makeText(this, "تمت المعايرة بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    private fun saveCoordinates() {
        val prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE).edit()
        for (i in 0 until 8) {
            prefs.putInt("coord_${i}_x", coords[i * 2])
            prefs.putInt("coord_${i}_y", coords[i * 2 + 1])
        }
        prefs.apply()
    }

    private fun loadCoordinates() {
        val prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE)
        for (i in 0 until 8) {
            coords[i * 2] = prefs.getInt("coord_${i}_x", 0)
            coords[i * 2 + 1] = prefs.getInt("coord_${i}_y", 0)
        }
    }

    private fun updatePrediction() {
        currentPrediction = model.predict(prevPrev, prev)
        val names = arrayOf("جزر", "ذرة", "طماطم", "بروكلي", "روبيان", "دجاج", "ستيك", "سمك")
        tvPrediction.text = "توقع: ${names[currentPrediction]}"
        tvPrediction.setTextColor(Color.RED)
    }

    private fun showCorrectionDialog() {
        val items = arrayOf("جزر", "ذرة", "طماطم", "بروكلي", "روبيان", "دجاج", "ستيك", "سمك")
        AlertDialog.Builder(this)
            .setTitle("اختر العنصر الصحيح")
            .setItems(items) { _, which ->
                model.update(prevPrev, prev, which)
                updatePrediction()
                tvPrediction.setTextColor(Color.GREEN)
                Toast.makeText(this, "تم تحديث النموذج", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun loadLastTwo() {
        val prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE)
        prevPrev = prefs.getInt("prevPrev", 0)
        prev = prefs.getInt("prev", 0)
    }

    private fun saveLastTwo() {
        getSharedPreferences("greedy_bot", MODE_PRIVATE)
            .edit()
            .putInt("prevPrev", prevPrev)
            .putInt("prev", prev)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}