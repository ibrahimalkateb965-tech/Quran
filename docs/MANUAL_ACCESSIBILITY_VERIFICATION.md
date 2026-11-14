# Blind Quran App iOS: Manual Accessibility & VoiceOver Verification Guide

**Application Target:** `com.aistudio.quranblind.a11y`  
**Deployment Target:** iOS 15.0+ (Tested on iOS 27.2 / iPhone 18 Pro Simulator)  
**Authoritative Standards:** `ACCESSIBILITY_GUIDELINES_IOS.md`, Invariants `I1`–`I6`, Requirements `R1`–`R8`

---

## 1. Overview & Objectives

This manual verification guide provides accessibility engineers, QA auditors, and developers with rigorous, step-by-step procedures to inspect, test, and validate the accessibility architecture of the Blind Quran App iOS client on the iOS Simulator and physical hardware.

The verification covers two complementary methodologies:
1. **Xcode Accessibility Inspector**: Static tree hierarchy, single-node cardinality inspection, accessibility traits, custom rotor actions, and automated accessibility auditing.
2. **VoiceOver Screen Reader on iOS Simulator**: Real-time auditory verification, gesture emulation, system gestures (Magic Tap, 2-finger scrub Escape), custom rotor workflows, 3-finger horizontal paging in Right-to-Left (RTL), and the Screen-Off Battery Saver touch barrier.

---

## 2. Prerequisites & Simulator Environment Setup

### 2.1 Booting the Target Simulator
Ensure the target iOS Simulator is booted and active:
```bash
# List available simulator devices
xcrun simctl list devices available | grep iPhone

# Boot iPhone 18 Pro simulator (or your chosen device UUID)
xcrun simctl boot 72019BA2-8C01-4A19-AA03-7B95B07A7FC1 || true

# Open the Simulator application
open -a Simulator
```

### 2.2 Building and Installing the Application
Compile and install the latest build onto the simulator:
```bash
# Build the application target using xcodebuild
xcodebuild -project iosApp/QuranBlind.xcodeproj \
    -scheme QuranBlind \
    -sdk iphonesimulator \
    -destination "platform=iOS Simulator,id=72019BA2-8C01-4A19-AA03-7B95B07A7FC1" \
    build

# Install the compiled .app bundle into the simulator
xcrun simctl install 72019BA2-8C01-4A19-AA03-7B95B07A7FC1 \
    ~/Library/Developer/Xcode/DerivedData/QuranBlind-*/Build/Products/Debug-iphonesimulator/QuranBlind.app

# Launch the app with RTL layout and Arabic locale
xcrun simctl launch 72019BA2-8C01-4A19-AA03-7B95B07A7FC1 \
    com.aistudio.quranblind.a11y \
    -AppleLanguages "(ar)" \
    -AppleLocale "ar_SA"
```

---

## 3. Part I: Xcode Accessibility Inspector Verification

Xcode Accessibility Inspector allows inspecting accessibility trees without needing auditory VoiceOver speech.

### 3.1 Launching and Attaching the Inspector
1. Launch Xcode Accessibility Inspector:
   ```bash
   open -a "Accessibility Inspector"
   ```
2. In the target device selector dropdown located in the upper-left corner of the Inspector window, choose the active simulator (e.g., `iPhone 18 Pro`).
3. Ensure the Blind Quran App is running in the foreground on the simulator.

---

### 3.2 Test Case AI-1: Stealth Box Surface & Single-Node Cardinality (Invariant I2)

**Objective**: Verify that the central recitation card is strictly collapsed into $\le 1$ accessibility node and prevents navigation focus from trapping inside individual Arabic words, Ayah number badges, or equalizer bars.

**Procedure**:
1. Click the **Inspection Crosshair** button in the Inspector toolbar (or press `Cmd + F7`).
2. Hover over the recitation area (the Ayah card containing the Arabic Uthmanic text).
3. Click to select the element.

**Verification Checklist**:
- [ ] **Bounding Frame**: Exactly one single green focus rectangle encloses the entire central recitation canvas (`AyahCardView`).
- [ ] **No Sub-Element Trapping**: Hovering or clicking on individual Arabic words, the Ayah number pill, or the 5 animated equalizer bars does NOT create separate focus nodes.
- [ ] **Hierarchy Panel**: In the element hierarchy navigation tree, verify the node is a leaf element with `children: .ignore`.
- [ ] **Properties Panel**:
  - **Label**: Matches `"الآية \(ayah.numberInSurah) من سورة \(surah.nameArabic): \(ayah.textArabic)"`  
    *Example:* `"الآية 1 من سورة الفاتحة: بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"`
  - **Value**: Matches playback state:
    - `"متوقف"` when recitation is paused or idle.
    - `"قيد التلاوة"` when recitation is active.
  - **Hint**: Reads `"انقر نقراً مزدوجاً للتشغيل أو الإيقاف، أو استخدم إجراءات الروتور"`.
  - **Traits**: Contains `Button` or `Updates Frequently`.

