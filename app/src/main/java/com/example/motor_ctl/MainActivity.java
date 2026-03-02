package com.example.motor_ctl;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5469;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btn_start_service);
        Button btnStop = findViewById(R.id.btn_stop_service);

        btnStart.setOnClickListener(v -> checkPermissionAndStart());
        btnStop.setOnClickListener(v -> stopService(new Intent(MainActivity.this, FloatingService.class)));

        // Proactively check permission on startup
        if (!hasOverlayPermission()) {
            requestOverlayPermission();
        }
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    private void checkPermissionAndStart() {
        if (hasOverlayPermission()) {
            Intent intent = new Intent(MainActivity.this, FloatingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        } else {
            requestOverlayPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, requestCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (hasOverlayPermission()) {
                checkPermissionAndStart();
            } else {
                Toast.makeText(this, "Permission denied. Cannot show bubble.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}