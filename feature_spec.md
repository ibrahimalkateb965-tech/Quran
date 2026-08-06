# Feature Specification: Audio & Navigation Enhancements (Blind App)

## 1. Bug Fix: Surah Index Navigation
- **Current Behavior:** Selecting a Surah from the index (e.g., Surah 37) incorrectly jumps to Ayah 37.
- **Expected Behavior:** Selecting any Surah from the index must always reset the position and start at **Ayah 1** of that newly selected Surah.

## 2. UI/UX: Remove Bottom Playback Controls
- **Change:** Completely remove the bottom playback control panel/buttons (Play, Pause, Next, Previous).
- **Reason:** Playback will now rely entirely on the double-tap gesture on the Ayah card, optimizing the UI for blind users.

## 3. Feature: "Continuous Listening" (الاستماع المتواصل) Logic Update
- **UI Change:** Merge the "Continuous Listening" and "Repeat" buttons into a single button located at the **top of the screen**.
- **New Playback Logic:** When this button is activated, the player should play a range of 10 Ayahs: starting from 10 Ayahs before the current one, up to the current Ayah (e.g., if standing on Ayah 35, it plays Ayah 25 through 35).
- **Constraint:** This feature should be disabled or hidden if the current Ayah number is less than 10 (it requires at least 10 Ayahs to function as intended).

## 4. Feature: Stop Auto-Advance on Surah End
- **Change:** When the audio finishes playing the last Ayah of a Surah, the player must STOP. It should **not** automatically transition to the next Surah.

## 5. UX: App Startup Behavior
- **Change:** When the application is launched, it should immediately open and display the "Surah Index" (قائمة السور) by default, instead of requiring the user to open it manually.

## 6. Data Update: Reciters List
- **Change:** Remove the reciters "Mishary Alafasy" (الشيخ مشاري العفاسي) and "Maher Al-Muaiqly" (الشيخ ماهر المعيقلي) from the available reciters data source.

---
**Instructions for Kimi (The Architect):**
Please analyze these 6 requirements against the current codebase (e.g., `QuranViewModel`, `QuranAudioService`, `QuranPlayerScreen`, `SurahIndexSheet`, UI components, etc.). Output a precise, step-by-step technical implementation plan. Do not write the full code; just provide the exact files to modify and the architectural logic changes required in concise bullet points.
