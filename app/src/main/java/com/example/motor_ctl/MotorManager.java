package com.example.motor_ctl;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;

public class MotorManager implements SerialInputOutputManager.Listener {
    private static final String TAG = "MotorManager";
    private static final String ACTION_USB_PERMISSION = "com.example.motor_ctl.USB_PERMISSION";
    private static final int BAUDRATE = 9600;

    private static MotorManager instance;
    private final Context context;
    private final UsbManager usbManager;
    private UsbSerialPort usbSerialPort;
    private SerialInputOutputManager usbIoManager;

    private boolean isConnected = false;
    private String currentStatus = "OFFLINE";
    private String lastError = "";

    public interface MotorDataListener {
        void onStatusChanged(String status);
        void onDataReceived(String data);
        void onError(String error);
    }

    private MotorDataListener listener;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            connect();
                        }
                    } else {
                        Log.d(TAG, "permission denied for device " + device);
                        notifyError("USB Permission denied");
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                connect();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && usbSerialPort != null &&
                        usbSerialPort.getDriver().getDevice().equals(device)) {
                    disconnect();
                }
            }
        }
    };

    private MotorManager(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            this.context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            try {
                ContextCompat.registerReceiver(this.context, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static synchronized MotorManager getInstance(Context context) {
        if (instance == null) {
            instance = new MotorManager(context);
        }
        return instance;
    }

    public void setListener(MotorDataListener listener) {
        this.listener = listener;
        if (this.listener != null) {
            this.listener.onStatusChanged(currentStatus);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void connect() {
        if (isConnected) return;

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            notifyError("No USB devices found");
            return;
        }

        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDevice device = driver.getDevice();

        if (!usbManager.hasPermission(device)) {
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
            usbManager.requestPermission(device, usbPermissionIntent);
            return;
        }

        UsbDeviceConnection connection = usbManager.openDevice(device);
        if (connection == null) {
            notifyError("Failed to open connection");
            return;
        }

        usbSerialPort = driver.getPorts().get(0);
        try {
            usbSerialPort.open(connection);
            usbSerialPort.setParameters(BAUDRATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            usbIoManager = new SerialInputOutputManager(usbSerialPort, this);
            usbIoManager.start();

            isConnected = true;
            currentStatus = "ONLINE";
            if (listener != null) listener.onStatusChanged(currentStatus);
            Log.d(TAG, "USB Connected");
        } catch (IOException e) {
            notifyError("Error opening port: " + e.getMessage());
            disconnect();
        }
    }

    public void disconnect() {
        isConnected = false;
        currentStatus = "OFFLINE";
        if (usbIoManager != null) {
            usbIoManager.stop();
            usbIoManager = null;
        }
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close();
            } catch (IOException ignored) {}
            usbSerialPort = null;
        }
        if (listener != null) listener.onStatusChanged(currentStatus);
        Log.d(TAG, "USB Disconnected");
    }

    public void sendCommand(String cmd) {
        if (!isConnected || usbSerialPort == null) return;
        try {
            usbSerialPort.write((cmd + "\n").getBytes(), 100);
            Log.d(TAG, "Sent: " + cmd);
        } catch (IOException e) {
            notifyError("Write error: " + e.getMessage());
            disconnect();
        }
    }

    public void setSpeed(int level) {
        // level 1-10 -> PWM 0-255
        // lvl 1 -> 25, lvl 10 -> 255 (approx)
        int pwmValue = (int) (level * 25.5);
        if (pwmValue > 255) pwmValue = 255;
        sendCommand(String.valueOf(pwmValue));
    }

    @Override
    public void onNewData(byte[] data) {
        String message = new String(data);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) listener.onDataReceived(message);
        });
    }

    @Override
    public void onRunError(Exception e) {
        notifyError("Connection lost: " + e.getMessage());
        disconnect();
    }

    private void notifyError(String error) {
        lastError = error;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (listener != null) listener.onError(error);
        });
        Log.e(TAG, error);
    }
}
