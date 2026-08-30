# Extra Dim Quick Settings Tile App

A lightweight, headless Android application that provides a Quick Settings (QS) Tile with custom **Sunglasses Icon**, **Dimming Intensity Control**, and **Instant Direct IPC** to toggle Android's native **Extra Dim** feature (`reduce_bright_colors_activated`).

## Features & Highlights
- 🕶️ **Custom Sunglasses Icon:** Distinctive, custom-designed vector icon for Quick Settings.
- ⚡ **Direct IPC (Zero Delay):** Uses `Settings.Secure.putInt()` directly via `WRITE_SECURE_SETTINGS` permission with zero shell lag.
- 🎚️ **Long-Press Intensity Card:** Long-press the tile to open a sleek dialog with a live **0–100% Intensity Slider** (`reduce_bright_colors_level`) and On/Off toggle.
- 🏷️ **Creator Credits & Subtitle:** Embedded subtitle (`by Bhanu`) on the tile and Telegram credit link (`@darkdevil7773`) in the settings card.
- 🚫 **100% Headless:** No clutter in app drawer or home screen.


---

## Project Structure
```
.
├── app
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src
│       └── main
│           ├── AndroidManifest.xml
│           ├── java
│           │   └── com
│           │       └── bhanu
│           │           └── extradimtile
│           │               └── ExtraDimTileService.java
│           └── res
│               ├── drawable
│               │   └── ic_dim.xml
│               └── values
│                   ├── colors.xml
│                   └── strings.xml
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
└── README.md
```

---

## Build & Installation

### 1. Build APK
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Grant Required Permission (Non-Rooted & Rooted Setup)

Because Android protects system-level secure settings, you need to grant the `WRITE_SECURE_SETTINGS` permission **once** via ADB. The app does **not** need root to run.

#### Step-by-Step for Non-Rooted Devices:

1. **Enable Developer Options on your phone:**
   - Go to **Settings > About Phone / About Device**.
   - Tap **Build Number** 7 times until you see *"You are now a developer!"*.

2. **Enable USB Debugging:**
   - Go to **Settings > System / Additional Settings > Developer Options**.
   - Turn **ON** **USB Debugging**.
   - *(OEM specific)*:
     - **Realme / Oppo / OnePlus (ColorOS / Realme UI / OxygenOS):** Enable **Disable permission monitoring**.
     - **Xiaomi / Redmi / Poco (MIUI / HyperOS):** Enable **USB debugging (Security settings)** *(requires SIM/Mi account)*.

3. **Connect to PC & Authorize:**
   - Connect your phone to your computer via USB.
   - On your phone prompt: Select **"Always allow from this computer"** and tap **Allow**.

4. **Run the ADB Grant Command:**
   ```bash
   adb shell pm grant com.bhanu.extradimtile android.permission.WRITE_SECURE_SETTINGS
   ```

5. *(Optional verification)*:
   ```bash
   adb shell dumpsys package com.bhanu.extradimtile | grep WRITE_SECURE_SETTINGS
   ```
   *Expected output: `android.permission.WRITE_SECURE_SETTINGS: granted=true`*

---

#### Alternative for Rooted Devices (via su shell / Termux):
```bash
adb shell su -c "pm grant com.bhanu.extradimtile android.permission.WRITE_SECURE_SETTINGS"
```
*(Or directly in Termux on device: `su -c "pm grant com.bhanu.extradimtile android.permission.WRITE_SECURE_SETTINGS"`).*

---

### 4. Add the Quick Settings Tile
1. Swipe down twice from the top of your screen to open the full Quick Settings panel.
2. Tap the **Edit** (pencil) icon.
3. Scroll down to find the **Extra Dim** tile.
4. Drag it up into your active tiles list.
5. Tap the tile to toggle Extra Dim on or off instantly with zero lag.

# extradim
