# MechBoard

> A privacy-focused, open-source mechanical keyboard for Android with customizable colors, realistic switch sounds, custom keypress sound uploads, and personalized themes. Built on HeliBoard.

**MechBoard** brings the tactile aesthetics and acoustic satisfaction of high-end custom mechanical keyboards to Android. Featuring sculpted MT3/SA spherical keycaps, physical switch acoustics, customizable palettes, dynamic RGB underglow, and a dedicated Mech Studio workbench—all while remaining **100% offline** and private.

---

## 🔒 100% Offline & Privacy by Design

- **Zero Network Permissions**: MechBoard does not request or use `android.permission.INTERNET`.
- **Completely Self-Contained**: All switch audio assets, keycap physics rendering, dictionaries, and theme configurations run strictly locally on your device.
- **Zero Telemetry**: No tracking, no analytics, no keystroke logging, and no external server connections whatsoever. Your typing data never leaves your phone.

---

## ✨ Features

### ⌨️ Authentic 3D Mechanical Keycaps (MT3 / SA Profile)
- **Deep Spherical Scooped Dish**: Authentic MT3/Susuwatari dish geometry with concave radial scoops and specular rim lighting.
- **Tapered 3D Bevel Skirts**: 4-sided trapezoidal keycap walls with realistic shadow depth and diagonal corner chamfers.
- **Tactile Homing Bars**: Integrated tactile homing bars on `F` and `J` keys.
- **Physical Key Plunge**: Real-time downward plunge animation upon keypress.

### 🔊 Realistic Switch Acoustics
- **6 Physical Switch Sound Profiles**:
  - **Kailh Box Jade** – Loud, crisp, high-pitch tactile clickbar snap.
  - **Cherry MX Blue** – Classic clicky mechanical switch with sharp acoustic release.
  - **Gateron Brown** – Gentle tactile bump with warm acoustic clack.
  - **Gateron Red** – Smooth linear keypress with deep bottom-out thud.
  - **Gateron Black** – Heavy linear switch with muted acoustic travel.
  - **Gateron Silent** – Dampened silicone stem, whisper-quiet typing.
- **On-Keyboard Quick Volume Control**: Adjust switch click volume directly from the keyboard top bar without opening the app.
- **Silent Mode Audio Override**: Option to keep switch acoustics playing through media channel even when your phone is set to silent or vibrate.

### 🎨 Keycap Customizer & RGB Underglow
- **Part-by-Part Color Customizer**: Easily customize individual keycap components in 1 tap:
  - Switch Plate / Case Chassis
  - Alpha Keycaps (QWERTY letters)
  - Modifier Keycaps (Shift, Space, Symbols)
  - Accent Artisan Keys (Enter, Esc, Arrows)
  - Legend Text & Glyphs
- **RGB Underglow & Rim Lighting**: Dynamic rainbow spectrum glow pulsing beneath the switch plate.
- **Adaptive Light & Dark Mode**:
  - Automatically adapts to system Day/Night mode.
  - Manual override options: *Follow System (Auto)*, *Always Dark*, or *Always Light*.
  - Curated mechanical colorways: **Cyber Amber (MT3)**, **GMK Olivia**, **Carbon Stealth**, **GMK Dolch**, **Matcha Green**, **Cyberpunk Neon**, and **Retro Classic**.

### 📱 Mech Studio Hub
- **Real-Time Live Mobile Preview**: An interactive 4-row Android mobile layout preview that reflects all theme, color, switch sound, height, and RGB adjustments instantly.
- **Interactive Typing Line**: Tap preview keys to test switch acoustics, feel key plunge, and test text input live.
- **Size & Ergonomics**:
  - Fine-grained Keyboard Height slider (70% to 135%) with quick presets (*Compact*, *Normal*, *Tall*).
  - Key Spacing (Gaps) slider for customizable keycap density.
  - Tactile haptic bump vibration matching switch actuation points.

### 🚀 Smart Typing & Keyboard Flow
- **Smart Caps Lock Reset**: When typing in all-caps, sending a chat message (via Enter or in-app send button) automatically resets Caps Lock back to standard sentence capitalization—matching Gboard behavior.
- **Emoji Keyboard with Back Button**: Dedicated top-strip `← / ABC` button for returning to the text keyboard in a single tap.
- **Optimized Emoji Spacing**: Generous key spacing on emoji grids to prevent overlapping icons.
- **Clean Vector UI**: Exclusively utilizes modern Lucide vector icons across the interface.

---

## 🛠️ Building & Installation

### Requirements
- Android SDK 35
- JDK 17 or higher
- Android Studio Ladybug / Gradle 8.11+

### Build APK
```bash
# Clone the repository
git clone https://github.com/Ryderhxrzy/Mechanical-Keyboard-Android.git
cd Mechanical-Keyboard-Android

# Build Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/MechBoard_<version>-debug.apk
```

---

## 📄 License & Credits

- **MechBoard** is free and open-source software licensed under the [GNU General Public License v3.0](LICENSE).
- Based on [HeliBoard](https://github.com/HeliBorg/HeliBoard).
- Icons powered by [Lucide Icons](https://lucide.dev/) (ISC License).