---

### 3.3 Test Case AI-2: Custom Rotor Actions & Universal Activation

**Objective**: Verify that VoiceOver custom rotor actions (`إعادة التلاوة`, `الآية التالية`, `الآية السابقة`) are exposed on the Stealth Box surface.

**Procedure**:
1. With the Stealth Box selected in the Inspector, inspect the **Actions** section in the Properties panel.
2. Click the **Perform Action** button for each action.

**Verification Checklist**:
- [ ] **Activate (`default`)**: Toggles recitation between playing and paused.
- [ ] **Custom Action: `"إعادة التلاوة"`**:
  - Resets recitation of the active Ayah back to `00:00`.
  - Triggers a medium haptic pulse.
- [ ] **Custom Action: `"الآية التالية"`**:
  - Advances audio playback to the next Ayah.
  - Updates the label and Arabic text immediately.
- [ ] **Custom Action: `"الآية السابقة"`**:
  - Rewinds audio playback to the previous Ayah (or resets to beginning if elapsed time > 3s).

---

### 3.4 Test Case AI-3: Automated Accessibility Audit

**Objective**: Execute Apple's automated accessibility audit against all primary screens and sheets to confirm zero violations.

**Procedure**:
1. Click the **Audit** icon (stethoscope icon or `Cmd + 6`) in the Accessibility Inspector toolbar.
2. Ensure the main player screen (`QuranPlayerScreen`) is displayed on the simulator.
3. Click **Run Audit**.
4. Open the Surah Index Sheet (`SurahIndexSheet`), and click **Run Audit**.
5. Open the Reciter Selector Sheet (`ReciterSelectorSheet`), and click **Run Audit**.

**Verification Checklist**:
- [ ] **Contrast**: Zero low-contrast warnings against both light and dark OLED themes.
- [ ] **Hit Targets**: All interactive elements (header buttons $\ge 68\text{pt}$, action buttons $\ge 52\text{pt}$, sheet list items $\ge 64\text{pt}$) meet or exceed minimum touch target dimensions.
- [ ] **Dynamic Type**: Text elements scale cleanly with system font sizes without truncation or clipping.
- [ ] **Missing Labels**: Zero unlabeled image or icon warnings.

---

## 4. Part II: VoiceOver Screen Reader Simulator Verification

VoiceOver provides auditory feedback and system gesture handling.

### 4.1 Enabling VoiceOver on iOS Simulator
Choose one of the following methods:
- **Method A (Accessibility Shortcut)**: In the Simulator, press `Cmd + Option + F5`, or go to **Settings → Accessibility → Accessibility Shortcut** and check **VoiceOver**. Press the Side button triple-click shortcut.
- **Method B (Settings App)**: Open **Settings → Accessibility → VoiceOver → Toggle ON**.
- **Method C (Inspector Quick Toggle)**: In Accessibility Inspector, click **Speech** / VoiceOver toggle or press `Control + Option + Z`.

### 4.2 VoiceOver Simulator Keyboard Navigation Primer
When testing on macOS Simulator, use the **VO** modifier key (`Control + Option` or `Caps Lock`):

| Action | Simulator Shortcut | Screen Gesture Equivalent |
|---|---|---|
| **Move to Next Element** | `VO + Right Arrow` | 1-finger swipe right |
| **Move to Previous Element** | `VO + Left Arrow` | 1-finger swipe left |
| **Activate Element** | `VO + Space` | 1-finger double-tap |
| **Magic Tap (Play/Pause)** | `VO + Space` (on Root) / 2-finger double click | 2-finger double-tap |
| **Escape / Dismiss Modal** | `Escape` key | 2-finger scrub ("Z" gesture) |
| **Rotor Next / Previous Item** | `VO + Up / Down Arrow` | 1-finger swipe up / down |
| **Switch Rotor Category** | `VO + Command + Left / Right` | 2-finger rotation |
| **3-Finger Scroll Right (RTL Next)** | `Page Down` or `VO + Shift + Right` | 3-finger swipe right |
| **3-Finger Scroll Left (RTL Prev)** | `Page Up` or `VO + Shift + Left` | 3-finger swipe left |

---

### 4.3 Test Scenario 1: Cold Launch & Recitation Announcement (R2 / I2)

