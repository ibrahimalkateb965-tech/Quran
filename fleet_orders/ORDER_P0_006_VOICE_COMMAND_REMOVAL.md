# ORDER-P0-006 — Remove the Voice-Command Feature (Android)

| Field | Value |
|---|---|
| Order ID | ORDER-P0-006 (rev. A) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (or whoever has a working shell first) |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | ISSUED — blocked on B-13 (see §5) |
| Reason | **Client cancelled the voice-command feature**, 2026-09-11 |
| Cancels | ORDER-P0-003 (its whole payload was porting this code) |
| Scope | Android app only. No iOS voice code is to be written, now or later. |

---

## 0. ⚠️ THE ONE THING THAT WILL GO WRONG

**`SpeechManager.kt` is NOT a voice-command component. Do not delete it.**

The name says "Speech", it lives beside `VoiceCommandManager.kt`, and a fast pass over this
folder will take it out. That breaks the app. Read what it actually does:

- it watches `AccessibilityManager` and publishes `isTalkBackEnabledFlow`
- when TalkBack turns on it **stops** the app's own TTS, so the screen reader is the only voice
- `MainActivity` feeds that flow into `LocalTalkBackEnabled` for the entire Compose tree

It is input-free. It has nothing to do with the microphone. It is the mechanism by which this
app "relies on the phone's built-in screen reader" — the exact product principle being kept.

Same for `AccessibilityAnnouncements.kt`, `TalkBackCompositionLocal.kt`,
`HapticFeedbackManager.kt`. All stay.

---

## 1. VERIFIED SCOPE — read from disk 2026-09-11

### DELETE — 4 files
```
app/src/main/java/com/example/accessibility/VoiceCommandManager.kt     9,909 B  (SpeechRecognizer)
app/src/main/java/com/example/accessibility/VoiceCommandParser.kt      9,512 B
app/src/main/java/com/example/ui/components/VoiceCommandGuideSheet.kt  5,923 B
app/src/test/java/com/example/accessibility/VoiceCommandParserTest.kt  6,418 B
```

### EDIT — verified, exact

**`app/src/main/AndroidManifest.xml`** — remove both:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<queries>
    <intent>
        <action android:name="android.speech.RecognitionService" />
    </intent>
