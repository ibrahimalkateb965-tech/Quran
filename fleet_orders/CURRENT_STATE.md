# Blind App — Current State

**Last updated:** 2026-09-11 (session 3, Claude Code CLI) · maintained by Claude Code CLI (Fleet Commander)
**Read this first.** It is the single source of truth for where the iOS/KMP programme stands.

---

## 1. THE ONE THING TO DO NEXT

**As of 2026-09-11 17:25 (session 4):**

- **Phase 0 orders all gated, including P0-004:** P0-001, P0-002, P0-003b, P0-004, P0-005, P0-006 —
  **QUALITY GATE: PASS** each (§5). `0b62eed` (P0-005) is on origin; the P0-004 commit follows it
  locally until Ibrahim pushes.
- **P0-004 premise correction (found 2026-09-11 session 4):** the Retrofit/Moshi/OkHttp path in `:app`
  was **dead code** — `QuranRepositoryImpl` reads `assets/quran/quran_uthmani_tanzil.json` → Room and
  never injected `QuranApiService`; zero consumers anywhere in `app/src`. Ibrahim chose **option A:
  retire only, no Ktor port**. Ktor in `:shared` (P0-002) is now the programme's only network stack and
  currently has no caller — that is fine; audio streams by URL straight into ExoPlayer.
- **Next orders — the `:app` cut-overs, one at a time, each with the full regression gate**
  (`:app` 35 tests + `assembleDebug` + `assembleRelease` + `:shared` 29 tests):
  1. **P0-007** `SessionPreferences` → `SessionStore` over `createSecureStore` (call
     `SecureStoreAndroid.init(context)` from the `Application`; same file name → zero migration).
  2. **P0-008** `AyahCard`/repository `sanitizeUthmanicText` → `domain.text.sanitizeUthmanicText`
     (UthmanicTextTest 5/5 must stay green — byte-for-byte rule, CLAUDE.md §4.4).
  3. **P0-009** Android `Surah`/`Ayah`/`Reciter`/`SurahData` → `:shared` models.
  Assign to OpenCode CLI while B-17 stands — but see **B-18** (§6): its shell wrapper now blocks
  `gradlew`, so orders must say "no compile probes; Claude Code compiles at the gate".
