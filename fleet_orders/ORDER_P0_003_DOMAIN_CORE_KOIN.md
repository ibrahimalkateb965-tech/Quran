# ORDER-P0-003 — Domain Core Extraction & Koin Foundation

> # ⛔ CANCELLED — 2026-09-11
>
> **Do not execute this order. Not any part of it.**
>
> The client cancelled voice commands. `VoiceCommandParser.kt`, `VoiceCommandResult`,
> `VoiceCommandManager.kt` and `VoiceCommandParserTest.kt` are being **deleted from the Android
> app**, not migrated. Steps 4, 5 and 6 — and BUG-01, BUG-03 and I-10 with them — now describe
> work on code that will not exist.
>
> **What survives, and moves to ORDER-P0-003b:**
> - Steps 1–2: Koin in the version catalog and `shared/build.gradle.kts`
> - Step 3: moving `Surah`, `Ayah`, `Reciter`, `QuranRepository` into `commonMain`
> - Step 7: `SharedModule.kt` — but it can no longer register `VoiceCommandParser`, which leaves
>   it with nothing to register. P0-003b has to decide what the first Koin binding actually is.
>
> **What must NOT be deleted along with the voice code: `SpeechManager.kt`.** Despite the name it
> is not a voice-command component. It detects TalkBack and silences the app's own TTS while a
> screen reader is running, and `MainActivity` feeds its `isTalkBackEnabledFlow` into
> `LocalTalkBackEnabled` for the entire Compose tree. It *is* the implementation of "the app
> relies on the phone's screen reader". Deleting it breaks the app.


| Field | Value |
|---|---|
| Order ID | ORDER-P0-003 (rev. B) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **Antigravity IDE (Gemini)** |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | ⛔ **CANCELLED 2026-09-11 — DO NOT EXECUTE** |
| Supersedes | `ORDER-P0-003` in `fleet_orders/IOS_KMP_PHASE0_ORDERS.md` (rev. A assumed a `:composeApp` module that AD-2 cancelled) |
| Cancelled because | The client cancelled the voice-command feature. This order's entire payload was migrating `VoiceCommandParser` + `VoiceCommandResult` + their tests to `commonMain`. That code is being deleted, not ported. |
| Superseded by | ORDER-P0-003b (to be written) — domain models + `QuranRepository` + Koin foundation, **without** any voice-command content |
| Closes | B-02 (Hilt does not cross to iOS) — deferred to P0-003b |
| Raises | BUG-01, BUG-03, I-10, I-11 (see §7) |

---

## 0. GROUND TRUTH (read from disk 2026-09-10)

`VoiceCommandParser.kt` (9,512 B) and `VoiceCommandParserTest.kt` (6,418 B) were read in full.
`AppModule.kt` and `NetworkModule.kt` were read in full.

**The good news, stated precisely:** `VoiceCommandParser` has **zero Android imports**. Its only two imports
are `com.example.data.model.Reciter` (unused — see I-10) and `com.example.domain.repository.QuranRepository`
(a pure interface). It is genuinely portable today. This is the single highest-value, lowest-risk asset in the
repository for KMP migration, which is why it goes first.

**Hilt graph, as actually built:**
- `AppModule` (`@Module @InstallIn(SingletonComponent)`) provides `QuranDatabase`, `AyahDao`, `BookmarkDao`,
  `SessionPreferences`, `HapticFeedbackManager`, `SpeechManager`, `VoiceCommandManager`.
- `RepositoryModule` (`abstract`, `@Binds`) binds `QuranRepositoryImpl` → `QuranRepository`.
- **Six of the seven `AppModule` providers take `@ApplicationContext Context`.** They are Android-bound by
  construction and are explicitly **out of scope** for this order.

**⚠️ Files this order touches that I could NOT read** (operational blocker B-13, §8):
`data/model/Surah.kt`, `data/model/Ayah.kt`, `data/model/Reciter.kt`, `domain/repository/QuranRepository.kt`.
They sit 8 folders below the connected folder and the staging limit is 7.
Therefore **this order does not dictate their contents.** Where their code is involved you must **report what
you found** so I can audit the move at the gate. Do not treat my silence about them as approval.

Partial evidence I do have — from `VoiceCommandParserTest.kt:20`:
```kotlin
Surah(2, "البقرة", "Al-Baqarah", "سورة البقرة", 286, "مدنية", 2)
```
7 positional parameters. Use this to sanity-check that the file you move is the file I inferred.

---

## 1. OBJECTIVE

Stand up `shared/src/commonMain` as a real, compiling domain layer containing the voice-command parser, the
domain models it needs, and a Koin module — with tests that run in `commonTest` **without MockK and without
JUnit**. Android keeps working unchanged throughout.

