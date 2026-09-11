# ORDER-P0-008 — Cut `:app` over to the shared `domain.text.sanitizeUthmanicText`

| Field | Value |
|---|---|
| Order ID | ORDER-P0-008 (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — data layer only, see §2; B-17 stands |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11** |
| Depends on | ORDER-P0-003b (PASS — `com.aistudio.quranblind.domain.text.sanitizeUthmanicText` is the canonical pass-2 function, `UthmanicTextTest` 5/5 in `:shared`) |
| Governing rule | CLAUDE.md §4.4 — Uthmanic text integrity is byte-for-byte; any diacritic regression is a P0 defect |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

Three copies of the sanitizer exist today:

| Copy | Location | Body |
|---|---|---|
| **Canonical (pass 2)** | `shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/text/UthmanicText.kt:6-14` | noon-sukoon regex, `06DF→06E0`, `06E4→0653`, strip `0600`, `06DD`, `FEFF`, `200A`, `2060` |
| UI copy (pass 2, identical) | `app/src/main/java/com/example/ui/components/player/AyahCard.kt:180-192` — `private fun`, consumed once at line 76 | same 8 steps, character-for-character (verified in P0-003b A4) |
| Repository copy (**pass 1**, subset) | `app/src/main/java/com/example/data/repository/QuranRepository.kt:157-165` — `override fun` of `com.example.domain.repository.QuranRepository:16` | noon-sukoon regex, `06E4→0653`, strip `FEFF`, `200A`, `2060` (no `06DF→06E0`, no `0600`/`06DD` strip) |

Consumers of the repository copy: internal lines 97 and 128 of `QuranRepositoryImpl`, and
`app/src/test/java/com/example/data/repository/QuranRepositoryTest.kt:63`
(`repository.sanitizeUthmanicText(...)`). **Nothing else** in `app/src` calls the interface method — the
ViewModel never does. `app/src/test/java/com/example/UthmanicTextTest.kt` (3 tests) re-implements the
transformation inline (B-12) and does not call any production function; it is unaffected.

`Ayah.textArabic` is read in exactly one place: `AyahCard.kt:75-76`. So the text that reaches the screen is
today `pass2(pass1(json))` and after this order `pass2(pass2(json))`. `pass2` is idempotent
(`UthmanicTextTest` "idempotent" case) and pass 1 is a step-subset of pass 2 applied in the same order,
so the rendered bytes are unchanged. The Room cache (`AyahEntity.textArabic`) will hold pass-2 output for
newly cached surahs instead of pass-1 output; rows cached earlier still pass through `AyahCard`'s pass 2
on read. No migration.

`:app` already depends on `:shared` (`app/build.gradle.kts:90`, `implementation(project(":shared"))`), so
the top-level function is on both the main and the unit-test classpath — P0-007 already imports
`com.aistudio.quranblind.store.*` from `:app`.

---

## 1. OBJECTIVE

Make `com.aistudio.quranblind.domain.text.sanitizeUthmanicText` the **only** sanitizer in the codebase:
delete the repository copy and its interface method, point the two internal call sites and the one test at
the shared function. The UI copy in `AyahCard.kt` is removed by Claude Code (§2) in the same commit.

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
app/src/main/java/com/example/domain/repository/QuranRepository.kt         MODIFY — Step 1 (one line removed)
app/src/main/java/com/example/data/repository/QuranRepository.kt           MODIFY — Step 2 (import + method removal)
app/src/test/java/com/example/data/repository/QuranRepositoryTest.kt       MODIFY — Step 3 (import + one call site)
```

**Reserved to Claude Code CLI — do NOT open or edit:**
`app/src/main/java/com/example/ui/components/player/AyahCard.kt` is a `@Composable` file (CLAUDE.md §2:
OpenCode never touches UI composables). The Commander removes its private copy at the gate.

## 3. FILES YOU MUST NOT TOUCH

Every other file. In particular: anything under `ui/**`, `accessibility/**`, `service/**`; `res/**`;
`shared/**` (the canonical function is frozen); `app/src/test/java/com/example/UthmanicTextTest.kt`;
`app/build.gradle.kts`; `gradle/libs.versions.toml`; `proguard-rules.pro`; `.github/**`; `CLAUDE.md`;
`fleet_config.json`. Do not delete any `hs_err_pid*.log` / `replay_pid*.log`.

---

## 4. STEPS

### Step 1 — `domain/repository/QuranRepository.kt` (interface)

Remove the single line

```kotlin
    fun sanitizeUthmanicText(text: String): String
```

Nothing else changes — imports, the other six members, `getAyahs` default parameter all stay.

### Step 2 — `data/repository/QuranRepository.kt` (`QuranRepositoryImpl`)

1. Add the import `import com.aistudio.quranblind.domain.text.sanitizeUthmanicText` directly **after**
   `import android.content.Context` (line 3) — the file's imports are alphabetical and `com.aistudio`
   sorts before `com.example`.
2. Delete the whole method at lines 157–165:

   ```kotlin
       override fun sanitizeUthmanicText(text: String): String {
           val bareNoonNextLetters = "[يرملونصذثكجشقسدطزفتضظب]"
           val pattern = Regex("(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*$bareNoonNextLetters)")
           return text.replace(pattern, "$1")
               .replace('ۤ', 'ٓ')
               .replace("﻿", "")
               .replace(" ", "")
               .replace("⁠", "")
       }
   ```

   together with the blank line that follows it (line 166), so `resolveAudioEndpoint` keeps exactly one
   blank line above it.
3. Lines 97 and 128 (`sanitizeUthmanicText(it.textArabic)` / `sanitizeUthmanicText(item.getString("textArabic"))`)
   are **not edited** — they now resolve to the imported top-level function.

### Step 3 — `QuranRepositoryTest.kt`

1. Add `import com.aistudio.quranblind.domain.text.sanitizeUthmanicText` directly after
   `import android.content.Context` (line 3).
2. Line 63: `val sanitized = repository.sanitizeUthmanicText(textWithSpecialChars)` →
   `val sanitized = sanitizeUthmanicText(textWithSpecialChars)`.
3. Keep the test name, the fixture string and the three assertions exactly as they are. The test still
   proves that `:app`'s unit-test classpath resolves the shared function.

### Step 4 — Compile probes (compile only)

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:compileDebugUnitTestKotlin
```

Both must be green on your tree alone — `AyahCard.kt` keeps its own private copy until the Commander
removes it, so nothing in `ui/` depends on your edits. If a probe times out on a cold Gradle start (seen in
P0-007), rerun it once. Do NOT run `testDebugUnitTest`, `assembleDebug` or `assembleRelease` (B-17: one
JVM at a time; Claude Code runs them at the gate).

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | Both compile probes BUILD SUCCESSFUL |
| A2 | `git status --short` beyond the pre-existing baseline shows exactly the three `M` files of §2 |
| A3 | `git diff --numstat` is: interface **+0 / −1**; `QuranRepositoryImpl` **+1 / −10**; `QuranRepositoryTest` **+2 / −1** |
| A4 | `grep -rn "override fun sanitizeUthmanicText\|repository.sanitizeUthmanicText" app/src` returns nothing |
| A5 | `grep -rn "sanitizeUthmanicText" app/src/main/java/com/example/data app/src/main/java/com/example/domain` shows exactly three hits, all in `QuranRepositoryImpl`: the import line and the two call sites (~lines 98 and 129) |
| A6 | `AyahCard.kt`, `shared/**`, `UthmanicTextTest.kt` (both modules), `build.gradle.kts`, `proguard-rules.pro` untouched (`git diff --stat` on them empty) |
| A7 | No test executed by you; no commit, no push |

## 6. REPORT-BACK

`fleet_orders/reports/ORDER_P0_008_REPORT_OPENCODE.md`, sections: BASELINE (`git status --short` before
you start), FULL `git diff` of the three files (verbatim), PROBE OUTPUT (last 20 lines each), A4/A5 GREP
OUTPUT verbatim, DEVIATIONS, BLOCKED ON, SELF-ASSESSMENT A1–A7.

FORBIDDEN: running any test; `assembleDebug`/`assembleRelease`; opening `AyahCard.kt`; touching any file
outside §2; deleting log files; git commit/push; Arabic outside data string literals (the Arabic character
class in the regex you delete is data — just delete it, do not retype it anywhere). STOP after reporting.
