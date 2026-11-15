# 🌌 AstroFrame (Android TV)

Automated **NASA Cosmos** & **The Met Museum** 4K wallpaper updater & native screensaver with contextual art captions for **Android TV** and **Projectivy Launcher**.

<p align="center">
  <a href="https://github.com/mandy321/AstroFrame/releases/latest"><img src="https://img.shields.io/github/v/release/mandy321/AstroFrame?style=for-the-badge&color=7C3AED&label=LATEST%20RELEASE" alt="Latest Release"></a>
  <a href="https://github.com/mandy321/AstroFrame/releases"><img src="https://img.shields.io/github/downloads/mandy321/AstroFrame/total?style=for-the-badge&logo=github&color=2563EB&label=TOTAL%20DOWNLOADS" alt="Total Downloads"></a>
  <a href="https://github.com/mandy321/AstroFrame/releases/latest"><img src="https://img.shields.io/github/downloads/mandy321/AstroFrame/latest/total?style=for-the-badge&color=059669&label=LATEST%20DOWNLOADS" alt="Latest Downloads"></a>
  <a href="https://github.com/mandy321/AstroFrame/stargazers"><img src="https://img.shields.io/github/stars/mandy321/AstroFrame?style=for-the-badge&logo=star&color=F59E0B&label=STARS" alt="Stars"></a>
  <a href="https://github.com/mandy321/AstroFrame"><img src="https://komarev.com/ghpvc/?username=mandy321-astroframe&style=for-the-badge&color=0EA5E9&label=REPO%20VIEWS" alt="Repo Views"></a>
</p>

---

## ✨ Features

* **Dual Art Collections**:
  * 🚀 **NASA Cosmos (APOD)**: Daily and historic 4K astronomical discoveries from NASA.
  * 🎨 **The Met Museum**: High-resolution public domain masterpieces (Van Gogh, Monet, Rembrandt, Vermeer, Hokusai, Cézanne).
  * 🌌 **Shuffle / Mix**: Alternates between deep space discoveries and world-class fine art.
* **Large 10-Foot TV Typography**: Bold 33sp headline, 21sp readable summary, and source badges (NASA Cyan / Museum Gold) legible from across the room.
* **Easy Remote Dialog Pickers**: Quickly select intervals (15m, 30m, 1h, 3h, 6h, 24h) and art sources via native D-pad choice dialogs.
* **Automatic Storage Cleanup**: Auto-deletes wallpapers older than 48 hours and caps storage at max 10 images (under 25MB total).
* **Projectivy Launcher Native Integration**: Seamlessly saves wallpapers to `/sdcard/Pictures/AstroFrame/`.
* **Privacy-First Live Analytics**: Lightweight PostHog telemetry to monitor active TV devices, wallpaper updates, and screensaver performance with zero personal tracking.

---

## 🛠️ How to Connect with Projectivy Launcher Premium

Once AstroFrame is installed on your Android TV:

### Step 1: Open AstroFrame on your TV
1. Launch **AstroFrame** from your TV apps menu.
2. Click **"Update Wallpaper Now"**.
3. You will see a live preview of the downloaded NASA wallpaper with the info card rendered.
4. (Optional) Click **"Overlay Position"** to adjust where the card sits (e.g. `Top Left` if your Projectivy app rows are at the bottom).

### Step 2: Configure Projectivy Launcher
1. On your Android TV home screen, open **Projectivy Settings** (the gear icon ⚙️).
2. Go to **Appearance** $\rightarrow$ **Wallpaper**.
3. Under **Wallpaper Type**, select **"User Defined Folder"** or **"Image (slideshow)"**.
4. In the settings list, select the **Uri** field and enter the exact URI:
   ```text
   file:///storage/emulated/0/Pictures/AstroFrame
   ```
   *(Note: Type the `file:///` prefix and press **Enter / OK** on your TV remote to save)*.
5. In Projectivy's **Slideshow interval**, set it to **30 minutes** (to match AstroFrame's schedule).
6. Return to your home screen—your background is now an astronomical museum frame!

---

## 🖼️ Using as a Screensaver / Ambient Frame

You can turn AstroFrame into an ambient screensaver using either of these two methods:

### Method 1: Projectivy Native Ambient Mode (Zero Extra Apps)
Projectivy Launcher has built-in idle detection that can hide your apps and turn your TV into a clean art frame:
1. Open **Projectivy Settings** $\rightarrow$ **Appearance** $\rightarrow$ **Wallpaper**.
2. Set **Display mode** to **"Always visible"**.
3. Set **Image transition** to **"Zoom"** or **"Pan & Zoom"** (creates a slow, cinematic Ken Burns camera drift across the cosmos or artwork).
4. When left idle on the home screen, Projectivy's UI rows fade away, leaving only the full-screen 4K artwork and readable caption card!

### Method 2: Native Android TV Screensaver (Works Inside SmartTube, YouTube, Everywhere!)
AstroFrame now includes Android's native **`DreamService` (Screensaver)** built right into the app:
1. Open AstroFrame on your TV.
2. Under **"Screensaver Photo Rotation"**, choose your preferred rotation speed:
   * **None (Static)**: Holds the latest wallpaper image without rotating.
   * **30 Seconds**, **1 Minute**, **2 Minutes**, or **5 Minutes**: Smoothly cross-fades through all stored artworks.