**Explicit non-goal:** removing Hilt. Hilt stays and keeps running the Android app. This order makes Koin
*exist alongside it*, owning only the code that moved to `commonMain`. A Hilt→Koin cutover on Android is a
later order with its own regression gate. Attempting both at once is how a shipping app breaks.

---

## 2. FILES YOU OWN

```
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Surah.kt          CREATE (moved)
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Ayah.kt           CREATE (moved)
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Reciter.kt        CREATE (moved)
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt   CREATE (moved)
shared/src/commonMain/kotlin/com/aistudio/quranblind/voice/VoiceCommandResult.kt    CREATE (moved)
shared/src/commonMain/kotlin/com/aistudio/quranblind/voice/VoiceCommandParser.kt    CREATE (moved)
shared/src/commonMain/kotlin/com/aistudio/quranblind/di/SharedModule.kt             CREATE
shared/src/commonTest/kotlin/com/aistudio/quranblind/voice/VoiceCommandParserTest.kt CREATE (rewritten)
shared/src/commonTest/kotlin/com/aistudio/quranblind/voice/FakeQuranRepository.kt   CREATE
shared/build.gradle.kts                                                             MODIFY (deps only)
gradle/libs.versions.toml                                                           MODIFY (append only)
```

## 3. FORBIDDEN

- ❌ **Do not delete the Android originals in this order.** Copy up, do not move out. The Android app must keep
  compiling against its own copies until a later order redirects its imports under a passing gate. Temporary
  duplication is the intended, safe state at the end of P0-003.
- ❌ Do not touch `app/src/**` at all — not one import, not one annotation.
- ❌ Do not remove, weaken, or reorganise Hilt. `AppModule` and `RepositoryModule` stay byte-identical.
- ❌ Do not move `SpeechManager`, `VoiceCommandManager`, `HapticFeedbackManager`, `SessionPreferences`,
  `QuranDatabase`, or any DAO. All are `Context`-bound; they need `expect/actual` designs that are not
  yet decided (ADR-02 is still open on persistence).
- ❌ Do not add Ktor. Networking is P0-002's payload.
- ❌ **Do not run any test task.** Test execution is exclusively Claude Code's. You run only the two compile
  probes in §5, Step 7.
- ❌ Do not "improve" parser behaviour beyond the two named fixes in Step 4. Any other behaviour change
  invalidates the test-equivalence argument this whole order rests on.

---

## 4. STEPS

### Step 1 — Version catalog

Append to `[versions]`:
```toml
koin = "4.1.0"
```
Append to `[libraries]`:
```toml
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
koin-test = { group = "io.insert-koin", name = "koin-test", version.ref = "koin" }
```

> `4.1.0` is a **baseline, not a verdict.** Resolve the newest Koin compatible with Kotlin 2.2.10 and report
> what you actually pinned. If it does not resolve, report the failure — do not silently substitute a version.

### Step 2 — `shared/build.gradle.kts`

Add to the existing source-set block only:
```kotlin
commonMain.dependencies {
    // ...existing coroutines + serialization from P0-001...
    api(libs.koin.core)
}
commonTest.dependencies {
    // ...existing kotlin.test from P0-001...
    implementation(libs.koin.test)
}
```
`api` (not `implementation`) for `koin-core`: consumers of `:shared` — including the future iOS framework —
need Koin types on their compile classpath.

### Step 3 — Move the domain models and repository interface

Copy `Surah`, `Ayah`, `Reciter`, and the `QuranRepository` **interface** into `commonMain` under the new
package `com.aistudio.quranblind.domain.*`.

For each file, before you write it, check for and report any of:
- `android.*` or `androidx.*` imports
- `java.*` imports (`java.util.Date`, `java.io.Serializable`, `BigDecimal`, …)
- Moshi annotations (`@Json`, `@JsonClass`)
- `@Parcelize` / `Parcelable`

Handling rules:
- Moshi `@Json(name = "x")` → `@SerialName("x")`, and add `@Serializable` from
  `kotlinx.serialization`. Report every field you renamed.
- `java.*` types → Kotlin/stdlib or kotlinx-datetime equivalents. **If you hit a `java.*` type with no obvious
  common equivalent, STOP and report it.** Do not invent a substitute; a wrong model type silently corrupts
  parsed Quran data.
- `Parcelable`/`@Parcelize` → drop in `commonMain`. Report that you dropped it.

**Report the full source of all four files in your report-back.** I have not been able to read them and I will
not pass this gate on an unseen diff.

### Step 4 — Move `VoiceCommandParser` + `VoiceCommandResult`

