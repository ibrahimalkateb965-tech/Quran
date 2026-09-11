# Blind App — Current State

**Last updated:** 2026-09-11 · maintained by Claude Code CLI (Fleet Commander)
**Read this first.** It is the single source of truth for where the iOS/KMP programme stands.

---

## 1. THE ONE THING TO DO NEXT

**Commit the work.** Two orders are complete and verified but nothing is committed yet:

```powershell
cd "F:\AI PROJECTS\Blind App"
git add -A app/src shared iosApp build.gradle.kts settings.gradle.kts gradle.properties gradle/libs.versions.toml .gitignore
git status          # review before committing
```

Then decide ADR-004 (§4) — it gates Phase 3 and costs nothing to settle now.

---

## 2. WHAT IS DONE

**ORDER-P0-001 — QUALITY GATE: PASS** (2026-09-11, 9/9 criteria).
`:shared` KMP module exists and configures; `:app` still assembles (4m 24s, 50 tasks, no
regression); the full iOS target surface — including `embedAndSignAppleFrameworkForXcode` — is
configured on Windows. Full detail in `ORDER_P0_001_EXECUTION_REPORT.md`.

Two P0 defects were caught by that gate, both of them errors **in the order itself**:
- **B-14** — AGP 9 dropped KMP compatibility with `com.android.library`. The module uses
  `com.android.kotlin.multiplatform.library` with `kotlin { android { … } }` instead.
- **B-15** — the root `build.gradle.kts` must declare the Kotlin plugins with `apply false`,
  or a subproject's versioned request fails ("already on the classpath with an unknown version").
  That file was not even listed in the order.

**ORDER-P0-006 — voice-command removal — COMPLETE and verified 2026-09-11.**
7 files edited (−11,315 bytes), 4 files deleted via `git rm`, `:app:assembleDebug` BUILD
SUCCESSFUL, and `git grep` for `VoiceCommand|SpeechRecognizer|RECORD_AUDIO|RecognitionService`
across `app/src` returns **empty**.

Notable: the voice UI was **already dead before removal**. `QuranPlayerScreen.kt` imported
`BigVoiceMicrophoneButton` and `ListeningVoiceBanner` and collected `voiceUiState`, but each name
appeared exactly once in the file — the import line. The microphone button was never rendered.

One self-inflicted failure worth recording: the import-cleanup script dropped
`androidx.compose.runtime.getValue` from `PlayerControlPanel.kt` because it searched for the
symbol as literal text, and `getValue` is used *implicitly* by `by` delegation. Five delegations
broke. **Any import-pruning must hard-exclude `getValue`, `setValue`, `provideDelegate`** and
anything else resolved by convention rather than by name. A second lesson: the first fix commit
reported success but wrote stale content — **verify file content after writing, never trust the
success message.**

---

## 3. DECISIONS ALREADY TAKEN

- **Voice commands are cancelled** by the client. None on Android, none ever on iOS. No
  `NSMicrophoneUsageDescription` / `NSSpeechRecognitionUsageDescription` in the iOS plist.
- **The app relies on the phone's own screen reader** (TalkBack / VoiceOver). That is the product
  principle, not an implementation detail.
- **`SpeechManager.kt` stays.** Despite the name it is not a voice-command component: it detects
  TalkBack and silences the app's internal TTS while a screen reader is running, and
  `MainActivity` feeds its `isTalkBackEnabledFlow` into `LocalTalkBackEnabled` for the whole
  Compose tree. Deleting it breaks the app. This is the single most likely mistake for anyone
  doing a fast pass over `accessibility/`.
- **`:app` is not renamed**, `:composeApp` is not created yet, iOS project is XcodeGen
  (`project.yml`, never a committed `.pbxproj`).
- **Stack is KMP, not Flutter** — because the Android app already ships. Flutter would mean
  rewriting a working product to reach a second platform.

---

## 4. OPEN DECISIONS

**ADR-004 — Compose Multiplatform vs native SwiftUI for the iOS UI. Still open, and it gates
Phase 3.**

The assumption that KMP buys better accessibility than Flutter is **false as stated**: Compose
Multiplatform on iOS renders to its own canvas and synthesises an accessibility tree for
VoiceOver — structurally the same approach as Flutter, and less mature (CMP iOS went stable in
May 2025; Flutter has shipped iOS a11y for years). The advantage only exists if the iOS UI is
native SwiftUI/UIKit.

