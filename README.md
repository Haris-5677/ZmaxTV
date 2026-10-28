# 📺 Zamax TV Home

A customized, ad-free, high-performance **Android TV Launcher** designed for TCL and Android 9+ Smart TVs. Features real-time multi-timezone world clocks, dynamic currency exchange rates via Google Finance, integrated Google Calendar sync, and full D-pad remote navigation.

---

## ✨ Features

- **Luxury Night Villa Theme**: High-contrast glassmorphic dashboard cards (`215dp × 84dp`) over high-resolution modern villa architecture.
- **Top-Left 2x2 Dashboard**:
  - 🇮🇳 **India (IST)**: Live `USD / INR` exchange rate from Google Finance.
  - 🇦🇪 **Dubai (GST)**: Live `AED / INR` exchange rate from Google Finance.
  - 🇸🇦 **Saudi Arabia (AST)**: Live `SAR / INR` exchange rate from Google Finance.
  - 🌤️ **Bangalore**: Live weather, temperature, and dynamic condition metrics.
- **📅 Google Calendar Widget**:
  - Automatically queries the Android system calendar provider (`CalendarContract.Instances`).
  - Seamlessly syncs events from the TV's logged-in Google Play accounts (`hassan2zam@gmail.com`).
  - Displays dynamic mini month grid with today highlighted and upcoming agenda.
- **TV Remote Optimized**:
  - 100% spatial D-pad navigation (Up, Down, Left, Right, OK, Back).
  - High-contrast visual focus ring on active cards.
- **Ultra Lightweight**:
  - App size is just **~561 KB** (starts instantly on Realtek RT2841 and low-RAM TV processors).
- **Debloated & Clean**:
  - Includes TV debloating tools and 1-click recovery scripts (`restore-tv.bat`, `restore-tv.ps1`).

---

## 🚀 Quick Install (ADB)

Download the pre-compiled, signed APK from [`release/HomeScreen.apk`](release/HomeScreen.apk):

```bash
# Connect to your TV over Wi-Fi
adb connect <TV_IP_ADDRESS>:5555

# Install the launcher
adb install -r release/HomeScreen.apk

# Grant Calendar read permission for Google Calendar sync
adb shell pm grant com.gathin.tvhome android.permission.READ_CALENDAR

# Set as default TV launcher
adb shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
```

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 (e.g. Eclipse Temurin or OpenJDK 17)
- Android SDK Platform 34

### Build Release APK
```bash
# Clone the repository
git clone https://github.com/<your-username>/zamax-tv-home.git
cd zamax-tv-home

# Build signed release APK
./gradlew assembleRelease
```
The output APK is generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📂 Repository Structure

```text
zamax-tv-home/
├── app/
│   ├── src/main/
│   │   ├── java/com/gathin/tvhome/
│   │   │   ├── MainActivity.java           # Launcher main activity & UI coordinator
│   │   │   ├── CalendarFetcher.java        # Google Calendar sync engine (CalendarContract)
│   │   │   ├── CalendarWidgetManager.java  # Mini-calendar grid & agenda view
│   │   │   ├── CurrencyFetcher.java        # Google Finance live rate scraper
│   │   │   └── ...                         # App adapter, grid, focus management
│   │   └── res/                            # Layouts, vector drawables, themes, strings
│   └── build.gradle                        # App dependencies & R8 configuration
├── release/
│   └── HomeScreen.apk                      # Pre-compiled, signed production APK (~561 KB)
├── tools/
│   ├── tv.py                               # Pure-Python ADB over TCP transport client
│   └── b64push.py                          # Chunked base64 file transfer utility
├── restore-point/
│   └── stable-zamax-tv/                    # Complete TV status snapshot & restore scripts
├── push-to-github.bat                      # 1-click script to push to your GitHub repo
├── push-to-github.ps1                      # PowerShell GitHub push automation
├── build.gradle                            # Project build script
├── settings.gradle                         # Gradle settings
└── README.md                               # Project documentation
```

---

## 🔄 Restore & Backup Scripts

If your TV receives an OTA update or you wish to re-apply this configuration:
- **Windows**: Double-click `restore-point/stable-zamax-tv/restore-tv.bat`
- **PowerShell**:
  ```powershell
  .\restore-point\stable-zamax-tv\restore-tv.ps1 -TvHost "<TV_IP>"
  ```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
