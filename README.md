# Motor Control System (Floating UI & Arduino)

This project is a comprehensive solution for controlling motors remotely via an Android application. It features a unique **Floating Overlay UI** that allows you to control your robot or machine while using other apps, and a responsive **Arduino Backend** for real-time execution.

---

## 🚀 Key Features

### 1. Floating Control Panel (Bubbles)
- **Always on Top**: Access motor controls from any screen.
- **Toggleable Display**: Double-tap the floating bubble to show/hide the full control panel.
- **Haptic Feedback**: High-quality vibration feedback on every interaction for a premium feel.

### 2. Precise Touch-Based Control
- **Joystick Logic**: The motor moves only while the button is pressed (`ACTION_DOWN`) and stops immediately upon release (`ACTION_UP`).
- **Directional Controls**: Dedicated buttons for Up, Down, Left, Right, and an Emergency Stop.
- **Speed Management**: 10-level speed control with real-time updates.

### 3. Smart Connectivity
- **Auto-Reconnect Watchdog**: The app automatically attempts to reconnect to the USB device every 5 seconds if the connection is lost.
- **Real-time Status**: Visible LED indicator (Green/Red) showing the USB connection status (ONLINE/OFFLINE).
- **Background Service**: Runs as a foreground service to ensure it isn't killed by the Android system.

---

## 🛠 Technology Stack

- **Android (Java)**:
    - `FloatingService`: Manages the WindowManager overlay.
    - `MotorManager`: Handles **USB Serial** communication via OTG.
    - `GestureDetector`: Supports double-tap and long-press interactions.
- **Arduino (C++)**:
    - Low-latency command processing via Serial.
    - PWM-based speed control.
- **Communication Protocol**:
    - **USB Serial (UART)** over OTG.
    - Baud Rate: 9600.

---

## 📡 Communication Protocol

The Android app sends commands followed by a newline (`\n`):

| Command | Action |
|:---:|:---|
| **U** | Move Forward (Up) |
| **D** | Move Backward (Down) |
| **L** | Turn Left |
| **R** | Turn Right |
| **S** | Stop All Motors |
| **[0-255]** | Raw PWM value for speed (sent when speed is adjusted) |

---

## ⚙️ How to Setup

### 1. Android Installation
1. Grant **"Display over other apps"** permission in settings.
2. Connect the Arduino to your phone using a **USB OTG Adapter**.
3. Accept the USB permission prompt when it appears.
4. Activate the service from the main screen.

### 2. Arduino Setup
1. Connect your Motor Driver (e.g., L298N) to the Arduino.
2. Connect the Arduino's USB cable to the phone's OTG adapter.
3. Upload the provided code in `arduino_motor_code.txt`.

---

## 🛡 Security & Accessibility
- **Target SDK**: Compatible with modern Android versions (up to Android 15).
- **Accessibility Ready**: Implements `performClick()` for screen readers and accessibility tools.
- **Foreground Notification**: Always shows a notification to the user when the service is active, respecting privacy and transparency.

## 📝 Developer Credits

Developed with ❤️ by **Salahuddin Hamran** and **Munir Zaid**
- **Contact**: +967 771446691  & +967 771974387
- **Role**: Lead Developer & Designer

---
