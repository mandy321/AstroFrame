# 🌌 AstroFrame (Android TV & Google TV)

### The Ultimate Ambient Art & Deep-Space Screensaver for Android TV, Google TV & Fire TV
**Engineered with Apple tvOS Glassmorphism, 10-bit Deep Color, OLED Burn-In Care & Samsung "The Frame" Matting**

<p align="center">
  <a href="https://github.com/mandy321/AstroFrame/releases/latest"><img src="https://img.shields.io/github/v/release/mandy321/AstroFrame?style=for-the-badge&color=7C3AED&label=LATEST%20RELEASE" alt="Latest Release"></a>
  <a href="https://github.com/mandy321/AstroFrame/releases"><img src="https://img.shields.io/github/downloads/mandy321/AstroFrame/total?style=for-the-badge&logo=github&color=2563EB&label=TOTAL%20DOWNLOADS" alt="Total Downloads"></a>
  <a href="https://github.com/mandy321/AstroFrame/releases/latest"><img src="https://img.shields.io/github/downloads/mandy321/AstroFrame/latest/total?style=for-the-badge&color=059669&label=LATEST%20DOWNLOADS" alt="Latest Downloads"></a>
  <a href="https://github.com/mandy321/AstroFrame/stargazers"><img src="https://img.shields.io/github/stars/mandy321/AstroFrame?style=for-the-badge&logo=star&color=F59E0B&label=STARS" alt="Stars"></a>
  <a href="https://github.com/mandy321/AstroFrame"><img src="https://komarev.com/ghpvc/?username=mandy321-astroframe&style=for-the-badge&color=0EA5E9&label=REPO%20VIEWS" alt="Repo Views"></a>
</p>

<p align="center">
  <a href="https://github.com/mandy321/AstroFrame/releases/latest/download/AstroFrame-v1.2.1.apk">
    <img src="https://img.shields.io/badge/📥_DOWNLOAD_LATEST_APK-v1.2.1-success?style=for-the-badge&logo=android&logoColor=white&color=10B981" alt="Download APK" height="40">
  </a>
  <a href="https://github.com/mandy321/AstroFrame/releases/latest">
    <img src="https://img.shields.io/badge/📦_ALL_RELEASES-GITHUB-blue?style=for-the-badge&logo=github&color=2563EB" alt="All Releases" height="40">
  </a>
</p>

---

## ⚡ What Makes AstroFrame the GOAT?

While conventional video screensavers stream gigabytes of repetitive video, **AstroFrame** is purpose-built to transform modern living room displays into authentic living art pieces. 

| Feature | AstroFrame | Conventional Screensavers (e.g. Aerial Views) |
| :--- | :---: | :---: |
| **Visual Library** | **Infinite Living Collections** (NASA APOD, JWST, Hubble, The Met Museum) | Static catalog of ~200 aerial clips |
| **Bandwidth Consumption** | **Ultra-Low (<100MB/day)** with local 4K cache | Heavy (15–25 GB/day streaming 4K video) |
| **Hardware Color Depth** | **10-bit Wide Color Gamut (`RGBA_F16`)** — Zero sky banding | 8-bit standard |
| **Display Protection** | **Active OLED Care Engine** (Imperceptible micro-drift shifts) | Basic static overlay repositioning |
| **Samsung "The Frame" Mode** | **Architectural Beveled Matting** with 45° inner shadowbox | Border-to-border only (looks like a TV, not a canvas) |
| **Motion Engine** | **60 FPS Hardware "Ken Burns" Parallax** | Fixed video playback |
| **Music Ecosystem** | **AuraMusic "Now Playing" Island** synergy | Basic media session hook |
| **TV Remote Control** | **Full D-Pad Interactivity** (Skip, Pause, Metadata Inspector) | Variable support |

---

## 📸 Screenshots & Visual Preview