For an app where the screen reader *is* the interface, that matters. Counter-consideration: the
requirement is feature parity with Android — but since the app delegates to the system screen
reader, and TalkBack and VoiceOver have different conventions (action lists vs rotor), literal
interaction parity would be *wrong* on iOS. Parity belongs in content, features and flows;
interaction should be native to each platform.

**A second question worth answering honestly:** with `VoiceCommandParser` deleted, the shared
payload has shrunk a lot. The audit had called it "the single highest-value, lowest-risk asset in
the repository for KMP migration". What is left to share is domain models, the `QuranRepository`
interface, `sanitizeUthmanicText`, the EveryAyah URL scheme and the network client — a few hundred
lines. `:shared` still prevents the two apps drifting on domain rules, but the justification is
materially weaker than when the plan was written, and that should be said out loud rather than
assumed away.

---

## 5. ORDERS

| Order | State |
|---|---|
| P0-001 — module skeleton | ✅ PASS |
| P0-002 — Ktor foundation | Unblocked, not started. Low urgency: Retrofit works on Android; Ktor is only needed for iOS. |
| P0-003 — domain core + Koin | ⛔ **CANCELLED** — its whole payload was porting the voice parser |
| P0-003b — domain + Koin, voice-free | To be written. Note `SharedModule` now has no first binding to register. |
| P0-004 — endpoint/repository migration | Was blocked on B-13; **B-13 is resolved**, so it can now be specified properly |
| P0-005 — SecureStore | Blocked on verifying B-06. `security/` and `util/` are empty directories, so `androidx.security.crypto` may simply be an unused dependency. `SessionPreferences.kt` settles it. |
| P0-006 — voice removal | ✅ **COMPLETE** — build green, grep empty |

**P0-002 and P0-003b must run sequentially, not in parallel** — both edit
`shared/build.gradle.kts` and `gradle/libs.versions.toml`.

---

## 6. OPERATIONAL

- **B-10 — ROOT CAUSE IDENTIFIED 2026-09-11. Not a local misconfiguration.**
  `device_bash` fails even for `echo` with `sandbox-helper: no Plan9 drive shares mounted under
  /mnt/.virtiofs-root/shared`. This is a known, open Anthropic bug:
  - [#93221](https://github.com/anthropics/claude-code/issues/93221) — identical error, same
    Claude Desktop **1.49585.0** and Electron **44.2.0** as this machine. Survives app restart,
    service restart, full reboot, and VM re-download.
  - [#92984](https://github.com/anthropics/claude-code/issues/92984) — same failure traced to
    Windows update **KB5124008** (build 26200.9445), which ships new Plan9 binaries
    (`p9rdr.sys`, `p9np.dll`, `vmcompute.exe`). `wusa /uninstall /kb:5124008` + reboot is
    reported to fix it immediately.
  - [#92958](https://github.com/anthropics/claude-code/issues/92958) — the ARM64 equivalent
    (KB5124012). This machine is x64, so #92984 is the likely match.

  **The uninstall removes a September 2026 cumulative *security* update.** That is a real
  tradeoff, not a free fix, and Windows will try to reinstall it. Confirm the KB is actually
  present on this machine before acting.

  Until resolved: Claude Code can read and write files through the bridge but **cannot run Gradle
  and cannot delete files**. Builds are run by Ibrahim and pasted back verbatim; a summary is not
  accepted as gate evidence.
- **B-13 — RESOLVED 2026-09-11.** `…\app\src\main\java\com\example` is connected as a second
  folder. Every source file is now readable and writable.
- OpenCode CLI is configured for `opencode/muse-spark-1.3-contributor-free` in `opencode.json`
  but **has no API key yet** — `opencode auth login`, free, email only, no card.
- Build warnings worth knowing, none blocking: `TYPE_ANNOUNCEMENT` deprecated,
  `PhoneStateListener` deprecated, `abandonAudioFocus` deprecated, Moshi KAPT codegen deprecated
  (moot once P0-004 retires Moshi), and Gradle features incompatible with Gradle 10.

---

## 7. HOW THIS PROGRAMME GOES WRONG

Two days produced four orders, an audit and an execution report before a single build ran — and
the first real build immediately exposed two P0 errors that had been sitting in the plan. Written
orders are not evidence. A green build is. Verify early, and treat any order written against
documentation rather than a working build as unproven.
