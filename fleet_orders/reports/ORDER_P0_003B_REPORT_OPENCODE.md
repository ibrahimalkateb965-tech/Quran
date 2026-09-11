ORDER-P0-003b REPORT — OpenCode CLI

1. BASELINE
- git HEAD before start: e1d82bf44413cc4f784d1913a597547b1d1c4e10
- Working tree before start (pre-existing, untouched by this order):
  M .agents/HOOKS_GUIDE.xlsx
  M fleet_orders/ORDER_P0_003B_DOMAIN_CORE.md
  ?? app/hs_err_pid24408.log
  ?? app/replay_pid24408.log
  ?? hs_err_pid13492.log
  ?? hs_err_pid21684.log
  ?? hs_err_pid22308.log
  ?? remote_ios_dev_playbook_diagram.html
- shared/src contained only network/ expect/actual scaffolding (ORDER-P0-002).
  No domain/, di/, or test files existed.

2. FILES CREATED (13, all new untracked; nothing else written in the repo)
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Surah.kt, 14 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Ayah.kt, 15 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/Reciter.kt, 38 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/SurahData.kt, 120 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/text/UthmanicText.kt, 14 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/audio/AyahAudioUrl.kt, 6 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt, 12 lines
- shared/src/commonMain/kotlin/com/aistudio/quranblind/di/SharedModule.kt, 15 lines
- shared/src/iosMain/kotlin/com/aistudio/quranblind/di/KoinIos.kt, 3 lines
- shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/text/UthmanicTextTest.kt, 89 lines
- shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/audio/AyahAudioUrlTest.kt, 33 lines
- shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/model/SurahDataTest.kt, 34 lines
- shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/FakeQuranRepository.kt, 21 lines

3. UthmanicText.kt FULL SOURCE
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
Body is a character-for-character copy of AyahCard.kt pass 2 (same pattern,
same replacement order). The Arabic inside BARE_NOON_NEXT_LETTERS and the
Regex literal is kept verbatim so the file diffs cleanly against AyahCard.kt.

4. INTERFACE DELTA (vs app com.example.domain.repository.QuranRepository)
- OMITTED allBookmarks: returns Flow<List<BookmarkEntity>>; BookmarkEntity is a Room type.
- OMITTED toggleBookmark(surahId, surahNameAr, ayahNumber): Room-bound via BookmarkDao.
- OMITTED isBookmarked(surahId, ayahNumber): Room-bound via BookmarkDao.
- OMITTED sanitizeUthmanicText(text): not a repository method in :shared; it is
  the pure top-level function in domain/text/UthmanicText.kt.
- CHANGED getAyahs(surahId, reciterIdentifier): reciterIdentifier has NO default
  in :shared. The Android default "ar.alafasy" is a dead alquran.cloud edition
  id no caller relies on; a wrong default is worse than none.
- KEPT getAllSurahs(), getSurahById(id), findSurahByName(query) with identical
  signatures (types re-pointed to com.aistudio.quranblind.domain.model).

5. T4 RESULT
Asserted code-point string (input == expected, sukoon kept):
"\u0645\u0650\u0646\u0652\u00A0\u0634\u064E\u0631\u0651\u0650"
Why: on the JVM \s does not match U+00A0 (no UNICODE_CHARACTER_CLASS), so the
(?=\s*...) lookahead fails between noon-sukoon and the following letter and the
sukoon survives. The test pins this behavior; a Kotlin/Native divergence on iOS
CI would surface here as a real platform difference.

6. PROBE OUTPUT (verbatim, captured via tail -25; only the two allowed probes
were run — no test task, no Ios/build task, no :app:assembleDebug per the
dispatch override)
--- ./gradlew :shared:compileAndroidMain ---
Calculating task graph as configuration cache cannot be reused because file 'gradle\libs.versions.toml' has changed.
> Task :shared:checkKotlinGradlePluginConfigurationErrors SKIPPED

> Task :shared:compileAndroidMain
w: file:///F:/AI%20PROJECTS/Blind%20App/shared/src/androidMain/kotlin/com/aistudio/quranblind/network/Logger.android.kt:5:1 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta. Consider using the '-Xexpect-actual-classes' flag to suppress this warning. Also see: https://youtrack.jetbrains.com/issue/KT-61573
w: file:///F:/AI%20PROJECTS/Blind%20App/shared/src/commonMain/kotlin/com/aistudio/quranblind/network/Logger.kt:3:1 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta. Consider using the '-Xexpect-actual-classes' flag to suppress this warning. Also see: https://youtrack.jetbrains.com/issue/KT-61573

