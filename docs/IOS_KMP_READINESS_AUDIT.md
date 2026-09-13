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
| I-02 | `Room 2.7.0` is KMP-capable but wired Android-only via KSP | ⟲ **DECIDED 2026-09-13 — ADR-005 (B):** Room leaves `:app`; bookmarks move to a multiplatform `BookmarkStore` in `:shared` with a lossless one-time Android migration. Closes on the ORDER-P1-012 gate. |
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

### ADR-002 — Room KMP vs SQLDelight vs no database at all — ⟲ **RESOLVED 2026-09-13: (c) no database in `:shared`**
The dataset is a static 2 MB JSON asset plus a tiny mutable session state
(last surah, last ayah, reciter). This does not need a relational database on either platform.
Options: (a) Room KMP 2.7 + bundled SQLite driver, (b) SQLDelight, (c) drop the DB —
in-memory index over the JSON + a key-value `SecureStore`.
**Recommendation: (c).** It removes KSP-per-target complexity, one dependency, and one
whole class of migration bugs. Devil's Advocate position: Room here is over-engineering
carried over from the Android build, not a requirement.

**Resolution (2026-09-13, read from the tree at `69ff703`).** Option **(c)** is what was built:

- `shared/build.gradle.kts` declares neither Room nor SQLDelight. The `libs.versions.toml` Room
  coordinates (2.7.0) are consumed by `:app` only (`app/build.gradle.kts:111-112,151`).
- **Static text** — `JsonQuranRepository` (commonMain) over `QuranJsonSource`; Android reads the asset,
  iOS reads the same file from the app bundle via `BundleQuranJsonSource` (iosMain). `iosApp/project.yml`
  references `../app/src/main/assets/quran/quran_uthmani_tanzil.json` as a resource, so the repository
  holds exactly one copy of the Uthmanic text (CLAUDE.md §4.4).
- **Mutable session state** — `SessionStore` (commonMain; `last_reciter_id`, `last_surah_id`,
  `last_ayah_index` in store `mueen_session_prefs`) over `interface SecureStore` +
  `expect fun createSecureStore(name)`: `EncryptedSharedPreferences` on Android, Keychain (`SecItem*`) on iOS.
  Delivered by ORDER-P0-005 / P0-007 / P0-010 / P1-008; host-tested by `JsonQuranRepositoryTest`,
  `SessionStoreTest` (`InMemorySecureStore`).
- **Out of scope of this ADR:** the Android-only Room layer in `app/src/main/java/com/example/data/local/`
  (`QuranDatabase`, `AyahDao`, `BookmarkDao`, wired in `di/AppModule.kt`) still exists. It is not shared
  and its retirement is a separate `:app` decision (I-02 stays open until then).

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

### ADR-004 — iOS UI: Compose Multiplatform vs native SwiftUI — ⟲ **RESOLVED 2026-09-11: SwiftUI native**
Decided by Ibrahim on 2026-09-11 after the Fleet Commander presented both options.

**Context.** The original case for KMP over Flutter was "better accessibility". That claim is false
as stated: Compose Multiplatform on iOS draws to its own Skia canvas and *synthesises* an
accessibility tree for VoiceOver — structurally the same approach as Flutter, and younger (CMP iOS
stable since May 2025). KMP only beats Flutter on accessibility when the iOS UI is native.
For this product the screen reader *is* the interface, so that is the deciding property.

**Decision.** iOS screens are written in **SwiftUI** inside `iosApp/`, on top of the `:shared`
XCFramework. **`:composeApp` is never created.** `:app` stays a plain Android Compose app.

**Consequences.**
- Phase 3 becomes "SwiftUI screens over `:shared`", not "Compose Multiplatform port". The row in §5
  is corrected below.
- `:shared` is justified as the guardian of domain rules (`sanitizeUthmanicText`, `QuranRepository`,
  the EveryAyah URL scheme, network client) — a smaller payload than the plan assumed, said out loud.
- The UI is written twice. Accepted: the surface is small (player, surah/reciter lists, settings),
  and the cost is known up front rather than hidden in an accessibility bridge we do not own.
