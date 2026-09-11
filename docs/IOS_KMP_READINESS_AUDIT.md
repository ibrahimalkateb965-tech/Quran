# iOS / KMP Readiness Audit — Blind App
**Auditor:** Claude Code CLI (Opus 5) · **Date:** 2026-09-10 · **Mode:** Devil's Advocate
**Revision B** — 2026-09-10, after reading the build files and part of the source directly from disk.
Rev. A was written from directory structure and dependency names. Rev. B corrects it where
first-hand reading contradicted inference. Every correction is marked ⟲.
**Scope:** Readiness of `F:\AI PROJECTS\Blind App` for a Kotlin Multiplatform + Compose Multiplatform iOS target.

---

## 0. Verdict

**Build-level iOS readiness: ~15%.**

The *content* of this app is highly portable — the Quran JSON, the Uthmanic font pipeline,
the command parser, the domain models and the accessibility documentation all cross platforms cleanly.
The *build and platform layer* is 100% Android-specific and shares nothing with iOS today.

This is not a migration. It is a **re-platforming of the infrastructure around a reusable core.**
Any plan that treats it as "add an iOS target" will fail at the DI layer within the first day.

⟲ **Rev. B amendment — the reusable core is smaller than rev. A claimed.**
Rev. A rated `sanitizeUthmanicText` as a 100%-reusable asset "with its test". On reading the test file,
that test does not exercise the production function at all (**B-12**). The Uthmanic sanitisation logic is
therefore *untested*, not *tested and portable*. Readiness of the **content** layer is unchanged; confidence
in it is materially lower.

---

## 1. P0 BLOCKERS

### B-01 — Stack contradiction already committed to the repo
`REMOTE_IOS_DEV_PLAYBOOK.md` (37 KB, modified 3 days ago) and `.github/workflows/ios_build.yml`
(23 KB, modified 2 days ago) are built end-to-end around **Flutter**, not KMP.
The Hook 22 configuration itself asserts `flutter analyze = 0` as a quality gate.

Two mutually exclusive iOS strategies are currently live in one repository.
**Decision required before any code is written.** Recommendation:

- **Keep** playbook §4 (Apple credentials without a Mac), §5–6 (GitHub secrets), §8 (CI cheat sheets),
  §9 (troubleshooting) — these are stack-agnostic and are the single most valuable asset in the repo.
  The `-legacy` OpenSSL `.p12` note alone saves a day of misdirected debugging.
- **Rewrite** §3.4–3.7 (Flutter toolchain, scaffold, pubspec) and the `flutter build ipa` job.
- **Archive** the Flutter-specific sections under `docs/archive/` rather than deleting them.

### B-02 — Hilt cannot cross to iOS
`hilt-android:2.60.1` is JVM/Android-only. Every `@HiltAndroidApp`, `@AndroidEntryPoint`,
`@HiltViewModel` and `AppModule` / `NetworkModule` must be replaced.
Target: **Koin** (`koin-core` in `commonMain`, `koin-android`, `koin-compose-viewmodel`).
This is the largest single refactor in the DI and ViewModel layers and it touches every screen.

### B-03 — No multiplatform module structure
`settings.gradle.kts` includes exactly one module, `:app`, applying `com.android.application`.
There is no `:shared`, no `org.jetbrains.kotlin.multiplatform`, no `org.jetbrains.compose`,
no `iosArm64` / `iosSimulatorArm64` / `iosX64` targets, and no `XCFramework` or `iosApp/` Xcode project.

### B-04 — Audio layer is entirely Android
`QuranAudioService.kt`, `MediaButtonPolicy.kt`, `CallEndTracker.kt` depend on
`androidx.media3` + `MediaSessionService` + Android `AudioFocus`. None of it crosses.
iOS requires `AVPlayer` + `AVAudioSession(.playback)` + `MPNowPlayingInfoCenter` +
`MPRemoteCommandCenter` + `AVAudioSession.interruptionNotification`.