1. Launch the app with VoiceOver active.
2. Observe initial focus placement.
3. **Expected Speech Output**:
   > *"القرآن. الآية 1 من سورة الفاتحة: بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ. متوقف. انقر نقراً مزدوجاً للتشغيل أو الإيقاف، أو استخدم إجراءات الروتور."*
4. Verify that focus is placed on the entire recitation canvas without requiring manual navigation into child views.

---

### 4.4 Test Scenario 2: Magic Tap System Gesture (R3)

1. With the app in the foreground, perform a **Magic Tap** (`Control + Option + Space` or two-finger double-tap anywhere on screen).
2. **Expected Behavior**:
   - Audio begins reciting immediately.
   - VoiceOver value announces or reflects: *"قيد التلاوة"*.
   - A subsequent Magic Tap pauses recitation immediately.
   - VoiceOver value announces or reflects: *"متوقف"*.
   - Recitation toggle works globally across the screen without having to navigate to a dedicated play button.

---

### 4.5 Test Scenario 3: Custom Rotor Navigation & Replay (R2)

1. Navigate focus to the Stealth Box recitation surface.
2. Rotate the Rotor (`VO + Command + Left/Right`) until **"إجراءات" (Actions)** is selected.
3. Swipe down (`VO + Down Arrow`):
   - Rotor announces: *"إعادة التلاوة"*
   - Double-tap (`VO + Space`): Current Ayah resets to `00:00` with a tactile medium haptic pulse.
4. Swipe down again:
   - Rotor announces: *"الآية التالية"*
   - Double-tap: Audio advances to Ayah 2 (*"الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"*).
5. Swipe down again:
   - Rotor announces: *"الآية السابقة"*
   - Double-tap: Audio rewinds back to Ayah 1 (*"بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"*).

---

### 4.6 Test Scenario 4: Three-Finger Horizontal Accessibility Scroll in RTL (R2)

1. With focus on the Stealth Box recitation surface:
2. Perform a **3-finger horizontal swipe right** (leading edge in RTL):
   - VoiceOver navigates to the **Next Ayah** (Ayah 2).
   - Audio streams Ayah 2 and updates the screen title.
3. Perform a **3-finger horizontal swipe left** (trailing edge in RTL):
   - VoiceOver navigates back to the **Previous Ayah** (Ayah 1).
   - Audio streams Ayah 1.

---

### 4.7 Test Scenario 5: Bottom Sheet Presentation & Two-Finger Scrub Escape (R3)

1. Navigate focus to the header button **"اختيار السورة"** (`SurahIndexButton`).
2. Double-tap to activate:
   - `SurahIndexSheet` opens from the bottom.
   - VoiceOver announces sheet presentation and active Surah (*"مفتوحة الآن"*).
3. Perform a **Two-Finger Scrub (Escape)** (press `Escape` key or draw a "Z" on screen):
   - The Surah sheet dismisses smoothly.
   - Focus returns gracefully to the main player screen.
4. Repeat with the **"اختيار القارئ"** (`ReciterSelectorSheet`) button to confirm uniform Escape dismissal across all bottom sheets.

---

### 4.8 Test Scenario 6: OLED Screen-Off Battery Saver Touch Barrier (R4)

1. Activate the Screen-Off Battery Saver:
   - Double-tap the **"وضع إيقاف الشاشة"** header button, or activate it via the Rotor.
2. **Visual Verification**:
   - The entire screen turns pure black (`#000000`).
   - Visual OLED energy savings are active.
3. **Accessibility Barrier Verification**:
   - VoiceOver announces:
     > *"شاشة توفير الطاقة نشطة. انقر مرتين للاستيقاظ."*
   - Attempt to swipe left or right: Focus remains locked on the overlay; no underlying buttons (Play, Surah, Theme) can receive touch or screen reader focus due to `.isModal` trait isolation.
   - Audio recitation continues uninterrupted in the background.
4. **Wake-up Verification**:
   - Perform a double-tap (`VO + Space` or double-tap screen) or a two-finger scrub (`Escape`).
   - A double-tap haptic pulse triggers.
   - VoiceOver announces: *"تم إلغاء وضع إيقاف الشاشة"*.
   - The overlay dismisses and reveals the visual player without interrupting recitation.

---

### 4.9 Test Scenario 7: Audio Interruption Safety & Ghost Resume Guard (Invariant I4 / R6)

This scenario verifies that audio recitation immediately pauses upon receiving system audio interruptions (telephony calls, alarms, competing media, or route changes) and strictly maintains the paused state without auto-resuming unbidden (Invariant `I4`).

