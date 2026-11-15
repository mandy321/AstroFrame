# 🌌 AstroFrame (Android TV)

Automated **NASA Astronomy Picture of the Day (APOD)** 4K wallpaper updater with embedded educational context for **Android TV** and **Projectivy Launcher**.

---

## ✨ Features

* **Automatic 30-Minute Rotation**: Uses Android's robust `WorkManager` background engine to fetch random historic discoveries from NASA's 25+ year archive or today's photo.
* **Readable On-Screen Context**: Generates a sleek, cinematic overlay card directly onto the 1080p/4K canvas with:
  * Official discovery date & NASA APOD badge
  * Artwork/Photo title in bold high-contrast TV typography
  * 2-sentence bite-sized explanation
  * Astrophotographer / Telescope credit (NASA, ESA, James Webb, Hubble, etc.)
* **Projectivy Launcher Native Integration**: Seamlessly saves wallpapers to `/sdcard/Pictures/AstroFrame/`, which Projectivy can cycle as a local slideshow or single dynamic wallpaper.
* **Zero TV UI Lag**: All image processing and text rendering is baked into the image on a background thread. No floating windows or battery drain.
* **10-Foot Remote UI**: Full Android TV interface with D-pad navigation to trigger instant updates, adjust rotation intervals, and reposition the info card.
* **Survives Reboots**: Re-registers automatically on TV boot via `BOOT_COMPLETED`.

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