<div align="center">
  <table style="border: none; width: 100%;">
    <tr>
      <td width="50%" align="center">
        <b>🎨 The Met Museum (Fine Art with Museum Gold Badges)</b><br>
        <a href="screenshots/preview_the_met.jpg"><img src="screenshots/preview_the_met.jpg" alt="The Met Museum Preview" width="100%"></a>
      </td>
      <td width="50%" align="center">
        <b>🚀 NASA Cosmos (4K APOD with NASA Cyan Badges)</b><br>
        <a href="screenshots/preview_nasa_apod.jpg"><img src="screenshots/preview_nasa_apod.jpg" alt="NASA Cosmos Preview" width="100%"></a>
      </td>
    </tr>
    <tr>
      <td colspan="2" align="center">
        <br><b>📺 Android TV Remote-Friendly D-Pad Interface</b><br>
        <a href="screenshots/preview_tv_app.png"><img src="screenshots/preview_tv_app.png" alt="AstroFrame TV Interface" width="85%"></a>
      </td>
    </tr>
  </table>
</div>

---

## ✨ Flagship Capabilities

### 🎮 1. Interactive TV Remote D-Pad Navigation
Never feel locked into a screensaver. Take complete control with your television remote:
* **`D-Pad Right / Media Next`**: Instantly crossfade to the next artwork.
* **`D-Pad Left / Media Previous`**: Return to the previous artwork.
* **`Center OK / Info Button`**: Toggle the Apple tvOS-style translucent metadata card (artwork title, telescope details, and dates).
* **`Play/Pause / D-Pad Down`**: Freeze rotation on any artwork to admire it indefinitely.
* **`Back / Escape`**: Instantly wake the television.

### 🛡️ 2. Active OLED Care & Anti-Burn-In Drift
Designed specifically for high-end **LG OLED**, **Sony Bravia QD-OLED**, and **Samsung OLED** displays:
* An imperceptible background shifter gently translates the artwork canvas and floating HUD by `[-3..+3]px` every 90 seconds.
* Subpixels never experience static retention, ensuring 100% burn-in safety during all-day ambient use.

### 🌈 3. 10-Bit Wide Color Gamut Deep Color Pipeline
* Eliminates color stepping, posterization, and gradient banding across deep nebulae and solar coronas.
* Harnesses `ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT` and renders in **`Bitmap.Config.RGBA_F16`** (16-bit half-float depth) on supported hardware like the **NVIDIA Shield TV Pro**, **Chromecast with Google TV**, and **Sony Bravia 4K**.

### 🖼️ 4. Architectural Museum Matting (Samsung "The Frame" Rival)
Select from architectural mat styles to turn your flat screen into a genuine framed canvas:
* **Bezel-to-Bezel**: Edge-to-edge full screen photography.
* **Modern Slim Mat (24dp)**: Clean modern border.
* **Museum Classic Bevel (48dp)**: 45-degree architectural inner bevel shadow with archival warm gallery cream (`#F4EFE6`).
* **Gallery Deep Shadowbox (72dp)**: Premium framed museum exhibition look.

### 🎥 5. Cinematic 60 FPS "Ken Burns" Parallax
* Smooth, meditative slow pan and scale (`1.065x`) rendered at 60 frames per second.
* Deep space images feel dynamic and breathing without stuttering or video compression artifacts.

