package com.example.motor_ctl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.provider.Settings;
import android.util.Log;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public class FloatingBubbleManager {
    private final Context context;
    private final WindowManager windowManager;
    private final View bubbleView;
    private final WindowManager.LayoutParams params;

    private int screenWidth;
    private int screenHeight;

    private static final String PREFS_NAME = "FloatingBubblePrefs";
    private static final String KEY_X = "bubble_x";
    private static final String KEY_Y = "bubble_y";

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;

    public FloatingBubbleManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.bubbleView = LayoutInflater.from(context).inflate(R.layout.floating_bubble_layout, null);

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;

        // Load saved position
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        params.x = prefs.getInt(KEY_X, 0);
        params.y = prefs.getInt(KEY_Y, 100);

        updateScreenDimensions();
    }

    public void show() {
        if (bubbleView.getParent() == null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    Log.e("FloatingBubbleManager", "Cannot show bubble: Overlay permission not granted");
                    return;
                }
                windowManager.addView(bubbleView, params);
            } catch (WindowManager.BadTokenException e) {
                Log.e("FloatingBubbleManager", "Failed to add bubble view: BadTokenException", e);
            } catch (Exception e) {
                Log.e("FloatingBubbleManager", "Unexpected error showing bubble", e);
            }
        }
    }

    public void hide() {
        if (bubbleView.getParent() != null) {
            try {
                windowManager.removeView(bubbleView);
            } catch (Exception e) {
                Log.e("FloatingBubbleManager", "Error removing bubble view", e);
            }
        }
    }

    public View getView() {
        return bubbleView;
    }

    public boolean handleTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;

            case MotionEvent.ACTION_MOVE:
                params.x = (int) (initialX + (event.getRawX() - initialTouchX));
                params.y = (int) (initialY + (event.getRawY() - initialTouchY));
                try {
                    windowManager.updateViewLayout(bubbleView, params);
                } catch (Exception e) {
                    Log.e("FloatingBubbleManager", "Error updating bubble layout", e);
                }
                return true;

            case MotionEvent.ACTION_UP:
                snapToEdge();
                return true;
        }
        return false;
    }

    private void savePosition() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_X, params.x)
                .putInt(KEY_Y, params.y)
                .apply();
    }

    private void snapToEdge() {
        updateScreenDimensions();
        int targetX = (params.x + bubbleView.getWidth() / 2 < screenWidth / 2) ? 0
                : screenWidth - bubbleView.getWidth();

        FloatPropertyCompat<View> xProperty = new FloatPropertyCompat<View>("x") {
            @Override
            public float getValue(View object) {
                return params.x;
            }

            @Override
            public void setValue(View object, float value) {
                params.x = (int) value;
                try {
                    windowManager.updateViewLayout(bubbleView, params);
                } catch (Exception e) {
                    Log.e("FloatingBubbleManager", "Error updating bubble layout during snap", e);
                }
            }
        };

        SpringAnimation springX = new SpringAnimation(bubbleView, xProperty, targetX);
        springX.addEndListener((animation, canceled, value, velocity) -> savePosition());
        springX.getSpring().setStiffness(SpringForce.STIFFNESS_LOW)
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        springX.start();

        savePosition();
    }

    private void updateScreenDimensions() {
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
    }
}
