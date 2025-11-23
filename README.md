# 🛡️ ResQ Beacon - Advanced Personal Safety System

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Status](https://img.shields.io/badge/Status-Stable-success?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

> **"Safety that works even when you can't."**

**ResQ Beacon** is an offline-first emergency response application designed to protect users when they are physically unable to interact with their phones (e.g., unconsciousness after an accident or restrained during an assault). It utilizes hardware sensors to proactively detect danger and automate the SOS process.

---

## 📸 App Screenshots

| **Dashboard & SOS** | **Helpline & Tools** | **Settings & Config** |
|:---:|:---:|:---:|
| ![Home](https://github.com/AK-SINGH-0/ResQ-Beacon-Android/blob/master/home.png?raw=true) | ![Tools](https://github.com/AK-SINGH-0/ResQ-Beacon-Android/blob/master/tools.png?raw=true) | ![Settings](https://github.com/AK-SINGH-0/ResQ-Beacon-Android/blob/master/settings.png?raw=true) |
| *One-tap Panic Button* | *Speed Dial & Sirens* | *Sensor Sensitivity Control* |

---

## 🚀 Key Features

### 1. 🧠 Intelligent Trigger System
* **Active Mode (Panic Button):** Big red button for immediate manual SOS.
* **Passive Mode (Fall Detection):** Uses the Accelerometer to detect high-impact forces (>50g) followed by inactivity. Automatically triggers SOS if the user is unconscious.
* **Gesture Mode (Shake-to-Alert):** Shake the phone firmly 5 times (4.5g force) to trigger SOS without unlocking the screen.

### 2. 📡 Smart SOS Messaging
Unlike standard apps that send a static text, ResQ Beacon sends a **Contextual Data Packet** via SMS:
> *"SOS! FALL DETECTED! Battery: 15% | Speed: 45km/h | Location: https://maps.google.com/?q=..."*
* **Why this matters:** Rescuers know if the victim is moving (kidnapping) and how much battery life is left.

### 3. 🛡️ Tactical Deterrents
* **Strobe Light:** Blinds/disorients attackers using the camera flash.
* **High-Decibel Siren:** Attracts immediate local attention.
* **Battery Saver Protocol:** Auto-stops high-drain features after 3 minutes to preserve battery for calls.

### 4. 🔒 Privacy & Offline Capability
* **Zero Cloud Dependency:** Works in basements, rural areas, and without internet.
* **Local Database:** Contacts are stored securely on the device using **Room Database**. No data is sold to third parties.

---

## 🛠️ Tech Stack

| Component | Technology Used | Purpose |
| :--- | :--- | :--- |
| **Architecture** | MVVM (Model-View-ViewModel) | Clean, maintainable code structure. |
| **Database** | Room (SQLite) | Storing emergency contacts locally. |
| **Sensors** | Android SensorManager | Processing Accelerometer data for G-Force calculation. |
| **Networking** | Retrofit & GSON | Logging incidents to cloud API (Optional). |
| **Background** | Foreground Services | Ensuring fall detection runs when the app is closed. |
| **UI/UX** | XML & Material Design 3 | Professional, accessible interface. |

---

## ⚙️ Installation

1.  Clone the repo:
    ```bash
    git clone [https://github.com/AK-SINGH-0/ResQ-Beacon-Android](https://github.com/AK-SINGH-0/ResQ-Beacon-Android)
    ```
2.  Open in **Android Studio**.
3.  Sync Gradle.
4.  Connect a physical device (Sensors do not work well on Emulators).
5.  Run the app.

*Note: You must grant SMS and Location permissions for the app to function.*

---

## 🔮 Future Roadmap

* **WearOS Integration:** Trigger SOS from a smartwatch.
* **Video Blackbox:** Background video recording for legal evidence.
* **AI Voice Trigger:** "Help!" keyword detection.

---

## 👨‍💻 Developer

**Ankush Kumar Singh**
* [Comming Soon LinkedIn Url ]
* https://github.com/AK-SINGH-0

---

*Disclaimer: This application is a safety tool. While rigorous testing has been conducted, the developer is not liable for hardware sensor failures in extreme conditions.*