### 🎶 6. AuraMusic "Now Playing" Island Synergy
* If you run [AuraMusic](https://github.com/mandy321/Auramusic-private), AstroFrame automatically displays an ambient frosted glass island in the top-left corner with track titles, artists, and bit-perfect audio badges.

---

## 📥 Direct APK Download

| Release | Architecture | Direct Download Link | Release Page |
| :--- | :--- | :--- | :--- |
| **v1.2.1 (Latest)** | Android TV / Fire TV (Universal) | [**⬇️ Download AstroFrame-v1.2.1.apk**](https://github.com/mandy321/AstroFrame/releases/latest/download/AstroFrame-v1.2.1.apk) | [Release Notes](https://github.com/mandy321/AstroFrame/releases/latest) |
| **All Releases** | Archives & History | [Browse All Releases](https://github.com/mandy321/AstroFrame/releases) | — |

#### Quick Install to TV via ADB:
```bash
adb install -r AstroFrame-v1.2.1.apk
```

---

## 📺 How to Set as Default TV Screensaver

Since Android TV 12 and Google TV often hide third-party screensaver menus, AstroFrame includes an automated **Daydream Assistant**:

1. Open **AstroFrame** on your television.
2. Click **"Set as TV Screensaver 📺"**.
3. AstroFrame immediately launches the hidden Google TV Daydream chooser.
4. Select **"AstroFrame Screensaver"** and choose your preferred timeout (e.g. *5 minutes*).

*(For Amazon Fire TV Stick, set AstroFrame via Projectivy Launcher's Idle Mode or use the standard 1-line ADB daydream command).*

---

## 🛠️ How to Connect with Projectivy Launcher

AstroFrame automatically generates and updates wallpapers in your TV's public media folder:
```text
/sdcard/Pictures/AstroFrame/
```
1. In **Projectivy Launcher**, navigate to **Settings $\rightarrow$ Appearance $\rightarrow$ Background $\rightarrow$ Source**.
2. Select **Folder** and choose `/sdcard/Pictures/AstroFrame/`.
3. Set transition to **Smooth Crossfade**.

---

## ⚙️ Building from Source

```bash
# Clone repository
git clone https://github.com/mandy321/AstroFrame.git
cd AstroFrame

# Build debug APK
./gradlew assembleDebug
```
Output binary will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

---

## ❓ Frequently Asked Questions (FAQ)

<details>
<summary><b>1. Does AstroFrame require the paid (Premium) version of Projectivy Launcher?</b></summary>
<br>

**No, AstroFrame is 100% free and does NOT require Projectivy Premium.**
* **As a Native TV Screensaver (`DreamService`)**: Works completely standalone and free on all Android TV, Google TV, and Fire TV devices. You don't even need Projectivy installed. Simply click *"Set as TV Screensaver 📺"* in the app.
* **System Wallpaper**: AstroFrame also automatically updates the native Android TV system wallpaper via `WallpaperManager` for launchers that read the system background.
* **About Projectivy**: If you specifically want to set Projectivy's personal home screen background to a custom *User Folder* (`/sdcard/Pictures/AstroFrame/`), that specific setting in Projectivy is a Projectivy Premium feature. All screensaver modes, 4K artwork rotations, 10-bit color, and OLED Care features in AstroFrame are completely free.
</details>

<details>
<summary><b>2. Why was my screensaver showing a black screen when I picked "The Met Museum"?</b></summary>
<br>

When switching to **The Met Museum**, high-resolution 4K/8K uncompressed scans must first be downloaded to the TV storage. 
* In **v1.2.1**, if the screensaver started before that initial download finished, it would time out.
* **Solution**: Simply open the app and tap **"Update Wallpaper Now"** once while on Museum mode. 
* *Note: The latest code now automatically triggers an immediate background download upon selecting the collection and includes an active polling retry loop so the screen never stays black.*
</details>

<details>
<summary><b>3. Is my TV at risk of OLED burn-in if I leave it on all day?</b></summary>
<br>

**No.** AstroFrame is engineered specifically with an **Active OLED Care Engine**. Every 90 seconds, it applies an imperceptible micro-drift shift (`[-3..+3]px`) to the artwork canvas and floating HUD. Static subpixel retention is eliminated, making it 100% safe for LG OLED, Sony Bravia QD-OLED, and Samsung OLED screens.
</details>

<details>
<summary><b>4. How do I control the screensaver with my TV remote?</b></summary>
<br>

AstroFrame features full Leanback D-pad remote interactivity:
* **Right Arrow / Next**: Jump to next artwork.
* **Left Arrow / Previous**: Go back to previous artwork.
* **OK / Center Button**: Toggle the Apple tvOS-style translucent metadata card (title, credit, telescope details).
* **Play/Pause / Down Arrow**: Freeze rotation on any artwork you want to keep on screen.
* **Back / Home**: Wake the TV and exit.
</details>

<details>
<summary><b>5. Does AstroFrame consume a lot of internet bandwidth like video screensavers?</b></summary>
<br>

**No.** Conventional video screensavers (like Apple TV aerial loops) stream 15–25 GB of 4K video per day. AstroFrame uses cached uncompressed 4K stills paired with a **60 FPS hardware Ken Burns motion engine**, consuming **less than 100 MB per day** while providing crystal-clear 10-bit color depth.
</details>

## 📄 License
MIT License. Astronomy photos and explanations courtesy of the [NASA APOD API](https://api.nasa.gov/) and [The Metropolitan Museum of Art Open Access](https://www.metmuseum.org/).
