package com.greedy.assistant;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOverlay = findViewById(R.id.btnOverlay);
        Button btnAccessibility = findViewById(R.id.btnAccessibility);
        Button btnStart = findViewById(R.id.btnStart);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            btnOverlay.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
        } else {
            btnOverlay.setEnabled(false);
            btnOverlay.setText("✓ إذن النافذة العائمة");
        }

        btnAccessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        btnStart.setOnClickListener(v -> {
            startService(new Intent(this, FloatingWindowService.class));
            startService(new Intent(this, AutoReadService.class));
            finish();
        });
    }
}