- **Uncommitted, not mine, Ibrahim's call:** Antigravity's OpenRouter fallback edits (12:50–12:52) to
  `CLAUDE.md`, `fleet_config.json`, `opencode.json`, `.agents/MEMORY_STORE.md`,
  `.agents/ACTIVE_CONTEXT_INJECTION.md`; plus `.agents/HOOKS_GUIDE.xlsx`,
  `remote_ios_dev_playbook_diagram.html`, and `hs_err_pid*.log` / `replay_pid*.log` litter (now 7 files
  after today's OOM kills).

### Real commit state (verified by Claude Code CLI, 2026-09-11 session 3)

The earlier claim "nothing is committed yet" was **wrong**. `git ls-files` + `git log` show:

- **`a819f65`** — `feat(kmp): add :shared module + iOS scaffold; remove voice commands`. Contains
  **all** of P0-001 and P0-006 (`shared/build.gradle.kts`, `iosApp/*`, root `build.gradle.kts`,
  `settings.gradle.kts`, `gradle/libs.versions.toml`, the 4 voice-file deletions). 323 files,
  +96,958 / −1,078. **Already on `origin/feat/ios-kmp-phase0`.** Do not rewrite it.
  Two blemishes it carries, both neutralised on top rather than by history rewrite:
  - **B-16 actually happened**: 12 generated files under `shared/build/**` (`classes.jar`,
    merged manifests, `.transforms/*.bin`, `R.txt`) were committed and pushed.
  - ~95 % of the diff is `.agents/skills/**` (TTF fonts, `google-font-licenses.json`,
    `phosphor-icons-upstream.json`) plus junk: `.coverage` and 3× `__pycache__/*.pyc`.
- **`0669720`** `chore(shared): add shared/.gitignore to keep shared/build out of VCS (B-16)` —
  replaces the misleadingly-titled local-only `2ee1a62` (rewritten via `reset --soft` +
  recommit; it was never on a remote). Adds `shared/.gitignore` and `git rm -r --cached` of the
  12 `shared/build/**` files, `.coverage` and the 3 `.pyc` files (17 files, +1 / −63). Nothing
  deleted on disk. `git ls-files shared/build` now prints nothing.
- **`docs(fleet): update CURRENT_STATE after B-16 and B-06 checks`** — this file + the handoff.
  Both commits are local (`[ahead 2]`), **not pushed** — per the handoff, Ibrahim pushes.

Loose ends left for Ibrahim: `.coverage` and `__pycache__/` now show as untracked (they exist on
disk and no `.gitignore` rule covers them — add one or delete them). `.agents/skills/**` and
`testers.csv`/`testers.xlsx` (28 tester e-mails, tracked since before this branch) are Ibrahim's
call — flagged, not touched.

Unrelated working-tree changes deliberately left alone: `.agents/HOOKS_GUIDE.xlsx` (modified),
`remote_ios_dev_playbook_diagram.html` (untracked, 736 KB).

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
- **ADR-004 — iOS UI is native SwiftUI (decided by Ibrahim 2026-09-11).** `:composeApp` is
  **never created**; `:app` stays plain Android Compose; iOS screens live in `iosApp/` over the
  `:shared` XCFramework. Reason: CMP on iOS synthesises the VoiceOver tree over a Skia canvas
  (Flutter's approach, less mature); for an app where the screen reader is the interface, only a
  native UI keeps VoiceOver quality under our control. Full ADR in
  `docs/IOS_KMP_READINESS_AUDIT.md` §4.
- **`:app` is not renamed**, iOS project is XcodeGen (`project.yml`, never a committed `.pbxproj`).
- **Stack is KMP, not Flutter** — because the Android app already ships. Flutter would mean
  rewriting a working product to reach a second platform.

---

## 4. OPEN DECISIONS

- **ADR-001 — Uthmanic text shaping on iOS.** Still needs the real-device spike (ORDER-P1-005,
  Antigravity). Under ADR-004 it is tested with SwiftUI `Text` + `uthman_taha.ttf` under CoreText.
- **ADR-002 — Room vs SQLDelight vs no DB.** Audit recommends (c) no DB. Not yet formally closed;
  P0-002/P0-003b must not add a persistence library while it is open.

*ADR-004 was closed 2026-09-11 — see §3. The note that `:shared`'s payload shrank after the voice
parser was deleted (domain models, `QuranRepository`, `sanitizeUthmanicText`, EveryAyah URL
scheme, network client — a few hundred lines) stands and is recorded in the ADR's consequences.*

---

## 5. ORDERS

| Order | State |
|---|---|
| P0-001 — module skeleton | ✅ PASS |
| P0-002 — Ktor foundation | ✅ **QUALITY GATE: PASS** 2026-09-11 12:39 (session 3). OpenCode CLI built it; Commander applied 4 small amendments (Ktor **3.2.3** — 3.2.0 breaks D8 on minSdk 24; `expectSuccess = true`; injectable `warn`; test import). `:shared` 5/5, `:app` 35/35, `assembleDebug` green. Retrofit still serves 100 % of traffic. Full verdict at the end of `reports/ORDER_P0_002_REPORT_OPENCODE.md`. |
| P0-003 — domain core + Koin | ⛔ **CANCELLED** — its whole payload was porting the voice parser |
| P0-003b — domain + Koin, voice-free | **Written** — `ORDER_P0_003B_DOMAIN_CORE.md` (Antigravity). Ports `Surah`/`Ayah`/`Reciter`/`SurahData`, `QuranRepository` interface (bookmarks omitted — Room), canonical `sanitizeUthmanicText` (= `AyahCard.kt` pass 2, a verified superset of the repository's pass 1), `ayahAudioUrl`, empty `sharedModule` + `initKoin`. Blocked on P0-002 gate. |
| P0-004 — retire dead Retrofit path | ✅ **QUALITY GATE: PASS** 2026-09-11 17:19 (session 4). Re-scoped from "migrate to Ktor" to **delete** after the premise proved false (zero consumers). OpenCode CLI did Steps 1–5 (3 `git rm`, −7/−13/−12 lines in `app/build.gradle.kts` / `proguard-rules.pro` / `libs.versions.toml`), then was OOM-killed before reporting; Commander verified every diff and wrote `reports/ORDER_P0_004_REPORT_OPENCODE.md`. `:app` 35/35, `assembleDebug` + **`assembleRelease` (R8)** green, `mapping.txt` has 0 retrofit/moshi classes, `:shared` 29/29. First order to touch `app/`. |
| P0-005 — SecureStore | ✅ **QUALITY GATE: PASS** 2026-09-11 13:07 (session 3). OpenCode CLI built it. `SecureStore` interface + `expect createSecureStore`, Android actual over `EncryptedSharedPreferences` (**fail-open** to plain prefs), iOS Keychain actual (unverified locally — CI), `SessionStore` with verbatim legacy migration. `:shared` 29/29, `:app` 35/35, `assembleDebug` green. `SessionPreferences.kt` untouched — cut-over is a later order. |
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
- **B-10 re-confirmed 2026-09-11 (session 2)** — same Plan9 error on the first `device_bash` call.
- **B-10 cause confirmed by the app itself (2026-09-11, session 3).** The Cowork handoff states the
  Windows update of **2026-09-08** breaks the Cowork VM's Plan9 mount — matching #92984 /
  KB5124008 above. **Claude Code CLI is unaffected**: git, Gradle and tests all ran natively in
  session 3 (`assembleDebug` 3 s cached, `testDebugUnitTest` 2 m 04 s, 35 tests / 0 failures).
  The Testing Monopoly is therefore back with Claude Code CLI; Ibrahim no longer needs to paste
  build output.
- **Staging depth limit is 7 folders below a connected root.** `app\src\main\java\com\example\<pkg>\File.kt`
  is 8–9 deep, so with only `F:\AI PROJECTS\Blind App` connected, every file under a sub-package
  (`data/`, `ui/`, …) is unreachable. That is the real reason B-13 needed the second folder.
  **The second folder must be re-connected in every new session**, or `SessionPreferences.kt`,
  `QuranViewModel.kt` etc. cannot be read.
- **B-06 — RESOLVED 2026-09-11 (session 3): `androidx.security.crypto` is USED, keep it.**
  `git grep -nE "EncryptedSharedPreferences|MasterKey|security\.crypto" -- app/src` → 12 hits,
  all in `app/src/main/java/com/example/data/local/SessionPreferences.kt` (lines 6–7, 15–24,
  30–38), plus a comment in `res/xml/backup_rules.xml:10` that excludes
  `mueen_session_prefs.xml` from Auto Backup for exactly this reason. Consumers:
  `di/AppModule.kt:44` (`provideSessionPreferences`) and `ui/viewmodel/QuranViewModel.kt:78`.
  Version `securityCrypto = "1.1.0-alpha06"` (`gradle/libs.versions.toml:37`), declared at
  `app/build.gradle.kts:114`. **Do not remove.**

  Design caveat for P0-005 (finding only — no change made): the encrypted payload is just
  `last_reciter_id`, `last_surah_id`, `last_ayah_index` — not secrets. `createSecurePrefs` is
  **fail-closed**: if Keystore init fails twice it throws `IllegalStateException` and the app
  cannot start. For a blind-first app, "resume position" is not worth a hard crash; the
  SecureStore order should decide whether the `actual` keeps encryption (parity) or degrades to
  plain `SharedPreferences` / `UserDefaults` on failure. Also verify the library's support status
  before pinning the KMP `actual` to it — `security-crypto` has been alpha-only for years.
- **B-17 — host memory (found 2026-09-11, session 3, open).** 15.8 GB machine at its commit limit
  (`vmmem` 4.1 GB, Chrome 1.6 GB, Antigravity IDE 1.5 GB, Claude 1.9 GB). Gradle daemon (`-Xmx4g`) +
  Kotlin daemon (`-Xmx3g`) + D8 crash with `G1 virtual space` mmap failures, and MockK's ByteBuddy
  self-attach fails because no JVM can be spawned. **Rules:** kill stale Gradle/Kotlin daemons before a
  gate run; never let two agents run Gradle at once; close the Cowork VM or Antigravity IDE for a full
  `assembleDebug`. Also: `agy`/OpenCode runs that go silent for >10 min are hung — kill and redo.
  **Session 4 addendum:** Claude Code's own *background* Bash jobs get killed by the harness's
  low-memory watchdog mid-Gradle ("stopped because the system is running low on memory") — the Gradle
  daemon survives and finishes the build, but the shell that would report is gone. Run gate steps in the
  **foreground**, one `./gradlew` per call, and read results from the JUnit XML / APK timestamps rather
  than from `UP-TO-DATE`.
- **B-18 — OpenCode's `lean-ctx` shell wrapper blocks `gradlew` (found 2026-09-11 17:02, session 4,
  open).** Error: `'gradlew' is not in the shell allowlist … permanent restriction`. It ran fine for
  P0-002/003b/005 this morning, so the cause is something changed since — most likely Antigravity's
  uncommitted `opencode.json` edits (12:50). Fix is one of: `lean-ctx allow gradlew`, or
  `shell_allowlist = []` in `C:\Users\Kt\.config\lean-ctx\config.toml` (file does not exist yet — built-in
  defaults are in effect). **Ibrahim's call.** Until then, orders to OpenCode must not include compile
  probes; Claude Code compiles at the gate.
- **B-13 — RESOLVED 2026-09-11.** `…\app\src\main\java\com\example` is connected as a second
  folder. Every source file is now readable and writable.
- OpenCode CLI is configured for `opencode/muse-spark-1.3-contributor-free` in `opencode.json`
  but **has no API key yet** — `opencode auth login`, free, email only, no card.
- Build warnings worth knowing, none blocking: `TYPE_ANNOUNCEMENT` deprecated,
  `PhoneStateListener` deprecated, `abandonAudioFocus` deprecated, and Gradle features incompatible
  with Gradle 10. (The Moshi codegen warning is gone — P0-004 removed Moshi.)

---

## 7. HOW THIS PROGRAMME GOES WRONG

Two days produced four orders, an audit and an execution report before a single build ran — and
the first real build immediately exposed two P0 errors that had been sitting in the plan. Written
orders are not evidence. A green build is. Verify early, and treat any order written against
documentation rather than a working build as unproven.
