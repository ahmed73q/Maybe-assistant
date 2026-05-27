package com.greedy.assistant;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private TextView tvPrediction;
    private Button btnApprove, btnWrong, btnCalibrate;
    private DynamicMarkov model;
    private int currentPrediction = 0;
    private int prevPrev = 0, prev = 0;
    private boolean isCalibrating = false;
    private int[] coords = new int[16]; // x0,y0 ... x7,y7

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        model = new DynamicMarkov(this);
        loadCoordinates();
        loadLastTwo();
        inflateFloatingWindow();
        updatePrediction();

        AutoReadService.onNewResult = winner -> {
            prevPrev = prev;
            prev = winner;
            saveLastTwo();
            updatePrediction();
        };
        AutoReadService.onRoundStart = () -> { };
    }

    private void inflateFloatingWindow() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_window, null);
        tvPrediction = floatingView.findViewById(R.id.tvPrediction);
        btnApprove = floatingView.findViewById(R.id.btnApprove);
        btnWrong = floatingView.findViewById(R.id.btnWrong);
        btnCalibrate = floatingView.findViewById(R.id.btnCalibrate);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 200;

        // سحب النافذة
        final int[] initialX = {0}, initialY = {0};
        final float[] initialTouchX = {0}, initialTouchY = {0};
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX[0] = params.x;
                    initialY[0] = params.y;
                    initialTouchX[0] = event.getRawX();
                    initialTouchY[0] = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX[0] + (int) (event.getRawX() - initialTouchX[0]);
                    params.y = initialY[0] + (int) (event.getRawY() - initialTouchY[0]);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });

        btnApprove.setOnClickListener(v -> {
            if (!isCalibrating && currentPrediction >= 0) {
                int x = coords[currentPrediction * 2];
                int y = coords[currentPrediction * 2 + 1];
                if (x != 0 || y != 0) {
                    TapService.tap(x, y);
                    Toast.makeText(this, "تم النقر على العنصر", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "لم تتم معايرة هذا العنصر", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnWrong.setOnClickListener(v -> {
            if (!isCalibrating) showCorrectionDialog();
        });

        btnCalibrate.setOnClickListener(v -> startCalibration());

        windowManager.addView(floatingView, params);
    }

    private void startCalibration() {
        isCalibrating = true;
        Toast.makeText(this, "اضغط على العناصر بالترتيب: جزر، ذرة، طماطم، بروكلي، روبيان، دجاج، ستيك، سمك", Toast.LENGTH_LONG).show();

        View overlay = new View(this);
        overlay.setBackgroundColor(Color.argb(180, 0, 0, 0));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(overlay, params);

        final int[] index = {0};
        final String[] names = {"جزر", "ذرة", "طماطم", "بروكلي", "روبيان", "دجاج", "ستيك", "سمك"};
        overlay.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && index[0] < 8) {
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                coords[index[0] * 2] = x;
                coords[index[0] * 2 + 1] = y;
                Toast.makeText(this, names[index[0]] + " مسجل عند (" + x + "," + y + ")", Toast.LENGTH_SHORT).show();
                index[0]++;
                if (index[0] >= 8) {
                    saveCoordinates();
                    windowManager.removeView(overlay);
                    isCalibrating = false;
                    Toast.makeText(this, "تمت المعايرة بنجاح", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });
    }

    private void saveCoordinates() {
        SharedPreferences prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 8; i++) {
            editor.putInt("coord_" + i + "_x", coords[i * 2]);
            editor.putInt("coord_" + i + "_y", coords[i * 2 + 1]);
        }
        editor.apply();
    }

    private void loadCoordinates() {
        SharedPreferences prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE);
        for (int i = 0; i < 8; i++) {
            coords[i * 2] = prefs.getInt("coord_" + i + "_x", 0);
            coords[i * 2 + 1] = prefs.getInt("coord_" + i + "_y", 0);
        }
    }

    private void updatePrediction() {
        currentPrediction = model.predict(prevPrev, prev);
        String[] names = {"جزر", "ذرة", "طماطم", "بروكلي", "روبيان", "دجاج", "ستيك", "سمك"};
        tvPrediction.setText("توقع: " + names[currentPrediction]);
        tvPrediction.setTextColor(Color.RED);
    }

    private void showCorrectionDialog() {
        final String[] items = {"جزر", "ذرة", "طماطم", "بروكلي", "روبيان", "دجاج", "ستيك", "سمك"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر العنصر الصحيح");
        builder.setItems(items, (dialog, which) -> {
            model.update(prevPrev, prev, which);
            updatePrediction();
            tvPrediction.setTextColor(Color.GREEN);
            Toast.makeText(this, "تم تحديث النموذج", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    private void loadLastTwo() {
        SharedPreferences prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE);
        prevPrev = prefs.getInt("prevPrev", 0);
        prev = prefs.getInt("prev", 0);
    }

    private void saveLastTwo() {
        SharedPreferences prefs = getSharedPreferences("greedy_bot", MODE_PRIVATE);
        prefs.edit().putInt("prevPrev", prevPrev).putInt("prev", prev).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}