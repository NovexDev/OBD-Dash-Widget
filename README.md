# OBD Dash Widget

**OBD Dash Widget** brings live, customizable OBD-II gauge widgets directly to your Android homescreen. Connect your Android device to your vehicle's OBD-II port via any ELM327-compatible Bluetooth, BLE, USB, or Wi-Fi adapter, and monitor real-time vehicle parameters (RPM, Speed, Coolant Temperature, Engine Load, Fuel Level, and more) right from your launcher.

> **Credits, Origins & AI Assistance**  
> * **Original Project**: OBD Dash Widget is derived from and based on the open-source project [AndrOBD](https://github.com/fr3ts0n/AndrOBD) created by [fr3ts0n](https://github.com/fr3ts0n). Special thanks to the original author for creating the foundational OBD-II protocol and communication stack!  
> * ☕ **Support the original developer**: [PayPal Donation Link](https://PayPal.me/fr3ts0n)  
> * **AI Assistance**: This modernized, widget-focused fork was created and refactored with the assistance of AI development tools.

---

## 🚗 Features

### 1. Homescreen Launcher Widgets
* **Analog Gauge (Zeiger)**: High-DPI circular dashboard dials with sweeping red needles, scale numbers (`x1000`, `km/h`, `°C`), and distinct redline warning arcs matching modern car instrument clusters.
* **Digital Readout (Zahl)**: Clean, high-contrast numerical display with PID label and physical units.
* **Trend Chart (Verlauf)**: Real-time sparkline graph showing live measurement curves over customizable time windows (1 min, 2 min, 5 min, 10 min).
* **Square 1:1 Aspect Ratio**: Sleek dark card containers matching modern automotive dashboards.

### 2. Live Background Updating
* Powered by a persistent Foreground Service (`ObdForegroundService`).
* Updates widget displays in the background smoothly at 3 Hz (~330 ms) while preventing system IPC saturation.
* Interactive notification bar with live status and one-tap **Connect / Disconnect** actions.

### 3. Read-Only ECU Safety Protection
* All ECU write and fault-code clearing commands (`0x04` / `0x08`) are completely disabled on the communication layer to guarantee 100% safe, read-only vehicle monitoring.

### 4. OBD-II Connectivity
* **Bluetooth / Bluetooth LE (BLE)**
* **USB** (FTDI / CH340 / CP2102)
* **Wi-Fi / WLAN** (TCP/IP)
* **Built-in Demo Mode**: Simulates live driving data (1000 ➔ 6000 RPM, 0 ➔ 120 km/h) for instant testing without a vehicle.

### 5. Multilingual Support
* Full internationalization supporting English, German, Spanish, French, Italian, Russian, Portuguese, Dutch, Finnish, Czech, Hungarian, Korean, Turkish, and more.

---

## 🛠️ Build & Setup

### Requirements
* Android 6.0+ (API 23) or higher.
* Android Studio Jellyfish / Ladybug or newer.
* JDK 17.

### Building
```bash
git clone https://github.com/NovexDev/OBD-Dash-Widget.git
cd OBD-Dash-Widget
./gradlew assembleDebug
```

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0** (GPL-3.0) - see the [LICENSE](LICENSE) file for details.
