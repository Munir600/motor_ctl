package com.example.motor_ctl;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.provider.Settings;
import android.util.Log;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;

public class FloatingService extends Service implements MotorManager.MotorDataListener {
    private static final String CHANNEL_ID = "FloatingServiceChannel";
    private WindowManager windowManager;
    private View bubbleView;
    private FloatingBubbleManager bubbleManager;
    private View controlPanelView;
    private WindowManager.LayoutParams controlParams;
    private MotorManager motorManager;

    private int speedLevel = 5;
    private boolean isExpanded = false;
    private final android.os.Handler watchdogHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            if (!motorManager.isConnected()) {
                motorManager.connect();
            }
            watchdogHandler.postDelayed(this, 5000); // Check every 5 seconds
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            try {
                startForeground(
                        1,
                        getNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                );
            } catch (Exception e) {
                Log.e("FloatingService", "Failed to start foreground service", e);
                // If we can't start as foreground, we might need to stop or show a regular notification
            }
        } else {
            startForeground(1, getNotification());
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubbleManager = new FloatingBubbleManager(this);
        bubbleView = bubbleManager.getView();
        bubbleManager.show();

        setupGestureDetector();
        setupControlPanel();

        motorManager = MotorManager.getInstance(this);
        motorManager.setListener(this);
        motorManager.connect();

        watchdogHandler.post(watchdogRunnable);
    }

    private void setupGestureDetector() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                bubbleView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                togglePanel();
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                bubbleView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                stopSelf();
            }
        });

        bubbleView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            boolean gestureHandled = gestureDetector.onTouchEvent(event);
            boolean bubbleMoved = bubbleManager.handleTouch(event);
            return gestureHandled || bubbleMoved;
        });
    }

    private void setupControlPanel() {
        @SuppressLint("InflateParams")
        View controlPanelView = LayoutInflater.from(this).inflate(R.layout.floating_controls_layout, null);
        this.controlPanelView = controlPanelView;

        controlPanelView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.performClick();
            }
            return false;
        });

        int layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        controlParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Allow touches outside
                PixelFormat.TRANSLUCENT);
        controlParams.gravity = Gravity.CENTER;
        controlParams.y = 0;

        // Touch listener for movement buttons
        View.OnTouchListener movementListener = (view, event) -> {
            int action = event.getAction();
            int id = view.getId();

            if (action == MotionEvent.ACTION_DOWN) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (id == R.id.btn_up)
                    motorManager.sendCommand("U");
                else if (id == R.id.btn_down)
                    motorManager.sendCommand("D");
                else if (id == R.id.btn_left)
                    motorManager.sendCommand("L");
                else if (id == R.id.btn_right)
                    motorManager.sendCommand("R");
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (action == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                motorManager.sendCommand("S");
                return true;
            }
            return false;
        };

        controlPanelView.findViewById(R.id.btn_up).setOnTouchListener(movementListener);
        controlPanelView.findViewById(R.id.btn_down).setOnTouchListener(movementListener);
        controlPanelView.findViewById(R.id.btn_left).setOnTouchListener(movementListener);
        controlPanelView.findViewById(R.id.btn_right).setOnTouchListener(movementListener);

        // Stop button remains clickable for safety
        controlPanelView.findViewById(R.id.btn_stop).setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            motorManager.sendCommand("S");
        });

        controlPanelView.findViewById(R.id.btn_speed_plus).setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (speedLevel < 10)
                speedLevel++;
            updateSpeedUI();
        });

        controlPanelView.findViewById(R.id.btn_speed_minus).setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (speedLevel > 1)
                speedLevel--;
            updateSpeedUI();
        });
    }

    private void updateSpeedUI() {
        ((TextView) controlPanelView.findViewById(R.id.txt_speed_level)).setText(String.valueOf(speedLevel));
        String speedText = getString(R.string.speed_label, speedLevel);
        ((TextView) controlPanelView.findViewById(R.id.txt_data)).setText(speedText);
        motorManager.setSpeed(speedLevel);
    }

    private void togglePanel() {
        try {
            if (isExpanded) {
                if (controlPanelView.getParent() != null) {
                    windowManager.removeView(controlPanelView);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Log.e("FloatingService", "Cannot show panel: Overlay permission not granted");
                    return;
                }
                if (controlPanelView.getParent() == null) {
                    windowManager.addView(controlPanelView, controlParams);
                    updateSpeedUI(); // Ensure UI is fresh
                }
            }
            isExpanded = !isExpanded;
        } catch (Exception e) {
            Log.e("FloatingService", "Error toggling panel", e);
        }
    }

    @Override
    public void onStatusChanged(String status) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (bubbleView == null) return;
            View led = bubbleView.findViewById(R.id.connection_led);
            if (led == null) return;

            if ("ONLINE".equals(status)) {
                led.setBackgroundResource(R.drawable.led_green);
                if (controlPanelView != null) {
                    TextView txtStatus = controlPanelView.findViewById(R.id.txt_status);
                    if (txtStatus != null) {
                        txtStatus.setText(getString(R.string.status_online));
                        txtStatus.setTextColor(android.graphics.Color.GREEN);
                    }
                }
            } else {
                led.setBackgroundResource(R.drawable.led_red);
                if (controlPanelView != null) {
                    TextView txtStatus = controlPanelView.findViewById(R.id.txt_status);
                    if (txtStatus != null) {
                        txtStatus.setText(getString(R.string.status_offline));
                        txtStatus.setTextColor(android.graphics.Color.RED);
                    }
                }
            }
        });
    }

    @Override
    public void onDataReceived(String data) {
        // Optional: show Arduino feedback
    }

    @Override
    public void onError(String error) {
        // Show error briefly or log
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Motor Control Service", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        watchdogHandler.removeCallbacks(watchdogRunnable);
        bubbleManager.hide();
        if (isExpanded && controlPanelView != null && controlPanelView.getParent() != null) {
            try {
                windowManager.removeView(controlPanelView);
            } catch (Exception ignored) {}
        }
        motorManager.disconnect();
        super.onDestroy();
    }
}
