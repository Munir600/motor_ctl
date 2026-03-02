package com.example.motor_ctl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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
        startForeground(1, getNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        motorManager = MotorManager.getInstance(this);
        motorManager.setListener(this);
        motorManager.connect();

        bubbleManager = new FloatingBubbleManager(this);
        bubbleView = bubbleManager.getView();
        bubbleManager.show();

        setupGestureDetector();
        setupControlPanel();

        watchdogHandler.post(watchdogRunnable);
    }

    private void setupGestureDetector() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                bubbleView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                togglePanel();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                bubbleView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                stopSelf();
            }
        });

        bubbleView.setOnTouchListener((v, event) -> {
            boolean gestureHandled = gestureDetector.onTouchEvent(event);
            boolean bubbleMoved = bubbleManager.handleTouch(event);
            return gestureHandled || bubbleMoved;
        });
    }

    private void setupControlPanel() {
        controlPanelView = LayoutInflater.from(this).inflate(R.layout.floating_controls_layout, null);

        controlPanelView.setOnTouchListener((v, event) -> {
            // Prevent clicks from passing through if needed
            return false;
        });

        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        controlParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // Allow touches outside
                PixelFormat.TRANSLUCENT);
        controlParams.gravity = Gravity.CENTER;
        controlParams.y = 0;

        // Button listeners with Haptic Feedback
        View.OnClickListener listener = view -> {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            int id = view.getId();
            if (id == R.id.btn_up)
                motorManager.sendCommand("U");
            else if (id == R.id.btn_down)
                motorManager.sendCommand("D");
            else if (id == R.id.btn_left)
                motorManager.sendCommand("L");
            else if (id == R.id.btn_right)
                motorManager.sendCommand("R");
            else if (id == R.id.btn_stop)
                motorManager.sendCommand("S");
        };

        controlPanelView.findViewById(R.id.btn_up).setOnClickListener(listener);
        controlPanelView.findViewById(R.id.btn_down).setOnClickListener(listener);
        controlPanelView.findViewById(R.id.btn_left).setOnClickListener(listener);
        controlPanelView.findViewById(R.id.btn_right).setOnClickListener(listener);
        controlPanelView.findViewById(R.id.btn_stop).setOnClickListener(listener);

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
        ((TextView) controlPanelView.findViewById(R.id.txt_data)).setText("Speed: " + speedLevel);
        motorManager.setSpeed(speedLevel);
    }

    private void togglePanel() {
        if (isExpanded) {
            windowManager.removeView(controlPanelView);
        } else {
            windowManager.addView(controlPanelView, controlParams);
        }
        isExpanded = !isExpanded;
    }

    @Override
    public void onStatusChanged(String status) {
        View led = bubbleView.findViewById(R.id.connection_led);
        TextView txtStatus = controlPanelView.findViewById(R.id.txt_status);

        if ("ONLINE".equals(status)) {
            led.setBackgroundResource(R.drawable.led_green);
            txtStatus.setText("ONLINE");
            txtStatus.setTextColor(0xFF00FF00);
        } else {
            led.setBackgroundResource(R.drawable.led_red);
            txtStatus.setText("OFFLINE");
            txtStatus.setTextColor(0xFFFF0000);
        }
    }

    @Override
    public void onDataReceived(String data) {
        // Optional: show Arduino feedback in a different way or log it
        // and avoid overwriting the 'Speed: X' text set by the buttons
    }

    @Override
    public void onError(String error) {
        // Show error briefly or log
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Motor Control Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Motor Control Active")
                .setContentText("Floating overlay is running")
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
        if (isExpanded)
            windowManager.removeView(controlPanelView);
        motorManager.disconnect();
        super.onDestroy();
    }
}