- Interaction parity between platforms is explicitly **not** a goal (CLAUDE.md §3: "Never share the
  interaction model"). Parity lives in content, features and flows; TalkBack action lists and the
  VoiceOver rotor stay native to each platform.
- ADR-001 (Uthmanic shaping) is now tested with SwiftUI `Text` + `uthman_taha.ttf` under CoreText —
  no Skia text-shaping risk on iOS.
- Any future proposal to revisit this needs a real-device VoiceOver spike (rotor, custom actions,
  announcements) accepted by a blind user before a single screen is written.

### ADR-005 — Retire Room from `:app`; bookmarks become a multiplatform `BookmarkStore` in `:shared` — ⟲ **DECIDED 2026-09-13: Alternative (B)**
Decided by Ibrahim on 2026-09-13 (session 14) after the Fleet Commander presented three alternatives.
Closes **I-02**. Implementation is **ORDER-P1-012** (contract at the end of this ADR); this revision
changes documentation only.

#### Context (read from the tree at `040fc29`)

Room `2.7.0` survives in `:app` only (`app/build.gradle.kts:111-112` `room.ktx`/`room.runtime`,
`:151` `ksp(room.compiler)`; the KSP plugin itself stays — Hilt needs it). It backs one database,
`quran_a11y_database` (`data/local/QuranDatabase.kt`, schema version 3, `fallbackToDestructiveMigration`),
with two tables that carry two unrelated responsibilities:

1. **`ayahs` (`AyahDao`/`AyahEntity`) — a derived cache that is never read.** `QuranRepositoryImpl.getAyahs`
   parses `assets/quran/quran_uthmani_tanzil.json` on every call, writes the parsed rows into `ayahs`
   (`insertAyahs`) and consults the table only if the asset parse returned an empty list — which cannot
   happen for a bundled, verified asset. `:shared` already owns this read path: `JsonQuranRepository`
   (commonMain) applies the same `sanitizeUthmanicText` and the same `ayahAudioUrl` scheme
   (`resolveAudioEndpoint` in `:app` is a character-for-character duplicate). The cache is redundant twice over.
2. **`bookmarks` (`BookmarkDao`/`BookmarkEntity`) — real user data.** Columns: `id` (autogenerated, unused by
   any caller), `surahId`, `surahNameAr`, `ayahNumber`, `timestamp`, `note` (always `""`). Read as
   `ORDER BY timestamp DESC`; identity in practice is the pair `(surahId, ayahNumber)`
   (`deleteBySurahAndAyah`, `isBookmarked`). A blind user navigates by these; losing them on upgrade is a
   Quality Gate failure under CLAUDE.md §4.1, independent of any test result.

**Architecture leak.** `BookmarkEntity` — a Room `@Entity` in `com.example.data.local` — is imported by
`ui/viewmodel/QuranViewModel.kt` (`StateFlow<List<BookmarkEntity>>`) and by the composable
`ui/components/BookmarksSheet.kt` (parameter type). The persistence schema is the UI contract. The `:app`
`domain/repository/QuranRepository.kt` interface likewise exposes `Flow<List<BookmarkEntity>>` from the
domain layer. Any storage change today forces a UI change; that is the dependency direction Clean
Architecture forbids, and it is why I-02 could not be closed by a data-layer-only edit.

**Why now.** ADR-002 (c) established "no database in `:shared`" with `SecureStore`/`SessionStore` as the
key-value pattern and `JsonQuranRepository` as the text source. iOS (Phase 3, SwiftUI) will need bookmarks
too; if they stay in Android-only Room, iOS gets a second implementation and the domain model drifts.

#### Alternatives considered

- **(A) Keep Room, close I-02 as "accepted".** Cheapest today. Rejected: permanent Android/iOS drift in
  user data, the UI leak stays, and `ksp(room.compiler)` + two artifacts remain in `:app` for a cache
  nobody reads.
- **(B) Remove Room entirely; bookmarks move to `:shared` as a key-value-backed store with a one-time
  Android migration. — CHOSEN.**
- **(C) SQLDelight in `:shared`.** Contradicts ADR-002 (c); a relational engine for a list of a few
  hundred small records is the same over-engineering ADR-002 already rejected.

#### Decision

**D-1 — Room leaves `:app` completely.** `androidx.room.ktx`, `androidx.room.runtime` and
`ksp(androidx.room.compiler)` are removed from `app/build.gradle.kts`; the `room` version and the three
library aliases are removed from `gradle/libs.versions.toml` (no other module consumes them). The package
`com.example.data.local` (`QuranDatabase`, `AyahDao`, `AyahEntity`, `BookmarkDao`, `BookmarkEntity`) is
deleted, together with `provideQuranDatabase`/`provideAyahDao`/`provideBookmarkDao` in `di/AppModule.kt`.
Post-condition: `grep -rn "androidx.room" app/ gradle/` is empty; the release `mapping.txt` contains no
`androidx.room` class.

**D-2 — Domain model.** `:shared` commonMain gains `domain/model/Bookmark.kt`:

```kotlin
@Serializable
data class Bookmark(
    val surahId: Int,
    val ayahNumber: Int,
    val surahNameAr: String,
    val createdAtEpochMillis: Long,
    val note: String = "",
)
```

Identity is `(surahId, ayahNumber)`; the legacy autogenerated `id` is dropped (no caller reads it).
`surahNameAr` is carried over verbatim so the existing TalkBack strings in `BookmarksSheet`
(`"سورة ${surahNameAr}، الآية ${ayahNumber}"`) render byte-identically.

**D-3 — `BookmarkStore` in `:shared` commonMain (`store/BookmarkStore.kt`), same pattern as `SessionStore`.**
Backed by `SecureStore` (namespace `mueen_bookmarks`), one key `bookmarks_v1` holding the whole list as a
`kotlinx.serialization` JSON array, plus a flag key `legacy_room_migrated` (`"1"` once D-5 has completed).
Public surface:

```kotlin
class BookmarkStore(private val store: SecureStore, private val now: () -> Long) {
    val bookmarks: StateFlow<List<Bookmark>>          // newest first (createdAtEpochMillis DESC)
    fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean
    fun toggle(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean   // returns the new state
    fun importLegacy(items: List<Bookmark>)           // idempotent merge, existing keys win
    fun isLegacyMigrated(): Boolean
    fun markLegacyMigrated()
    companion object { STORE_NAME; KEY_BOOKMARKS; KEY_LEGACY_MIGRATED }
}
```

Every mutation rewrites the key and emits on `bookmarks`; `toggle` reproduces the exact semantics of the
old `QuranRepositoryImpl.toggleBookmark` (delete by pair if present, else insert; return the new state).
A corrupt or unparsable value is treated as an empty list **but is not overwritten** until the first
successful mutation (fail-safe, never fail-destructive). `now` is injected so `commonTest` is deterministic;
Android passes `System::currentTimeMillis`, iOS `{ (NSDate().timeIntervalSince1970 * 1000).toLong() }`
inside the Koin module. The store is host-tested in `commonTest` (`BookmarkStoreTest` over
`InMemorySecureStore`): toggle on/off, ordering, JSON round-trip, idempotent import, corrupt-value tolerance,
flag semantics.

**D-4 — UI and domain consume `Bookmark` only.** `QuranViewModel` takes `BookmarkStore` (Hilt
`@Provides`, alongside `provideSessionStore`), `bookmarks: StateFlow<List<Bookmark>>` is derived from
`bookmarkStore.bookmarks`, `isCurrentAyahBookmarked` becomes a `combine` of the store flow and the active
ayah (replacing the three point-in-time `repository.isBookmarked` reads, which is also what makes D-5
race-free from the UI's point of view), and `toggleBookmark` calls `bookmarkStore.toggle`.
`BookmarksSheet(bookmarks: List<Bookmark>, …)` — the composable body is otherwise untouched (composable
files are Commander-only per CLAUDE.md §2). `com.example.domain.repository.QuranRepository` loses
`allBookmarks`/`toggleBookmark`/`isBookmarked` and keeps only the inherited read contract;
`QuranRepositoryImpl` loses both DAO constructor parameters and the `ayahs` cache branch (the asset JSON
read path stays as it is in this order — switching `:app` onto `JsonQuranRepository` is a separate,
behaviour-neutral follow-up, see Consequences). Post-condition: `grep -rn "data.local" app/src` is empty.

**D-5 — Deterministic, lossless, one-time Android migration** (`app/.../data/migration/LegacyRoomBookmarkMigrator.kt`,
Android-only, no Room API):

1. Runs once per process from `QuranBlindApp.onCreate` on `Dispatchers.IO` (application scope), after
   `SecureStoreAndroid.init(this)`. Guard: `if (bookmarkStore.isLegacyMigrated()) return`.
2. `val file = context.getDatabasePath("quran_a11y_database")`. If `!file.exists()` →
   `markLegacyMigrated()`, return (fresh install; nothing to lose).
3. Open read-only with `SQLiteDatabase.openDatabase(file.path, null, OPEN_READONLY)`;
   `SELECT surahId, surahNameAr, ayahNumber, timestamp, note FROM bookmarks`; map each row to `Bookmark`
   (`timestamp` → `createdAtEpochMillis`). If the `bookmarks` table does not exist (schema never created)
   treat as zero rows.
4. `importLegacy(rows)`; then **verify**: for every legacy `(surahId, ayahNumber)` the store's
   `isBookmarked` is `true`, and the persisted JSON re-reads with ≥ that many entries. Only if
   verification passes: `markLegacyMigrated()` and `context.deleteDatabase("quran_a11y_database")`
   (removes the file and its `-wal`/`-shm`/`-journal` siblings; the `ayahs` cache disappears with it — it is
   derived data). Log one line at `Log.i` with the migrated count.
5. Any exception at steps 3–4 → log at `Log.w`, leave the database file **and** the flag untouched, and
   retry on the next launch. The app never starts without its bookmarks and never deletes what it has not
   proven it copied. Because D-4 derives the UI state from the store flow, a migration that completes a few
   milliseconds after first composition is reflected automatically — no restart, no manual refresh.

Host-tested in `:app` (`LegacyRoomBookmarkMigratorTest`, Robolectric): a fixture SQLite file with the
version-3 `bookmarks` schema and N rows is migrated → N bookmarks in the store, order preserved by
timestamp, the file is gone, the flag is set; a second run is a no-op; a fixture with a locked/corrupt file
leaves file and flag untouched.

#### Consequences

- `:app` loses two runtime artifacts, one annotation processor pass and ~120 lines of `data.local`;
  `:shared` gains one model, one store and one test class. iOS Phase 3 can wire `BookmarkStore` into
  `KoinIos.kt` with zero new domain code.
- The persistence namespace `mueen_bookmarks` is separate from `mueen_session_prefs`; `SessionStore.clear()`
  keeps its current meaning (session only).
- Follow-up (not part of P1-012): switch `QuranRepositoryImpl`'s asset parse onto `JsonQuranRepository`
  through an `androidMain` `QuranJsonSource` over `Context.assets` — removes the last `org.json` duplicate.
- Rejected shortcut, recorded so it is not re-proposed: "just stop writing to `ayahs` and keep Room for
  bookmarks" — leaves the UI leak, the KSP pass and the Android-only user data in place.

#### Quality Gate & acceptance criteria (Commander executes; delegates build only)

- [ ] **Zero data loss:** `LegacyRoomBookmarkMigratorTest` green; plus a manual gate on the
      Commander's device/emulator — install the current APK, create ≥ 3 bookmarks, install the P1-012 APK
      over it, open the bookmarks sheet: same entries, same order, TalkBack strings unchanged;
      `run-as … ls databases/` shows no `quran_a11y_database*`.
- [ ] **Clean Architecture:** `grep -rn "data.local\|androidx.room" app/src app/build.gradle.kts gradle/libs.versions.toml` empty;
      no `ui/` or `domain/` file imports anything from a `data.*` package for bookmarks.
- [ ] **`:shared` tests green** — `./gradlew :shared:testAndroidHostTest` (currently 47/47) + `BookmarkStoreTest` (≥ 6 cases).
- [ ] **`:app` unit tests green** — `./gradlew :app:testDebugUnitTest` (currently 35/35) + `LegacyRoomBookmarkMigratorTest`;
      `QuranRepositoryTest` re-pointed to the two-argument constructor.
- [ ] **`:app:assembleDebug` under the 2 GB ceiling** —
      `./gradlew :app:assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m" -Dkotlin.daemon.jvmargs="-Xmx2048m"`
      `BUILD SUCCESSFUL`; debug dex contains `store/BookmarkStore` and no `androidx/room`.
- [ ] **Blind-first:** bookmark toggle announcement, `BookmarksSheet` labels/hints and focus order
      unchanged (TalkBack walk-through on the Commander's device).
- [ ] `assembleRelease` only if ≥ 5 GB free (B-20); if run, `mapping.txt` has zero `androidx.room` classes.

#### ORDER-P1-012 — task contract (to be issued per TEMPLATE_02; summary here so the ADR is self-contained)

| Slice | Owner | Files | Gate evidence |
| :--- | :--- | :--- | :--- |
| 1. `:shared` — `domain/model/Bookmark.kt`, `store/BookmarkStore.kt` | OpenCode CLI (Kotlin, non-UI) | commonMain only | Commander writes `BookmarkStoreTest` first (TDD), runs `:shared:testAndroidHostTest` |
| 2. `:app` data layer — delete `data/local/*`, shrink `QuranRepositoryImpl` + domain interface, `AppModule` providers, `LegacyRoomBookmarkMigrator`, `QuranBlindApp.onCreate` hook, Gradle/catalog removal | OpenCode CLI | `app/src/main/java/com/example/{data,domain,di}/**`, `QuranBlindApp.kt`, `app/build.gradle.kts`, `gradle/libs.versions.toml` | Commander writes `LegacyRoomBookmarkMigratorTest`, re-points `QuranRepositoryTest`, runs `:app:testDebugUnitTest` |
| 3. `:app` UI — `QuranViewModel.kt` bookmark flows, `BookmarksSheet.kt` parameter type, `QuranPlayerScreen.kt` call site | Claude Code CLI (composables are Commander-only) | `ui/**` | `assembleDebug` 2 GB, TalkBack walk-through, migration device gate |

Slices 1 → 2 → 3 are sequential (2 depends on the `Bookmark`/`BookmarkStore` API from 1; 3 compiles only
after 2). OpenCode verification is compile-only (`:shared:compileAndroidMain`, `:app:compileDebugKotlin`);
no delegate runs a test. Diff bounds are set in the order file from the line counts above.

---

## 5. PHASED PLAN

| Phase | Content | Gate |
| :--- | :--- | :--- |
| **0** | Module skeleton, version catalog, Gradle memory + compiler-strategy fix, ASCII root name, Ktor foundation, domain core + Koin | ⟲ **Gate corrected.** Rev. A required "`:shared` compiles for `iosSimulatorArm64`" — **impossible on this machine** (see the hard constraint below). Actual gate: `:shared:compileDebugKotlinAndroid` **and** `:app:assembleDebug` both succeed, with no new `hs_err_pid*.log`. iOS compilation is verified on CI, not locally. |
| **1** | Domain + `VoiceCommandParser` + `sanitizeUthmanicText` to `commonMain`; Hilt → Koin | `commonTest` green under Claude Code |
| **1.5** | **ADR-001 spike:** Uthmanic rendering on a real iOS device | Visual parity confirmed, or stack decision revisited |
| **2** | `expect/actual` `AudioEngine` (ExoPlayer / AVPlayer) + `SecureStore` | Background playback survives lock screen and an incoming call |
| **3** | ⟲ *(ADR-004)* **SwiftUI screens in `iosApp/` over the `:shared` XCFramework**; `expect` accessibility layer. No `:composeApp`. | TalkBack behaviour on Android unchanged; VoiceOver on iOS native (rotor, custom actions) and accepted by a blind user |
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

**Rev. D — 2026-09-13.** ADR-005 added (Room retirement from `:app`, alternative (B) decided by Ibrahim),
I-02 row updated to point at it. No other section changed.

**Rev. C — 2026-09-13.** ADR-002 resolved as (c) "no database in `:shared`", with the evidence
listed under the ADR. No other section changed.

**Rev. B — 2026-09-10.** Read from disk: `gradle.properties`, `settings.gradle.kts`, root and app
`build.gradle.kts`, `libs.versions.toml`, `gradle-wrapper.properties`, `di/AppModule.kt`,
`di/NetworkModule.kt`, `accessibility/VoiceCommandParser.kt`, `accessibility/VoiceCommandParserTest.kt`,
`UthmanicTextTest.kt`, plus full directory listings of `app/src`.

Added: B-11, B-12, B-13, §1b (BUG-01, BUG-03, B-05a–c, I-10, I-11), §6, §7.
Corrected: B-06 (downgraded to unverified), B-02 (NetworkModule is not a Hilt module), I-01, I-04,
§3 reusable assets (`sanitizeUthmanicText` claim withdrawn; `VoiceCommandParserTest` added),
ADR-003 (resolved), Phase 0 gate (was unachievable).

Still unread, still inference only — see B-13: everything under `data/` and `domain/`.