`VoiceCommandResult` is referenced throughout the parser but is **not defined in `VoiceCommandParser.kt`**.
Locate its definition (most likely `accessibility/VoiceCommandManager.kt`), copy the sealed hierarchy into
`commonMain` unchanged, and report where you found it.

Copy `VoiceCommandParser` **verbatim** except for exactly these changes:

**(a) BUG-01 — broken end-of-input anchor. FIX THIS.**
Line 64 currently reads:
```kotlin
val regex = Regex("""(?:تشغيل|شغل|شغلي|افتح|اعرض)?\s*(?:سور[ةه]|صور[ةه])\s+(.+?)(?:\s+من|\s+آية|\s+الآية|\$)""")
```
Inside a Kotlin **raw string**, `\$` is a literal backslash followed by a dollar sign, which regex reads as an
*escaped literal `$` character* — not the end-of-input anchor. Arabic speech input never contains a `$`, so
that alternation branch can never match, and the lazy `(.+?)` falls through to the fallback regex on line 68.
Correct form in a raw string:
```kotlin
val regex = Regex("""(?:تشغيل|شغل|شغلي|افتح|اعرض)?\s*(?:سور[ةه]|صور[ةه])\s+(.+?)(?:\s+من|\s+آية|\s+الآية|${'$'})""")
```
⚠️ **This fix changes parser behaviour.** That is intended — but it means the existing test suite is no longer
a pure equivalence check. Add a test for `"تشغيل سورة البقرة"` **and** for a surah name at end-of-input, and
report any test whose result changed. If a test now fails, report it; do not adjust the test to match.

**(b) BUG-03 — regex recompilation on the hot path. FIX THIS.**
`normalizeArabicText` compiles `Regex("[ًٌٍَُِّْـ]")` on every invocation, and `matchesAny` calls it once per
synonym per command — roughly 60 regex compilations for a single spoken command. This is a voice-driven app
for blind users where response latency is the primary UX quality. Hoist the diacritics `Regex` to a
`companion object val`, and pre-normalise every synonym list once at class-init so `matchesAny` compares
already-normalised strings.

This is a pure-performance change: **it must not alter a single parser result.** If any test result changes,
you have introduced a defect — report it rather than adjusting the test.

**(c)** Drop the unused `import com.example.data.model.Reciter` (I-10).

**(d)** Update the remaining import to the new `com.aistudio.quranblind.domain.repository.QuranRepository`.

Change nothing else. Not the synonym lists, not the reciter matching, not `normalizeArabicText`'s replacement
rules, not the priority order in `parseCommand`.

### Step 5 — `FakeQuranRepository` (commonTest)

The existing test uses **MockK**, which is JVM-only and cannot run in `commonTest`. Replace it with a
hand-written fake implementing the full `QuranRepository` interface:
- `findSurahByName("البقرة")` returns the same `Surah(2, "البقرة", "Al-Baqarah", "سورة البقرة", 286, "مدنية", 2)`.
- Every other method returns an empty/null/no-op default, mirroring `mockk(relaxed = true)`.
- No `TODO()` and no `throw` in any method — `relaxed = true` never threw, and a throwing fake would change
  test outcomes for reasons unrelated to the parser.

### Step 6 — Port the test suite to `commonTest`

Translate `VoiceCommandParserTest.kt` mechanically:
- `org.junit.Test` → `kotlin.test.Test`
- `org.junit.Before` → `kotlin.test.BeforeTest`
- `org.junit.Assert.assertEquals` → `kotlin.test.assertEquals`
- `org.junit.Assert.assertTrue` → `kotlin.test.assertTrue`
- `mockk(relaxed = true)` + `every {…}` → `FakeQuranRepository()`

**Keep all 10 test methods and every assertion inside them.** Do not drop the long reciter test — 20+
assertions covering Husary/Minshawi/Abdulbasit variants is the most valuable coverage in the repo.

Then **add** the two new surah-anchor cases required by fix (a).

### Step 7 — `SharedModule.kt` (Koin)

```kotlin
package com.aistudio.quranblind.di

import com.aistudio.quranblind.voice.VoiceCommandParser
import org.koin.dsl.module

val sharedModule = module {
    factory { VoiceCommandParser(get()) }
}
```

`factory`, not `single`: `VoiceCommandParser` is stateless, and a factory avoids pinning a repository instance
into a long-lived Koin singleton before the repository's own lifecycle is decided (ADR-02 open).

`QuranRepository` is deliberately **not** bound here — its implementation is still Android/Room-bound. Consumers
supply it. This is the correct seam and must stay unbound until the persistence ADR closes.