[Incubating] Problems report is available at: file:///F:/AI%20PROJECTS/Blind%20App/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.4.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 10s
1 actionable task: 1 executed
Configuration cache entry stored.
--- ./gradlew :shared:compileAndroidHostTest ---
Calculating task graph as no cached configuration is available for tasks: :shared:compileAndroidHostTest
> Task :shared:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :shared:androidJar UP-TO-DATE
> Task :shared:androidPreBuild UP-TO-DATE
> Task :shared:preAndroidMainBuild UP-TO-DATE
> Task :shared:compileAndroidMain UP-TO-DATE
> Task :shared:bundleAndroidMainClassesToCompileJar
> Task :shared:compileAndroidHostTest

[Incubating] Problems report is available at: file:///F:/AI%20PROJECTS/Blind%20App/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.4.1/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 13s
4 actionable tasks: 2 executed, 2 up-to-date
Configuration cache entry stored.

7. GREP (A5: no android./java./org.junit/io.mockk under commonMain or commonTest)
Pattern `import (android\.|java\.|org\.junit|io\.mockk)` over shared/src:
- Single match: shared/src/androidMain/.../network/Logger.android.kt: `import android.util.Log`
  (pre-existing ORDER-P0-002 actual; androidMain is outside the A5 scope of
  commonMain/commonTest).
- Zero matches in shared/src/commonMain (all 8 new files clean).
- Zero matches in shared/src/commonTest (all 4 new test files use kotlin.test only).
- commonTest file shared/src/commonTest/.../text/UthmanicTextTest.kt is 100%
  ASCII (0 non-ASCII bytes); every fixture decoded from \uXXXX escapes was
  verified code-point-identical against app/src/test/.../UthmanicTextTest.kt.

8. git diff --stat -- app/ gradle/
(empty — zero lines of output; nothing under app/ or Gradle was modified.
The ?? app/hs_err_pid24408.log and ?? app/replay_pid24408.log entries are
pre-existing untracked crash logs from the baseline, not created by this order.)

9. DEVIATIONS
- T6 failure-message string is "Not idempotent" (short English) instead of
  interpolating the fixture; keeps the test file 100% ASCII per the escaped-
  fixture rule.
- FakeQuranRepository.findSurahByName matches on nameEnglish (case-insensitive)
  since Arabic normalization helpers are Android-side only; documented in code
  as a test double with no consumers yet.
- Helper scripts used to guarantee byte-identity (dump/convert/verify) live
  outside the repo in the pre-approved temp dir and were never added to git.
- :app:assembleDebug was NOT run (dispatch override limited probes to the two
  :shared: compile tasks above).
- Otherwise NONE: models/Reciter/SurahData copied verbatim (SurahData keeps the
  source's translationArabic "Ash-Shura" on id 42 as-is); @Serializable on
  Surah/Ayah only; audio builder, Koin module, and KoinIos verbatim per Steps
  6 and 9.

10. BLOCKED ON
- Nothing. All 13 files created, both compile probes green. Awaiting Claude
  Code CLI's own test execution (:shared:testDebugUnitTest) and Quality Gate.

11. SELF-ASSESSMENT
- A1 (:shared:compileAndroidMain + :shared:compileAndroidHostTest green): PASS
- A2 (:app:assembleDebug): NOT RUN (explicitly excluded by dispatch override)
- A3 (git diff --stat -- app/ gradle/ '*.gradle.kts' empty): PASS
- A4 (sanitize body matches AyahCard.kt:183-193): PASS (copied, not merged)
- A5 (no android./java./org.junit/io.mockk in commonMain/commonTest): PASS
- A6 (T1-T6 + audio + surah tests written, none executed): PASS
- A7 (interface deviations listed, nothing else omitted): PASS
- A8 (T4 expected value stated as code points): PASS

---

## QUALITY GATE — Claude Code CLI (Fleet Commander), 2026-09-11 12:50

**VERDICT: QUALITY GATE: PASS.**

| Check | Result |
|---|---|
| `:shared:testAndroidHostTest` (`--rerun-tasks`, 12:48:45) | **21/21 green** — UthmanicTextTest 8, SurahDataTest 5, AyahAudioUrlTest 3, HttpClientFactoryTest 5 |
| A4 byte-identity | `diff` of the 8 replacement lines against `AyahCard.kt:184-191` → identical (only `return`/brace differ: expression body) |
| A5 grep commonMain/commonTest | 0 matches |
| `git diff -- app/ gradle/ shared/build.gradle.kts` | empty |
| A2 `:app:assembleDebug` | not re-run: no `app/` or Gradle change; deferred to the P0-005 gate (B-17 memory budget) |

Interface delta accepted as specified (bookmarks omitted pending ADR-002; `sanitizeUthmanicText` is a pure function; no `reciterIdentifier` default). `sharedModule` is intentionally empty until ORDER-P0-004.
