# ORDER-P0-003b — Domain Core in `commonMain` (voice-free) + `sanitizeUthmanicText` port

| Field | Value |
|---|---|
| Order ID | ORDER-P0-003b (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — reassigned from Antigravity by Ibrahim 2026-09-11 (host memory, B-17) |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11 12:45** — ORDER-P0-002 passed its gate (Ktor 3.2.3, koin-core 4.1.0 are in `:shared`) |
| Supersedes | ORDER-P0-003 (cancelled: its payload was the deleted voice parser) |
| Depends on | ORDER-P0-002 (Step 1b/2b: `koin-core`, `kotlinx-coroutines-test` in `:shared`) |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

Package root for `:shared` is **`com.aistudio.quranblind`** (namespace `com.aistudio.quranblind.shared`).
The Android app's package is `com.example` and is **not** changed by this order.

Files you will port, all read in full:

| Android source | Portability | Notes |
|---|---|---|
| `app/src/main/java/com/example/data/model/Surah.kt` | ✅ pure Kotlin data class | 7 fields; `revelationType` is an Arabic string literal (`"مكية"`/`"مدنية"`) — keep it, it is data, not code |
| `app/src/main/java/com/example/data/model/Ayah.kt` | ✅ pure Kotlin data class | 8 fields, defaults on 5 |
| `app/src/main/java/com/example/data/model/Reciter.kt` | ✅ pure Kotlin | `DEFAULT_RECITERS` (20 entries, Arabic names are data) + `DEFAULT_RECITER = husary_mujawwad`. Audio base is **`https://verse.mp3quran.net/data/`** — NOT everyayah.com; older orders were wrong about this |
| `app/src/main/java/com/example/data/model/SurahData.kt` | ✅ pure Kotlin object | 114 `Surah(...)` literals, no `android.*`/`java.*` imports |
| `app/src/main/java/com/example/domain/repository/QuranRepository.kt` | ⚠️ partially | 8 members; two bookmark members reference `BookmarkEntity` (**Room**) — see Step 3 |
| `app/src/main/java/com/example/data/repository/QuranRepository.kt:157` | ✅ pure Kotlin (Regex + String) | `sanitizeUthmanicText` **pass 1** (repository) |
| `app/src/main/java/com/example/ui/components/player/AyahCard.kt:180-194` | ✅ pure Kotlin | `sanitizeUthmanicText` **pass 2** (private, UI) — a **superset** of pass 1 |
| `app/src/main/java/com/example/data/repository/QuranRepository.kt:167-176` | ✅ pure Kotlin | `resolveAudioEndpoint` — the `{SSS}{AAA}.mp3` builder |
| `app/src/test/java/com/example/UthmanicTextTest.kt` | JUnit4 | 5 tests; the fixtures are the byte-identity oracle for Step 5 |

**The two `sanitizeUthmanicText` copies differ.** Production output is `pass2(pass1(text))`. Claude Code
verified that pass 2 is a superset whose extra replacements (`\u06DF→\u06E0`, strip `\u0600`, strip `\u06DD`)
commute with pass 1, so **`pass2(text) == pass2(pass1(text))` for every input**. The canonical shared
function is therefore **pass 2, exactly as written in `AyahCard.kt`**. Do not "merge" them by hand; copy pass 2.

---

## 1. OBJECTIVE

`shared/src/commonMain` contains the domain models, the static surah table, the reciter table, the
canonical `sanitizeUthmanicText`, the audio-URL builder, and a `QuranRepository` interface with **no**
Android/Room types — with `commonTest` tests proving byte-identity of the Uthmanic port. `:app` is
untouched and still compiles.

**Explicit non-goals:** changing any file under `app/`; wiring `:app` to `:shared` (a later order);
removing Hilt; adding a database (ADR-002 is open); bookmarks (Room-bound, see Step 3).

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Surah.kt              CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Ayah.kt               CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Reciter.kt            CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/SurahData.kt          CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/text/UthmanicText.kt        CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/audio/AyahAudioUrl.kt       CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/di/SharedModule.kt                 CREATE
shared/src/iosMain/kotlin/com/aistudio/quranblind/di/KoinIos.kt                         CREATE
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/text/UthmanicTextTest.kt    CREATE
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/audio/AyahAudioUrlTest.kt   CREATE
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/model/SurahDataTest.kt      CREATE
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/FakeQuranRepository.kt      CREATE
```

## 3. FILES YOU MUST NOT TOUCH

- `app/**` — not one byte. `git diff --stat -- app/` must be empty at report time.
- `shared/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `settings.gradle.kts`,
  `build.gradle.kts` — Gradle is OpenCode's. If a dependency you need is missing, **STOP and report**.
- `shared/src/commonMain/kotlin/com/aistudio/quranblind/network/**` — OpenCode's (ORDER-P0-002).
- `.github/workflows/**`, `CLAUDE.md`, `fleet_config.json`.

---

## 4. STEPS

### Step 1 — Models

Copy `Surah`, `Ayah`, `Reciter` to `domain/model/` under package `com.aistudio.quranblind.domain.model`.
Field names, order, types and defaults **identical** to the Android originals. Add `@Serializable`
(`kotlinx.serialization`) to `Surah` and `Ayah` only; `Reciter` stays a plain data class (it is a static
table, never parsed). No `@SerialName` renames — the Android models carry no Moshi annotations.

### Step 2 — Static tables

- `SurahData` → `domain/model/SurahData.kt`, same package. Copy the 114 literals **verbatim**.
- `Reciter.DEFAULT_RECITERS` / `DEFAULT_RECITER` come along inside `Reciter.kt`'s companion, verbatim,
  including the `BASE_URL = "https://verse.mp3quran.net/data/"` constant.

### Step 3 — `QuranRepository` interface (`domain/repository/`)

```kotlin
package com.aistudio.quranblind.domain.repository

import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getAllSurahs(): List<Surah>
    fun getSurahById(id: Int): Surah?
    fun findSurahByName(query: String): Surah?
    fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>>
}
```

Deliberate differences from the Android interface — **report each one, do not silently deviate further**:
- `allBookmarks`, `toggleBookmark`, `isBookmarked` are **omitted**: they return/accept `BookmarkEntity`,
  a Room type. Bookmarks return in a later order once ADR-002 closes.
- `sanitizeUthmanicText` is **not** a repository method in `:shared`; it is the pure function of Step 4.
- `getAyahs` has **no default** for `reciterIdentifier`. The Android default `"ar.alafasy"` is a dead
  alquran.cloud edition id that no caller relies on; a default that is wrong is worse than none.

### Step 4 — `sanitizeUthmanicText` (`domain/text/UthmanicText.kt`)

Top-level, pure, public (Swift needs it exported):

```kotlin
package com.aistudio.quranblind.domain.text

private const val BARE_NOON_NEXT_LETTERS = "[يرملونصذثكجشقسدطزفتضظب]"
private val noonSukoonPattern = Regex("(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*$BARE_NOON_NEXT_LETTERS)")

fun sanitizeUthmanicText(text: String): String =
    text.replace(noonSukoonPattern, "$1")
        .replace('\u06DF', '\u06E0')
        .replace('\u06E4', '\u0653')
        .replace("\u0600", "")
        .replace("\u06DD", "")
        .replace("\uFEFF", "")
        .replace("\u200A", "")
        .replace("\u2060", "")
```

This is `AyahCard.kt` pass 2 character for character. Keep the replacement order exactly so a reviewer can
diff it against `AyahCard.kt` line by line.

**Kotlin/Native regex caveat:** `Regex` with a lookahead `(?=…)` and Arabic literals inside a character
class is supported by Kotlin/Native's regex engine, but `\\s` semantics for U+00A0 differ from the JVM.
Add test T4 below to pin it.

### Step 5 — `commonTest` — Uthmanic byte-identity (`UthmanicTextTest.kt`)

`kotlin.test` only — **no JUnit, no MockK** (neither exists on iOS). Port every fixture from
`app/src/test/java/com/example/UthmanicTextTest.kt` and assert against the **shared function**, not
against inline `replace` chains as the Android test does:

| # | Input | Expected | Source |
|---|---|---|---|
| T1 | `"\u0621\u064Eامَنُو\u0653ا\u06DF\u0600 إِذَا"` | `"\u0621\u064Eامَنُو\u0653ا\u06E0 إِذَا"` | small rounded zero + rosette |
| T2 | `"مِنْ شَرِّ مَا خَلَقَ"` | `"مِن شَرِّ مَا خَلَقَ"` | ikhfa |
| T2b | `"مَنْ يَقُولُ"` | `"مَن يَقُولُ"` | idgham |
| T2c | `"أَنْعَمْتَ عَلَيْهِمْ"` | unchanged | izhar — sukoon must survive |
| T3 | `"وَلَا ٱلضَّا\u06E4لِّینَ"` | `"وَلَا ٱلضَّآلِّینَ"` | small madda → maddah above |
| T4 | `"مِنْ\u00A0شَرِّ"` | **unchanged** (sukoon kept) | NBSP between noon and next letter. On the JVM `\\s` does not match U+00A0 (no `UNICODE_CHARACTER_CLASS`), so the lookahead fails and the sukoon survives. Assert `unchanged`; if Claude Code's iOS CI run disagrees, that is a real K/N divergence and will be handled then |
| T5 | `"بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"` | unchanged, 4 words after split | no false positives on Basmala |
| T6 | idempotence: `f(f(x)) == f(x)` for every fixture above | | |

Write the expected strings as **escaped code points** (`"\u0621\u064E…"`), not as visible Arabic — a
text editor's bidi reordering or a font's glyph substitution must not be able to alter a fixture silently.

### Step 6 — Audio URL builder (`domain/audio/AyahAudioUrl.kt`) + test

```kotlin
package com.aistudio.quranblind.domain.audio

fun ayahAudioUrl(audioBaseUrl: String, surahId: Int, ayahInSurah: Int): String {
    val base = if (audioBaseUrl.endsWith("/")) audioBaseUrl else "$audioBaseUrl/"
    return base + surahId.toString().padStart(3, '0') + ayahInSurah.toString().padStart(3, '0') + ".mp3"
}
```
Verbatim port of `resolveAudioEndpoint`. Tests: `(Husary_128kbps base, 2, 5)` →
`https://verse.mp3quran.net/data/Husary_128kbps/002005.mp3`; base without trailing slash gets one;
`(114, 6)` → `114006.mp3`.

### Step 7 — `SurahDataTest.kt`

`SURAH_LIST.size == 114`; ids are `1..114` in order; `first().nameEnglish == "Al-Fatihah"`,
`ayahCount == 7`; `last().id == 114`; `sumOf { ayahCount } == 6236`.

### Step 8 — `FakeQuranRepository.kt` (`commonTest`)

Implements the Step 3 interface over `SurahData.SURAH_LIST` with `getAyahs` returning `flowOf(emptyList())`.
Exists so later orders have a test double; no test uses it yet.

### Step 9 — `SharedModule.kt` (Koin)

```kotlin
package com.aistudio.quranblind.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val sharedModule = module {
    // Intentionally empty until the first shared implementation exists (ORDER-P0-004).
    // QuranRepository is NOT bound here: its implementation is still Android/Room-bound.
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModule)
}
```
Plus in `iosMain` (`di/KoinIos.kt`): `fun initKoinIos() = initKoin()` — a zero-argument entry point
Swift can call. That is the **only** file you create outside `commonMain`/`commonTest`.

### Step 10 — Compile probes (compile only)

```bash
./gradlew :shared:compileAndroidMain
./gradlew :shared:compileAndroidHostTest
./gradlew :app:assembleDebug
```
Nothing else. No `build`, no `test`, no `allTests`, no `*Ios*` task (Kotlin/Native cannot compile iOS
targets on Windows — CI does that).

---

## 5. ACCEPTANCE CRITERIA (checked by Claude Code CLI)

| # | Criterion | Evidence |
|---|---|---|
| A1 | `:shared:compileAndroidMain` and `:shared:compileAndroidHostTest` → BUILD SUCCESSFUL | verbatim |
| A2 | `:app:assembleDebug` → BUILD SUCCESSFUL | verbatim |
| A3 | `git diff --stat -- app/ gradle/ '*.gradle.kts'` is empty | output |
| A4 | `sanitizeUthmanicText` body is a character-for-character match of `AyahCard.kt:183-193` | Claude Code diffs it |
| A5 | No `android.`, `java.`, `org.junit`, `io.mockk` under `shared/src/commonMain` or `commonTest` | grep |
| A6 | Tests T1–T6 + audio + surah tests written; **none executed by you** | file content |
| A7 | Interface deviations from Step 3 are listed in the report, nothing else omitted | report |
| A8 | T4's expected value is stated in the report as a code-point string | report |

Claude Code runs `:shared:testDebugUnitTest` (Android host) itself. QUALITY GATE requires **all** tests
green **and** A4 exact.

---

## 6. REPORT-BACK

Write to `fleet_orders/reports/ORDER_P0_003B_REPORT_OPENCODE.md`:

```
ORDER-P0-003b REPORT — OpenCode CLI

1. BASELINE            git HEAD, working tree state before you started
2. FILES CREATED       path, line count (13 expected)
3. UthmanicText.kt     FULL SOURCE
4. INTERFACE DELTA     each omitted/changed member of QuranRepository, one line each
5. T4 RESULT           the exact code-point string you asserted, and why
6. PROBE OUTPUT        verbatim, both commands
7. GREP                output proving A5
8. git diff --stat -- app/ gradle/      (must be empty)
9. DEVIATIONS          "NONE" only if literally nothing
10. BLOCKED ON
11. SELF-ASSESSMENT    A1–A8 PASS/FAIL
```

FORBIDDEN (repeated on purpose): running any test; editing Gradle; touching `app/`; git commit/push;
Arabic in code comments, identifiers, commit messages or terminal output (Arabic **string literals that
are data** — surah names, reciter names, test fixtures — are required and are not a violation).
STOP after reporting. Do not begin ORDER-P0-005.
