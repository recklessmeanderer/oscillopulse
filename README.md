# OscilloPulse

A precision carrier & brainwave entrainment synthesizer for Android, featuring a real-time phosphor oscilloscope, optical image sonification, harmonic chord layering, binaural beats, and studio-grade 32-bit floating point WAV export.

---

## 📱 Getting the APK

There are two primary ways to get the installable APK directly from GitHub:

### 1. GitHub Releases (Recommended)
Whenever a release or tag is published (e.g. `v1.0.0`), the automated GitHub Actions workflow compiles the app and attaches the installable APK directly to the release:
1. Navigate to the **Releases** tab on the right side of this repository (or go to `https://github.com/<your-username>/<repo>/releases`).
2. Download `OscilloPulse.apk` (or `OscilloPulse-vX.X-debug.apk`).
3. Open and install it on your Android device (ensure "Install unknown apps" permission is allowed for your browser or file manager).

### 2. GitHub Actions Artifacts (Any Commit)
You can also download a fresh APK from any CI run or manual build:
1. Go to the **Actions** tab on your GitHub repository.
2. Select **"Build & Release APK"** from the left sidebar.
3. Click on the latest workflow run.
4. Scroll down to the **Artifacts** section and download **OscilloPulse-APKs**.

---

## 🚀 How to Trigger an APK Release Build

The repository includes a ready-to-run GitHub Actions workflow (`.github/workflows/release.yml`). You can trigger an APK build using any of these methods:

### Option A: Create a Release in GitHub (Web UI)
1. On your repository page, click **"Releases"** -> **"Draft a new release"**.
2. Click **"Choose a tag"**, type a version like `v1.0.0`, and click **"Create new tag"**.
3. Add a title and description, then click **"Publish release"**.
4. GitHub Actions will start automatically, build the APK, and attach `OscilloPulse.apk` to that release.

### Option B: Push a Git Tag from Terminal
```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0
```
This triggers the workflow and automatically creates/attaches the APK to the tag release.

### Option C: Manual Trigger via GitHub Actions Tab
1. Go to the **Actions** tab in GitHub.
2. Click **"Build & Release APK"** in the left menu.
3. Click the **"Run workflow"** dropdown button on the right and click **"Run workflow"**.
4. The workflow will build the APK and make it available as a downloadable artifact.

---

## 🛠 Local Build Instructions

To build the APK locally on your computer:

```bash
# Clone the repository
git clone https://github.com/<your-username>/<repo>.git
cd <repo>

# Decode the debug keystore (if not already decoded)
base64 -d debug.keystore.base64 > debug.keystore

# Build debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔑 Optional: Production Release Signing

By default, the workflow builds a signed debug APK that installs immediately on any Android device without certificate registration.

If you wish to build Play Store-signed release APKs with your own custom keystore, add these secrets under **Settings > Secrets and variables > Actions**:

| Secret Name | Description |
| :--- | :--- |
| `KEYSTORE_BASE64` | Base64-encoded string of your `.jks` or `.keystore` file (`base64 -w 0 upload-key.jks`) |
| `STORE_PASSWORD` | Password for the keystore |
| `KEY_PASSWORD` | Password for the signing key |

When these secrets are set, the workflow will automatically compile and attach a signed `OscilloPulse-release.apk` alongside the debug build.

---

## ✨ Features

- **Phosphor CRT Oscilloscope:** Real-time 60fps waveform trace with auto-trigger zero-crossing, timebase zoom (1x–8x), vertical gain, hold/freeze, Vpp, and RMS metering.
- **Optical Image Sonification:** Load any photo via zero-permission Photo Picker or 5 procedural presets (Cosmic Nebula, Ocean Surf, Forest Canopy, Golden Sunset, Aurora Borealis) to modulate noise floor luminance and display custom wallpaper behind the CRT grid.
- **Precision Carrier Wave Generator:** Sine, Square, Sawtooth, and Triangle waveforms with 0.1 Hz step fine-tuning (1 Hz to 1000 Hz).
- **Binaural Beat Engine:** Independent stereo left/right carrier synthesis with live beat frequency readout.
- **Harmonic Chord Layering:** Stack 3 carrier layers simultaneously (Power 5th, Octave, Major/Minor triads, Suspended chords, or Custom ratios).
- **Multi-Color Noise Floors:** White, Pink (Paul Kellet filter), and Brown (leaky integration) noise textures.
- **Audio Ducking:** Intelligent volume attenuation during alerts and navigation prompts.
- **32-bit Floating Point WAV Exporter:** Studio master export with customizable duration, sample rates (44.1 kHz, 48 kHz, 96 kHz), and bit depths (16-bit, 24-bit, 32-bit float).
- **Media Controls & Sticky Notification:** Bluetooth media buttons (play/pause/stop) and sticky lockscreen transport notification.
