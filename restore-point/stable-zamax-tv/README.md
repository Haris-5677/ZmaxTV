# Restore Point: stable Zamax TV

**Created**: 2026-10-28 17:02:53 IST  
**Device**: TCL UnionTV (Realtek RT2841, 1280x720 213dpi)  
**OS**: Android 9 Pie (API Level 28, PPR2.180905.006.A1)  
**Target IP**: `192.168.1.11:5555`

---

## 1. Summary of Captured State

- **Default Home Launcher**: `com.gathin.tvhome` (v3.1, build code 10).
  - Subtitle branding: `"Zamax"`.
  - 2x2 Clocks: India (IST), Dubai (GST), Saudi Arabia (AST), and Bangalore live weather.
  - Live Currency Exchange Rates dynamically fetched from Google Finance:
    - USD vs INR
    - AED vs INR
    - SAR vs INR
  - Aligned 215dp card widths (no cropping).
  - High-res modern luxury villa night wallpaper.
- **Debloated Status**:
  - 41 bloatware, tracking, and telemetry packages cleanly disabled (including `com.tcl.usercenter`, `com.tcl.bi`, `com.tcl.tvweishi`, `com.google.android.play.games`, etc.).
  - Cache trimmed, unneeded temporary APKs removed.
- **Included APKs**:
  - `apks/HomeScreen.apk` (Zamax TV launcher)

---

## 2. Saved System State Files

| File | Description |
|---|---|
| `RESTORE_POINT_MANIFEST.json` | Complete metadata and package information |
| `current-launcher.txt` | Default HOME intent handler (`com.gathin.tvhome`) |
| `current-activity.txt` | Active window and focused activity |
| `packages-enabled.txt` | Complete list of all active packages |
| `packages-disabled.txt` | Exact list of all 41 disabled bloatware packages |
| `packages.txt` | Full package paths and details |
| `packages-third-party.txt` | Sideloaded and user applications |
| `settings-global.txt` | Android Global settings dump |
| `settings-secure.txt` | Android Secure settings dump |
| `settings-system.txt` | Android System settings dump |
| `getprop.txt` | Complete Android build properties |
| `hardware-and-memory.txt` | Display dimensions, memory usage, and storage partition state |
| `package-zamax-launcher.txt` | Package dump for `com.gathin.tvhome` |

---

## 3. How to Restore

To restore the TV to this exact status at any time, run either:

### Windows PowerShell:
```powershell
.\restore-tv.ps1 -TvHost "192.168.1.11"
```

### Windows Command Prompt / Double-click:
Double-click `restore-tv.bat`.