### Step 8 — Compile probes (compile only, no tests)

```bash
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :app:assembleDebug
```
Nothing else. **Do not run `./gradlew build`** — it attempts iOS targets, which cannot compile on Windows
(see ORDER-P0-001 §2). Do not run any `test` task.

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion | Evidence |
|---|---|---|
| A1 | `:shared:compileDebugKotlinAndroid` → BUILD SUCCESSFUL | verbatim output |
| A2 | `:app:assembleDebug` → BUILD SUCCESSFUL (**no Android regression**) | verbatim output |
| A3 | No `android.*`, `androidx.*`, `java.*`, `io.mockk`, or `org.junit` reference anywhere under `shared/src/commonMain` or `shared/src/commonTest` | grep output |
| A4 | All 10 original test methods present in `commonTest`, plus ≥2 new surah-anchor cases | file content |
| A5 | `app/src/**` completely untouched | `git diff --stat -- app/src` is empty |
| A6 | Hilt `AppModule.kt` / `RepositoryModule` byte-identical | `git diff -- app/src/main/java/com/example/di` is empty |
| A7 | BUG-01 fixed using `${'$'}`, BUG-03 fixed by hoisting | file content |
| A8 | Full source of the 4 unread files (models + repository interface) reproduced in the report | report §3 |
| A9 | Koin version actually pinned is reported | report §2 |

**A2 and A5 are the ones that matter most.** This order adds a parallel module; if it disturbs the shipping
Android app in any way, it has failed regardless of how clean `commonMain` looks.

---

## 6. REPORT-BACK

```
ORDER-P0-003 REPORT — Antigravity IDE

1. BASELINE          git HEAD, working tree state
2. VERSIONS PINNED   koin = <actual>   (baseline 4.1.0; changed? why?)
3. MOVED FILES — FULL SOURCE
   For each of Surah.kt, Ayah.kt, Reciter.kt, QuranRepository.kt:
     - original source (verbatim)
     - final commonMain source (verbatim)
     - every change made and why (Moshi→kotlinx, java.* removals, Parcelable drops)
4. VoiceCommandResult — where found, and the sealed hierarchy verbatim
5. PARSER DIFF       unified diff, original → commonMain. Must show ONLY fixes (a)–(d).
6. TEST PORT         method-by-method mapping; the 2 new anchor cases; any behaviour change from BUG-01
7. PROBE OUTPUT      verbatim, both commands, warnings included
8. GREP              output proving A3
9. git diff --stat -- app/src        (must be empty)
10. DEVIATIONS       anything unauthorised, and why. "NONE" only if literally nothing.
11. BLOCKED ON
12. SELF-ASSESSMENT  A1–A9 PASS/FAIL
```

**Rules of engagement:** if a step fails, stop and report. Do not adjust a test to make it pass. Do not
substitute a version to clear a resolution error. Do not touch `app/src` to work around a compile error —
that is precisely the failure mode this order is shaped to prevent.

---

## 7. FINDINGS RAISED BY THIS REVIEW

**BUG-01 (P1)** — `\$` inside a raw-string regex, `VoiceCommandParser.kt:64`. Fixed in Step 4(a).

**BUG-03 (P2, blind-first UX)** — regex recompiled per call; ~60 compilations per spoken command. Fixed in Step 4(b).

**I-10 (P3)** — unused `import com.example.data.model.Reciter`. Removed in Step 4(c).

**I-11 (P2)** — `normalizeArabicText` calls `.lowercase()`, which is locale-sensitive on the JVM and
locale-invariant in Kotlin/Native. On Arabic input it is a no-op either way, so this is **not** a behaviour
risk — but it is dead work on the hot path. Do **not** remove it in this order; it is recorded so a later
cleanup does not mistake it for load-bearing.

---

## 8. OPERATIONAL BLOCKERS AFFECTING THIS ORDER

**B-10 (open)** — `device_bash` cannot mount the connected folder
(`no Plan9 drive shares mounted`), re-confirmed 2026-09-10. Claude Code cannot execute Gradle.
G2 therefore runs by Ibrahim pasting verbatim console output.

**B-13 (NEW, open)** — file staging is limited to **7 folders below the connected folder**.
`app/src/main/java/com/example/data/**` and `.../domain/**` sit at 8, so Claude Code cannot read the models,
repository, network layer, or local storage.
**Fix:** connect `F:\AI PROJECTS\Blind App\app\src\main\java\com\example` as an additional folder in the
Claude desktop app. Until then, orders touching those files rely on Antigravity/OpenCode reporting source
back, which is strictly weaker review and is why §6.3 demands full verbatim source.
