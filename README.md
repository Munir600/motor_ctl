# Motor Control Android App 🚀

A professional Android application designed to control and monitor motors via USB Serial communication with Arduino. The app features a high-performance floating overlay (bubble) that remains accessible over any other application.

## ✨ Key Features

- **Floating Overlay (Bubble)**: A persistent control interface that stays on top of all apps.
- **Physics-based Animations**: Uses `SpringAnimation` for smooth, life-like edge snapping (Magnet effect).
- **Advanced Gestures**:
    - `Double Tap`: Toggles the control panel visibility.
    - `Long Press`: Closes the service completely.
    - `Drag & Drop`: Smooth movement with automatic edge magnetizing.
- **Glassmorphism UI**: A modern, sleek, and highly transparent user interface for the control panel.
- **USB Serial Integration**:
    - Supports **Arduino**, **CH340**, **FTDI**, **CP210x**, and more.
    - Uses `usb-serial-for-android` for robust communication.
- **Fail-Safe Mechanism**:
    - **Watchdog**: Periodically checks and re-establishes the USB connection if lost.
    - **Boot Start**: Automatically starts the floating service when the device boots up.
- **Real-time Monitoring**: Displays connection status (ONLINE/OFFLINE) and speed levels.
- **Precise Control**: Up, Down, Left, Right, Stop commands, and 10 levels of speed control.

## 🛠️ Tech Stack

- **Language**: Java
- **UI Framework**: Native Android (XML)
- **Overlay**: WindowManager API
- **Animations**: AndroidX Dynamic Animation (Spring)
- **Serial Library**: [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android)
- **Persistence**: SharedPreferences for bubble position storage.

## 📦 Installation & Setup

1.  **Clone the project**:
    ```bash
    git clone https://github.com/YOUR_USERNAME/motor_ctl.git
    ```
2.  **Build**: Open in Android Studio and sync Gradle (requires JitPack for USB library).
3.  **Permissions**:
    - Grant **Display over other apps** (System Alert Window) on first run.
    - Ensure **Notifications** and **USB** permissions are allowed.
4.  **Hardware**: Connect your Arduino via an OTG cable. The app will automatically detect and prompt for USB permission.

## 📝 Developer Credits

Developed with ❤️ by **Salahuddin Hamran**
- **Contact**: +967 771446691
- **Role**: Lead Developer & Designer

---
*This project was developed to provide a seamless and aesthetic interface for industrial or hobbyist motor control applications.*