> **Trap:** on iOS, background audio requires `UIBackgroundModes: [audio]` in `Info.plist`.
> A missing key is not a warning — playback silently stops the moment the screen locks.
> For a blind-first app, silent failure is the worst possible failure mode.

### B-05 — Network stack is JVM-only
`Retrofit` + `OkHttp` + `Moshi` cannot compile for Kotlin/Native.
Target: **Ktor Client** (`OkHttp` engine on Android, `Darwin` engine on iOS) + `kotlinx.serialization`.
Scope is small — `BASE_URL` is one API plus the static EveryAyah URL scheme — so this is a
cheap and low-risk swap if done before the domain layer moves.

### B-06 — Encrypted storage — ⟲ **DOWNGRADED: UNVERIFIED, not a confirmed blocker**
Rev. A asserted this as a confirmed P0 blocker. That assertion was inferred from the dependency list alone
and **the evidence does not support it.**

`androidx.security.crypto` is declared in `app/build.gradle.kts` — but
`app/src/main/java/com/example/security/` is an **empty directory**, and so is `util/`.
There is no `EncryptedSharedPreferences` code where rev. A assumed it lived. The dependency may simply be
unused, in which case B-06 is not a blocker but a stale dependency to delete.

`data/local/SessionPreferences.kt` is the file that settles this, and it is **unreadable under B-13**.

Status: **needs verification before ORDER-P0-005 is written.** If it turns out to be real, the remedy is
unchanged — `expect interface SecureStore` → Android `EncryptedSharedPreferences` / iOS Keychain
(`kSecClassGenericPassword`) — and the mitigating argument still holds: `TrialManager` and the PIN system
were removed, so the remaining secure surface is last-read position and preferences. Encrypting a bookmark
is ceremony, not security.

### B-07 — Accessibility semantics do NOT auto-translate (highest product risk)
`blindAccessibleClickable` implements a **TalkBack** interaction model:
single tap announces, double tap activates. On iOS, VoiceOver *already* does exactly this natively.
Porting the shim as-is will produce double announcements and swallowed activations —
i.e. the custom component that makes the app excellent on Android will make it **worse** on iOS.

`clearAndSetSemantics` and `customActions` map onto the VoiceOver **rotor**, with different
ordering and discovery rules. This layer must be `expect/actual`, never shared, and it
cannot be validated by any automated test — only by a sighted-off VoiceOver pass on a real device.

### B-08 — Speech I/O is Android-only
`SpeechManager.kt` (TTS) and `VoiceCommandManager.kt` (`SpeechRecognizer`, `<queries>` manifest
declarations) → iOS `AVSpeechSynthesizer` and `SFSpeechRecognizer`.
Requires `NSMicrophoneUsageDescription` **and** `NSSpeechRecognitionUsageDescription`;
a missing usage-description string is an immediate process kill on iOS, not a permission denial.
Arabic on-device recognition quality differs from Android and must be re-benchmarked, not assumed.

### B-09 — Build environment will OOM on Kotlin/Native
`gradle.properties` currently sets `-Xmx2048m`, `org.gradle.workers.max=2`,
and `kotlin.compiler.execution.strategy=in-process`.
Two JVM crash dumps (`hs_err_pid14964.log`, `hs_err_pid22496.log`) are already sitting in the
project root — the current configuration is at its limit **before** adding iOS targets.
Kotlin/Native linking is substantially heavier than JVM compilation. Raise to ≥ 6 GB and
drop the `in-process` strategy, or expect intermittent, hard-to-attribute build failures.

### B-10 — Test execution channel is currently broken
`device_bash` cannot mount `F:\AI PROJECTS\Blind App` in this session
(`no Plan9 drive shares mounted`). File read/write works; **shell execution on the maintainer's
machine does not.** Since Claude Code holds the exclusive test-execution mandate, this is an
operational blocker for Quality Gate G2. Until it is fixed, tests must be run locally by the
maintainer and the **verbatim output** pasted back for adjudication — a summary is not acceptable evidence.

