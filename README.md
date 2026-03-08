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
- **Auto-Reconnect Watchdog**: The app automatically attempts to reconnect to the motor controller every 5 seconds if the connection is lost.
- **Real-time Status**: Visible LED indicator (Green/Red) showing the connection status (ONLINE/OFFLINE).
- **Background Service**: Runs as a foreground service to ensure it isn't killed by the Android system.

---

## 🛠 Technology Stack

- **Android (Java)**:
    - `FloatingService`: Manages the WindowManager overlay.
    - `MotorManager`: Handles serial/Bluetooth communication logic.
    - `GestureDetector`: Supports double-tap and long-press interactions.
- **Arduino (C++)**:
    - Low-latency command processing.
    - PWM-based speed control.
- **Communication Protocol**:
    - Simple Serial Protocol (UART/Bluetooth).

---

## 📡 Communication Protocol

The Android app sends single-character string commands:

| Command | Action |
|:---:|:---|
| **U** | Move Forward (Up) |
| **D** | Move Backward (Down) |
| **L** | Turn Left |
| **R** | Turn Right |
| **S** | Stop All Motors |
| **VX** | Set Speed Level (X = 1 to 10) |

---

## ⚙️ How to Setup

### 1. Android Installation
1. Grant **"Display over other apps"** permission.
2. Grant **"Background location/Bluetooth"** permissions (if using Bluetooth).
3. Activate the service from the main screen.

### 2. Arduino Setup
1. Connect your Motor Driver (e.g., L298N) to the Arduino.
2. Connect your Bluetooth module (e.g., HC-05) to the Hardware Serial (Pins 0/1).
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