#### Method A: iOS Simulator Verification (Competing Audio & Route Change)
Because `xcrun simctl` does not emulate a cellular baseband, authentic simulator interruption testing is performed via competing audio sessions or audio route changes:

1. **Option 1: Competing Audio via MobileSafari**:
   - Start recitation of any Surah in `QuranBlind`.
   - In terminal, command the simulator to play competing media in Safari:
     ```bash
     xcrun simctl openurl 72019BA2-8C01-4A19-AA03-7B95B07A7FC1 "https://www.w3schools.com/html/horse.mp3"
     ```
   - When Safari begins audio playback, `AVAudioSession` preempts `QuranBlind` (`AVAudioSessionInterruptionType.began`).
   - **Verification**: Recitation pauses immediately.
   - Switch back to `QuranBlind` and close or pause Safari (`AVAudioSessionInterruptionType.ended`).
   - **Verification**: Recitation **REMAINS STRICTLY PAUSED** (`isPlaying == false`). It must **NEVER** auto-resume.

2. **Option 2: Audio Route Change ("Becoming Noisy" / Headphone Disconnection)**:
   - While recitation is playing through headphones or a Mac audio device, disconnect or change the audio output device in macOS Settings / Control Center.
   - `AVAudioSession.routeChangeNotification` triggers with reason `.oldDeviceUnavailable`.
   - **Verification**: Recitation pauses immediately to prevent sound leakage in public environments.

#### Method B: Physical Hardware Device Verification (Cellular GSM & FaceTime Calls)
On a physical iPhone, telephony interruption testing is performed directly:
1. Start recitation of any Surah with VoiceOver active.
2. From a secondary phone, place an incoming Cellular (GSM) or FaceTime Audio call to the test device.
3. When the incoming call banner appears, verify recitation pauses immediately (`.began`).
4. Decline or terminate the call (`.ended`).
5. **Verification**:
   - Recitation must **REMAIN PAUSED** (`isPlaying == false`).
   - Recitation must **NEVER** auto-resume unprompted, even if iOS transmits `shouldResume = 1` in `AVAudioSessionInterruptionOptionKey`.
   - Verify the **2000ms Ghost Resume Guard**: Any accidental touch within `2000ms` of call termination is safely ignored.
   - After `2000ms`, an explicit user Magic Tap (two-finger double tap) cleanly resumes recitation.

---

## 5. Verification Matrix Summary

| Test ID | Requirement | Subsystem | Inspection Method | Expected Outcome |
|---|---|---|---|---|
| **V-01** | R1 / I3 | Dynamic VoiceOver Detection | Settings toggle mid-session | Tree updates hints and layout reactively without app restart |
| **V-02** | R2 / I2 | Single Node Stealth Box | Accessibility Inspector Crosshair | Exactly 1 focus node enclosing recitation surface; word tokens hidden |
| **V-03** | R2 | Label String Parity | Inspector Properties / VO Speech | `"الآية \(ayah) من سورة \(surah): \(text)"` |
| **V-04** | R2 | Value State Parity | Inspector Properties / VO Speech | `"قيد التلاوة"` (playing) / `"متوقف"` (paused) |
| **V-05** | R2 | Custom Rotor Replay | VoiceOver Rotor Actions | `"إعادة التلاوة"` resets Ayah to 00:00 with medium haptic pulse |
| **V-06** | R2 | Custom Rotor Paging | VoiceOver Rotor Actions | `"الآية التالية"` and `"الآية السابقة"` navigate between Ayahs |
| **V-07** | R2 | RTL 3-Finger Scroll | 3-finger horizontal gestures | Leading (right) -> Next Ayah; Trailing (left) -> Prev Ayah |
| **V-08** | R3 | Universal Magic Tap | 2-finger double tap (`VO + Space`) | Toggles play/pause globally across application |
| **V-09** | R3 | Two-Finger Scrub Escape | 2-finger "Z" gesture (`Escape`) | Dismisses modal sheets and overlays gracefully |
| **V-10** | R4 | OLED Pure Black | Color Inspector | Exact `#000000` (R:0, G:0, B:0, A:1.0) |
| **V-11** | R4 | Modal Isolation Barrier | VoiceOver Swiping | `.isModal` trait locks focus to overlay; background untouchable |
| **V-12** | R4 | Saver Wake Gestures | Double-tap / Escape | Double-tap haptic confirmation and wake announcement |
| **V-13** | R5 / I5 | Serialized Queue | Fast burst events | Announcements queued FIFO; zero truncated utterances |
| **V-14** | R6 / I4 | Interruption Safety | Simulator Safari preemption or route change, or physical incoming GSM call | Audio remains paused; suppresses ghost auto-resume; 2000ms suppression guard active |