⟲ Re-confirmed 2026-09-10 (rev. B): still failing, identical error.

### B-11 — ⟲ **NEW** — non-ASCII `rootProject.name` on a Windows host with spaces in the path
`settings.gradle.kts:25` sets `rootProject.name = "القرآن للمكفوفين"`, and the repository lives at
`F:\AI PROJECTS\Blind App` — two spaces in the path.

This value is not cosmetic. It propagates into Kotlin/Native framework naming, into derived build-directory
paths, and into the Xcode `embedAndSignAppleFrameworkForXcode` build-phase script. Non-ASCII plus spaces plus
Kotlin/Native plus Windows is a well-known source of failures that surface as unrelated-looking link errors.

Severity **P0 for iOS, zero impact on Android.** Remedied in ORDER-P0-001 Step 3 by setting
`rootProject.name = "QuranBlind"`. The **user-visible** app name is unaffected — it lives in
`app/src/main/res/values*/strings.xml` and in the iOS `CFBundleDisplayName`.

### B-12 — ⟲ **NEW** — the Uthmanic text tests do not test the Uthmanic text code
**This is the most serious finding in rev. B.**

`app/src/test/java/com/example/UthmanicTextTest.kt` contains five tests. Three of them claim to verify
`sanitizeUthmanicText`. None of them call it. Each re-implements the transformation inline in the test body
and then asserts on its own local result:

```kotlin
val input  = "ءَامَنُوٓا۟\u0600 إِذَا"
val output = input.replace('\u06DF', '\u06E0').replace("\u0600", "")
assertEquals("ءَامَنُوٓا۠ إِذَا", output)
```

This asserts that Kotlin's `String.replace` works. It would pass unchanged if the production sanitiser were
subtly wrong, or deleted outright. The same pattern repeats for the idgham/ikhfa sukoon rule and the madd
normalisation — the two most intricate rules in the file.

**Why this is P0 rather than a testing nit:** Uthmanic text integrity is a stated product constraint of this
app. Rev. A recommended porting this logic to `commonMain` "byte-for-byte, and keep the test". Doing so would
carry a *false* safety signal onto a second platform, on the one component where a silent defect alters the
text of the Qur'an — for users who cannot see the error.

Required before any port: locate the production `sanitizeUthmanicText`, rewrite the three tests to call it
with the same inputs, and record whether they still pass. **If they fail, that is the finding**, and the port
must stop until the function is correct.

### B-13 — ⟲ **NEW** — source files below `data/` and `domain/` are unreadable by the auditor
File staging from the connected folder is limited to **7 directory levels**.
`app/src/main/java/com/example/data/**` and `.../domain/**` sit at level 8.

Consequently the following were **not** read in rev. B and every statement about them remains inference:
`QuranApiService.kt`, `AlQuranCloudResponse.kt`, `data/repository/QuranRepository.kt`,
`domain/repository/QuranRepository.kt`, `SessionPreferences.kt`, `Surah.kt`, `Ayah.kt`, `Reciter.kt`,
and all of `data/local/` (Room entities and DAOs).

Combined with B-10, Claude Code can neither execute the build nor read a third of the source. This is the
binding constraint on review quality, and it is why ORDER-P0-002 was split (foundation now, endpoint
migration deferred to ORDER-P0-004) and why ORDER-P0-003 demands full verbatim source in its report-back.

**Fix:** connect `F:\AI PROJECTS\Blind App\app\src\main\java\com\example` as an additional folder in
the Claude desktop app. Highest-leverage unblock currently available.

---

## 1b. CONFIRMED CODE DEFECTS — ⟲ **NEW SECTION** (read first-hand, rev. B)