3. Click the cyan **"Set as TV Screensaver 📺"** button in AstroFrame (or go to Android TV Settings $\rightarrow$ **Device Preferences / System** $\rightarrow$ **Screensaver**).
4. Select **"AstroFrame Screensaver"** from the list.
5. Set your idle timeout (e.g. *5 minutes*).
6. Whenever you pause SmartTube, watch a movie, or leave any app idle, AstroFrame's 4K cosmos and museum artworks will smoothly take over full-screen! Pressing any button on your remote wakes the TV instantly.

---

## 📺 Device Compatibility Checklist

| Device | Installation | Wallpaper & Projectivy | System Screensaver |
| :--- | :--- | :--- | :--- |
| **NVIDIA Shield TV** (Shield / Pro) | Standard sideload / ADB | ⭐ 100% Native | 100% Native in TV Settings |
| **Google TV** (Chromecast, Sony, TCL, Hisense) | Standard sideload / ADB | ⭐ 100% Native (via Projectivy / FLauncher) | 100% Native in TV Settings |
| **Amazon Fire TV Stick** (Fire OS 6, 7, 8) | Via Downloader app or ADB | ⭐ 100% Native | Native via Projectivy Idle (or 1-line ADB command) |
| **Generic Android TV / Projectors** (Mi Box, Mecool, Formuler, XGIMI) | Standard sideload / ADB | ⭐ 100% Native | 100% Native in TV Settings |

### Device Notes:

* **NVIDIA Shield & Google TV**:
  * Fully plug-and-play. AstroFrame's native `DreamService` automatically appears in Android TV's *Settings $\rightarrow$ Device Preferences / System $\rightarrow$ Screensaver*.

* **Amazon Fire TV Stick**:
  * The background wallpaper updater and Projectivy Launcher work 100% natively on Fire OS.
  * To use the system-wide screensaver on Firestick (bypassing Amazon's ad screensaver), you can either:
    1. **Method A (Zero ADB)**: Use Projectivy's built-in Idle Mode on your home screen.
    2. **Method B (System-Wide via ADB)**: Run this single ADB command once to set AstroFrame as your default Firestick screensaver:
       ```bash
       adb shell settings put secure screensaver_components com.astroframe.tv/.screensaver.AstroDreamService
       adb shell settings put secure screensaver_enabled 1
       ```

---

## 🚀 Building & Installing

### Prerequisites
* JDK 17 or higher
* Android SDK (API 34+)
* ADB enabled on your Android TV (*Settings $\rightarrow$ Device Preferences $\rightarrow$ Developer Options $\rightarrow$ USB/Network Debugging*)

### 1. Build the APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### 2. Install on Android TV via ADB over Wi-Fi
Connect your computer to your TV's IP address:
```bash
adb connect <YOUR_TV_IP_ADDRESS>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🔑 NASA API Key & Authentication

### 1. Works Out-of-the-Box (No API Key Required!)
* By default, **AstroFrame requires zero API keys or accounts**.
* It connects directly to the official **`science.nasa.gov`** open platform and **The Met Museum Open Access API**, which are publicly available without rate limits or registration.

### 2. (Optional) Using Your Own NASA API Key
If you want to use your own personal API key from [api.nasa.gov](https://api.nasa.gov) for legacy fallback:

* **In the Android TV App**:
  * Open AstroFrame on your TV.
  * Select **"NASA API Key"** in the settings column.
  * Enter your custom key and click **Save**.

* **When Building from Source**:
  * Create or open `local.properties` in the project root:
    ```properties
    NASA_API_KEY=your_personal_key_here
    ```
  * Or export it as an environment variable before building:
    ```bash
    export NASA_API_KEY=your_personal_key_here
    ./gradlew assembleRelease
    ```

---

## ⚙️ Configuration & Customization

AstroFrame stores its settings in SharedPreferences:
* **API Key**: Comes pre-configured with your NASA API key.
* **Rotation Intervals**: `15 min`, `30 min`, `1 hour`, `3 hours`, `Daily`.
* **Overlay Positions**:
  * `Top Left` *(Recommended for Projectivy with bottom app rows)*
  * `Top Right`
  * `Bottom Left`
  * `Bottom Right`
  * `Hidden / Off` *(Pure artwork without text overlay)*
* **Source Modes**:
  * **Random Archive**: Pulls random historic photos across the full 1995–present NASA archive (recommended for frequent 30-min cycling).
  * **Today's APOD**: Always displays the official daily image.

---

## 📁 Storage Directory Structure
Wallpapers are generated and stored in the TV's public media folder:
```text
/sdcard/Pictures/AstroFrame/
├── current_wallpaper.jpg     <-- Latest generated wallpaper (overwritten each cycle)
├── apod_2024-03-24.jpg        <-- Rolling history of the last 20 images
├── apod_2023-11-12.jpg
└── ...
```

---

## 📄 License
MIT License. Astronomy photos and explanations courtesy of the [NASA APOD API](https://api.nasa.gov/).