</queries>
```
Dropping `RECORD_AUDIO` is a genuine win beyond tidiness: a Quran app that no longer asks for the
microphone is a materially easier Play Store review and a better privacy story.

**`app/src/main/java/com/example/di/AppModule.kt`** — remove the import
`com.example.accessibility.VoiceCommandManager` and the whole `provideVoiceCommandManager`
provider. **Keep `provideSpeechManager` exactly as it is.**

**`app/src/main/java/com/example/MainActivity.kt`** — remove the `RECORD_AUDIO` branch from
`checkAndRequestPermissions()` and from the permission-result callback, including the announcement
`"تنبيه: صلاحية الميكروفون مطلوبة لتفعيل الأوامر الصوتية."`

⚠️ **Read this callback carefully before editing** — there is a coupling that is easy to get wrong:
```kotlin
if (!notificationsGranted && recordAudioGranted) {
    viewModel.announce("تنبيه: صلاحية الإشعارات مطلوبة لتشغيل الصوت في الخلفية.")
}
```
The notification warning today fires **only if the microphone was granted**. Delete
`recordAudioGranted` naively and you either lose the notification warning or change when it
fires. The correct result: warn on `!notificationsGranted`, unconditionally. Background playback
matters to every user; it never had anything to do with the microphone. State in your report
which form you chose.

If `permissionsToRequest` can now be empty on pre-Tiramisu devices, make sure the launcher is not
called with an empty array.

### EDIT — UNVERIFIED (I could not read these — B-13)

| File | Size | What to expect |
|---|---|---|
| `ui/viewmodel/QuranViewModel.kt` | 36,616 B | almost certainly injects `VoiceCommandManager`, holds listening state, and routes `VoiceCommandResult` into playback actions. **The largest part of this job.** |
| `ui/screens/QuranPlayerScreen.kt` | 22,122 B | likely a mic button, a listening indicator, and the `VoiceCommandGuideSheet` call site |
| `ui/components/player/PlayerControlPanel.kt` | 11,087 B | likely the mic control itself |
| `res/values*/strings.xml` | — | voice-command strings, now orphaned |

**Report the full diff for each.** I have not seen this code and will not pass a gate on an
unseen diff.

---

## 2. ORDER OF OPERATIONS — this matters

Do **not** start by deleting files. `AppModule` and the ViewModel reference them; deleting first
gives you a wall of unresolved-reference errors with no signal in it.

1. `grep -ri "VoiceCommand\|SpeechRecognizer\|RECORD_AUDIO\|RecognitionService" app/src` —
   paste the complete output in your report **before** changing anything. This is the real scope;
   §1 is only what I could see.
2. Remove call sites first: ViewModel, then screen, then components.
3. Then `AppModule`, `MainActivity`, `AndroidManifest.xml`.
4. Then delete the 4 files.
5. Then `./gradlew :app:assembleDebug`.
6. Re-run the grep from step 1. It must come back empty except for anything you deliberately kept
   (say what and why).

---

## 3. FORBIDDEN

- ❌ Deleting or modifying `SpeechManager.kt`, `AccessibilityAnnouncements.kt`,
  `TalkBackCompositionLocal.kt`, `HapticFeedbackManager.kt`.
- ❌ Removing `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE*`, `WAKE_LOCK` or `VIBRATE`. Only
  `RECORD_AUDIO` goes.
- ❌ Touching anything under `shared/` or `iosApp/`. This order is Android-only.
- ❌ "Tidying" adjacent accessibility code while you are in there. Every extra line widens the
  regression surface of a change to a shipping app.
- ❌ Running any test. Test execution stays with Claude Code.
- ❌ Writing voice/speech-recognition code for iOS. Not in this order, not in any future one.

---

## 4. ACCEPTANCE CRITERIA

| # | Criterion | Evidence |
|---|---|---|
| A1 | `:app:assembleDebug` → BUILD SUCCESSFUL | verbatim output |
| A2 | grep for `VoiceCommand\|SpeechRecognizer\|RECORD_AUDIO\|RecognitionService` under `app/src` returns nothing | grep output, before **and** after |
| A3 | `SpeechManager.kt` byte-identical | `git diff` on that path is empty |
| A4 | `LocalTalkBackEnabled` still provided in `MainActivity`; TalkBack detection intact | file content |
| A5 | Manifest keeps every permission except `RECORD_AUDIO`; `<queries>` gone | manifest diff |
| A6 | The notification-warning condition change is stated explicitly | report |
| A7 | 4 files deleted, no more, no fewer | `git status` |
| A8 | `./gradlew :app:testDebugUnitTest` green | **Claude Code only** — after the above |

**A3 is the one I will check first.**

---

## 5. BLOCKED ON

**B-13 — staging depth.** Files 8+ folders below the connected folder cannot be read by Claude
Code. That is exactly where `QuranViewModel.kt`, `QuranPlayerScreen.kt`, `PlayerControlPanel.kt`
and `VoiceCommandGuideSheet.kt` live — the files holding most of this job. I can see their names
and sizes and nothing else.

**Fix, one click:** connect `F:\AI PROJECTS\Blind App\app\src\main\java\com\example` as an
additional folder in the Claude desktop app. Until then this order runs half-blind and the gate
depends on whatever diff the executor chooses to paste.

**B-10 — `device_bash` is down** on this machine (`no Plan9 drive shares mounted`), even for
`echo`. Claude Code cannot delete files or run Gradle. This is why the order exists as a document
instead of as a change already on disk.

---

## 6. DOWNSTREAM

- **ORDER-P0-003 is cancelled.** Its payload was porting the parser. Marked on the order itself.
- **ORDER-P0-003b must be written** — domain models + `QuranRepository` + Koin, minus anything
  voice. Note that with `VoiceCommandParser` gone, `SharedModule` has no first binding; P0-003b
  has to decide what it registers.
- **Audit B-08 needs rewriting.** It currently covers `SpeechManager` (TTS) *and*
  `VoiceCommandManager` (SFSpeechRecognizer) as one blocker. Half of it is now void. What remains
  on iOS is: detect VoiceOver (`UIAccessibility.isVoiceOverRunning` +
  `voiceOverStatusDidChangeNotification`) and a fallback `AVSpeechSynthesizer` for when it is off —
  i.e. `SpeechManager`'s actual job, which is screen-reader awareness, not speech input.
- **`NSMicrophoneUsageDescription` and `NSSpeechRecognitionUsageDescription` are no longer needed**
  in the iOS `Info.plist`. They were never added — `iosApp/project.yml` does not declare them.
  Keep it that way.
- **Phase 4 of the phased plan ("Speech I/O") should be deleted**, not rescheduled.