| ID | Location | Defect | Status |
| :--- | :--- | :--- | :--- |
| **BUG-01** | `VoiceCommandParser.kt:64` | `\$` inside a Kotlin **raw string** is a literal `$` character, not the end-of-input anchor. Arabic speech input never contains `$`, so that alternation branch can never match and the lazy `(.+?)` falls through to the fallback regex on line 68. | Fix ordered — P0-003 Step 4(a) |
| **BUG-03** | `VoiceCommandParser.kt:153–168` | `normalizeArabicText` compiles `Regex("[ًٌٍَُِّْـ]")` on every call, and `matchesAny` invokes it once per synonym per command — ≈60 regex compilations per spoken command. Latency is the primary UX quality in a voice-driven blind-first app. | Fix ordered — P0-003 Step 4(b) |
| **B-05a** | `NetworkModule.kt:43` | `Thread.sleep((1000 * tryCount).toLong())` — `Thread` does not exist in Kotlin/Native, and blocking a thread inside a coroutine is wrong on Android too. Must become `delay()`. | Fix ordered — P0-002 |
| **B-05b** | `NetworkModule.kt:41` | `android.util.Log.w` — Android-only; needs `expect/actual`. | Fix ordered — P0-002 |
| **B-05c** | `NetworkModule.kt:14, 62` | `BuildConfig.DEBUG` / `BuildConfig.BASE_URL` — AGP-generated, does not exist for iOS. Must become an injected config object. | Fix ordered — P0-002 |
| **I-10** | `VoiceCommandParser.kt:3` | Unused `import com.example.data.model.Reciter`. | Fix ordered — P0-003 Step 4(c) |
| **I-11** | `VoiceCommandParser.kt:167` | `.lowercase()` is locale-sensitive on JVM, locale-invariant in Kotlin/Native. On Arabic input it is a no-op either way, so **not** a behaviour risk — recorded so a later cleanup does not mistake it for load-bearing. | Recorded, deliberately not fixed |

⟲ **Structural correction to B-02.** Rev. A stated that "`AppModule` / `NetworkModule` must be replaced" as
Hilt modules. `NetworkModule` **is not a Hilt module.** It is a plain Kotlin `object` with `by lazy`
singletons, carrying no `@Module` or `@InstallIn` annotation, reached statically by its call sites. The Ktor
swap therefore does not have to fight Dagger — but every call site references it globally, and those call
sites are unreadable under B-13. `AppModule` and `RepositoryModule` *are* real Hilt modules; six of
`AppModule`'s seven providers take `@ApplicationContext Context` and are Android-bound by construction.

---

## 2. P1 ISSUES

| # | Issue | Impact |
| :--- | :--- | :--- |
| I-01 | `namespace = "com.example"` while `applicationId = "com.aistudio.quranblind.a11y"` | ⟲ Confirmed by reading `app/build.gradle.kts:14`. Deliberately **out of scope** for Phase 0: changing it moves generated `BuildConfig`/`R` packages and touches source imports across the app. (Referenced as "I-09" in ORDER-P0-001 §4 — that is this same issue; **I-01 is the canonical ID**.) |
| I-02 | `Room 2.7.0` is KMP-capable but wired Android-only via KSP | See ADR-002 below. Room may not be needed at all. |
| I-03 | Tests are `Robolectric` + `Roborazzi` — Android-only | Shared logic tests must move to `commonTest` with `kotlin.test`. Screenshot tests stay Android-side. |
| I-04 | `AGP 9.2.1` + `Kotlin 2.2.10` + `Compose BOM 2025.01.00` + Gradle 9.4.1 | ⟲ All four verified from `libs.versions.toml` and `gradle-wrapper.properties`. Compose Multiplatform must be pinned from the official compatibility matrix, not guessed — which is why AD-2 defers `:composeApp` out of Phase 0 entirely. |
| I-05 | `web_ios/` PWA duplicates the same 2 MB `quran.json` and the font | Three copies of the corpus will drift. Single-source them under shared resources. |
| I-06 | `java.time` / Android-only date APIs (if present in session logic) | Replace with `kotlinx-datetime` in `commonMain`. |

---

## 3. REUSABLE ASSETS — the good news

