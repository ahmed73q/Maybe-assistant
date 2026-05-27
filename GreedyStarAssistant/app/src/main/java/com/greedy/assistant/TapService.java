package com.greedy.assistant;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class TapService extends AccessibilityService {
    public static TapService instance = null;

    public static void tap(int x, int y) {
        if (instance != null) instance.performGlobalClick(x, y);
    }

    @Override
    public void onServiceConnected() {
        instance = this;
    }

    private void performGlobalClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
            dispatchGesture(builder.build(), null, null);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override
    public void onInterrupt() {}
}