| Asset | Reuse | Note |
| :--- | :--- | :--- |
| `quran_uthmani_tanzil.json` (2 MB) | 100% | Move to `composeResources`, single-sourced |
| `VoiceCommandParser.kt` (9.5 KB) | ~100% | ⟲ **Verified by reading it: zero Android imports.** Genuinely portable today — the best-factored file in the repo. Carries BUG-01 and BUG-03, both fixed on the way across (P0-003). |
| `VoiceCommandParserTest.kt` (6.4 KB) | ~90% | ⟲ **NEW ROW.** Real tests — they call the production class. 20+ assertions on reciter variants; the most valuable coverage in the repo. Blocked only by **MockK** and **JUnit**, both JVM-only: needs a hand-written fake and `kotlin.test`. |
| `sanitizeUthmanicText` | logic only | ⟲ **CORRECTED — rev. A was wrong.** Its test does **not** exercise it (**B-12**). The `uni06DF` knowledge is still valuable, but it must be re-tested against the real function *before* porting, not after. Do not "keep the test". |
| Domain models + `QuranRepository` interface | ~95% | Clean Architecture is paying off here |
| EveryAyah URL scheme | 100% | HTTPS, so iOS ATS-compliant with no exception needed |
| `ACCESSIBILITY_GUIDELINES_IOS.md` (29 KB) | 100% | Already written for VoiceOver parity; stack-agnostic |
| `ios_build.yml` credential/signing plumbing | ~60% | Keep signing and upload; replace the Flutter build step |
| Playbook §4–6 (Apple credentials without a Mac) | 100% | Stack-agnostic and hard to reproduce |
| `uthman_taha.ttf` | 100% file | **But** iOS text shaping of Uthmanic diacritics is unproven — see ADR-001 |

---

## 4. DECISIONS REQUIRED (ADRs)

### ADR-001 — Uthmanic text shaping on iOS *(P0, spike before committing)*
Android's text stack renders `uthman_taha.ttf` correctly after `sanitizeUthmanicText`.
CoreText applies different mark positioning and cluster rules. Superscript alef, the
small round zero (`uni06DF`), and mad marks are the exact glyphs that break first.
**Do not build the UI before proving this.** Ship a one-screen iOS spike rendering
Al-Baqarah 1–5 and compare against the Android screenshot at pixel level.
If CoreText mispositions marks, the entire typography approach changes — and finding that
out in month two instead of week one is the difference between a delay and a rewrite.

### ADR-002 — Room KMP vs SQLDelight vs no database at all
The dataset is a static 2 MB JSON asset plus a tiny mutable session state
(last surah, last ayah, reciter). This does not need a relational database on either platform.
Options: (a) Room KMP 2.7 + bundled SQLite driver, (b) SQLDelight, (c) drop the DB —
in-memory index over the JSON + a key-value `SecureStore`.
**Recommendation: (c).** It removes KSP-per-target complexity, one dependency, and one
whole class of migration bugs. Devil's Advocate position: Room here is over-engineering
carried over from the Android build, not a requirement.

### ADR-003 — Module topology — ⟲ **RESOLVED 2026-09-10 (ORDER-P0-001 rev. B)**
Rev. A proposed a four-module split: `:shared`, `:composeApp`, `:androidApp`, `iosApp/`.
**Decided otherwise, on two grounds:**

- **AD-1 — `:app` is NOT renamed to `:androidApp`.** The rename touches CI workflows, `.idea`, signing paths
  that reference `rootDir`, and Play Store upload paths, for zero architectural gain. The Android app is
  live in production; Phase 0 must not put it at risk for cosmetics.
- **AD-2 — `:composeApp` is NOT created in Phase 0.** An empty Compose Multiplatform module doubles the
  Kotlin/Native compile surface before any domain code exists, and pins a CMP↔Kotlin compatibility pair that
  is not yet needed (see I-04). Compose Multiplatform enters in Phase 3.

**Resulting Phase 0 topology:** `:app` (unchanged, Android) + `:shared` (KMP) + `iosApp/` (XcodeGen shell).

**AD-4** — the Xcode project is declared in `iosApp/project.yml` via **XcodeGen**, not a hand-written
`project.pbxproj`. A `pbxproj` cannot be validated on Windows and is unreviewable in diff; XcodeGen is
declarative text that regenerates deterministically on the macOS-15 runner.

---

## 5. PHASED PLAN

| Phase | Content | Gate |
| :--- | :--- | :--- |
| **0** | Module skeleton, version catalog, Gradle memory + compiler-strategy fix, ASCII root name, Ktor foundation, domain core + Koin | ⟲ **Gate corrected.** Rev. A required "`:shared` compiles for `iosSimulatorArm64`" — **impossible on this machine** (see the hard constraint below). Actual gate: `:shared:compileDebugKotlinAndroid` **and** `:app:assembleDebug` both succeed, with no new `hs_err_pid*.log`. iOS compilation is verified on CI, not locally. |
| **1** | Domain + `VoiceCommandParser` + `sanitizeUthmanicText` to `commonMain`; Hilt → Koin | `commonTest` green under Claude Code |
| **1.5** | **ADR-001 spike:** Uthmanic rendering on a real iOS device | Visual parity confirmed, or stack decision revisited |
| **2** | `expect/actual` `AudioEngine` (ExoPlayer / AVPlayer) + `SecureStore` | Background playback survives lock screen and an incoming call |
| **3** | Compose Multiplatform UI port; `expect` accessibility layer | TalkBack parity retained, VoiceOver parity established |
| **4** | Speech I/O (`SFSpeechRecognizer` / `AVSpeechSynthesizer`) | Arabic command recognition benchmarked, not assumed |
| **5** | CI rewrite, TestFlight, blind-user acceptance testing | `QUALITY GATE: PASS` |

**Do not compress 1.5 into 3.** It is the cheapest possible moment to discover a fatal
typography constraint, and the most expensive one to discover late.

---

## 6. ⟲ HARD PLATFORM CONSTRAINT (rev. B)

**Kotlin/Native cannot compile iOS targets on Windows.** Not slowly, not with a workaround — not at all.

Every iOS *compilation* result in this programme is produced by the GitHub Actions `macos-15` runner.
On the maintainer's machine the iOS targets are only ever *declared*, so that module topology is correct
and CI has something to build. `kotlin.native.ignoreDisabledTargets=true` (ORDER-P0-001 Step 1) is what lets
Gradle configuration succeed regardless.

Two operational consequences, both binding on the whole fleet:

1. **`./gradlew build` is forbidden on this repository.** It attempts `linkDebugFrameworkIosArm64` and fails.
   Every order specifies narrowly-scoped tasks instead.
2. **No local quality gate can assert anything about iOS.** Any order whose acceptance criteria mention an
   iOS build artefact is malformed. Gates assert Android compilation plus "no Android regression"; iOS
   verification happens on CI, or it has not happened.

---

## 7. ⟲ REVISION LOG

**Rev. B — 2026-09-10.** Read from disk: `gradle.properties`, `settings.gradle.kts`, root and app
`build.gradle.kts`, `libs.versions.toml`, `gradle-wrapper.properties`, `di/AppModule.kt`,
`di/NetworkModule.kt`, `accessibility/VoiceCommandParser.kt`, `accessibility/VoiceCommandParserTest.kt`,
`UthmanicTextTest.kt`, plus full directory listings of `app/src`.

Added: B-11, B-12, B-13, §1b (BUG-01, BUG-03, B-05a–c, I-10, I-11), §6, §7.
Corrected: B-06 (downgraded to unverified), B-02 (NetworkModule is not a Hilt module), I-01, I-04,
§3 reusable assets (`sanitizeUthmanicText` claim withdrawn; `VoiceCommandParserTest` added),
ADR-003 (resolved), Phase 0 gate (was unachievable).

Still unread, still inference only — see B-13: everything under `data/` and `domain/`.
