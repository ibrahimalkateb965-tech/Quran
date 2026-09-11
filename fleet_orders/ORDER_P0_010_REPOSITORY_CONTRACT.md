# ORDER-P0-010 — Make `:app`'s `QuranRepository` extend the shared `domain.repository.QuranRepository`

| Field | Value |
|---|---|
| Order ID | ORDER-P0-010 (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — one non-composable file, see §2 |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11** |
| Depends on | ORDER-P0-009 (PASS — `:app` now uses `com.aistudio.quranblind.domain.model.{Ayah,Surah}`, so the two interfaces' signatures are type-identical) |
| Governing rules | CLAUDE.md §2 (no tests, no Gradle, no `@Composable` files); §3 (Hilt stays Android-only — the Hilt binding is untouched) |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

### 0.1 The two interfaces

`shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt` (frozen):

```kotlin
interface QuranRepository {
    fun getAllSurahs(): List<Surah>
    fun getSurahById(id: Int): Surah?
    fun findSurahByName(query: String): Surah?
    fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>>
}
```

`app/src/main/java/com/example/domain/repository/QuranRepository.kt` (16 lines, the file you edit):

```kotlin
package com.example.domain.repository

import com.example.data.local.BookmarkEntity
import com.aistudio.quranblind.domain.model.Ayah
import com.aistudio.quranblind.domain.model.Surah
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    val allBookmarks: Flow<List<BookmarkEntity>>
    suspend fun toggleBookmark(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean
    suspend fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean
    fun getAllSurahs(): List<Surah>
    fun getSurahById(id: Int): Surah?
    fun findSurahByName(query: String): Surah?
    fun getAyahs(surahId: Int, reciterIdentifier: String = "ar.alafasy"): Flow<List<Ayah>>
}
```

The four non-bookmark members are signature-identical to the shared interface since P0-009 (same `Ayah`,
`Surah`, `Flow` types). The only difference is the default `reciterIdentifier = "ar.alafasy"`, which is
**dead**: the single caller, `ui/viewmodel/QuranViewModel.kt:291`, passes both arguments
(`repository.getAyahs(surahId, _settingsUiState.value.selectedReciter.serverIdentifier)`). Kotlin forbids
default values on overriding members, so the default must go — nothing observes it.

### 0.2 Consumers of `com.example.domain.repository.QuranRepository` (all unchanged by this order)

| File | Role | Impact |
|---|---|---|
| `data/repository/QuranRepository.kt` (`QuranRepositoryImpl : QuranRepository`) | implements all 7 members with `override`, `getAyahs(surahId: Int, reciterIdentifier: String)` with no default | none — the four members now satisfy the inherited shared declarations |
| `di/AppModule.kt:68-70` (`@Binds bindQuranRepository(impl: QuranRepositoryImpl): QuranRepository`) | Hilt binding to the `:app` interface | none |
| `ui/viewmodel/QuranViewModel.kt:76` (`private val repository: QuranRepository`) | 7 call sites (lines 93, 111, 209, 269, 291, 293, 332, 540) | none — all members still resolve |
| `app/src/test/java/com/example/data/repository/QuranRepositoryTest.kt` | constructs `QuranRepositoryImpl` directly | none |

`shared/src/commonTest/.../FakeQuranRepository.kt` implements the shared interface — untouched.

---

## 1. OBJECTIVE

Turn the `:app` interface into a thin Android extension of the shared contract: it **inherits** the four
read members from `com.aistudio.quranblind.domain.repository.QuranRepository` and declares only the three
Room-bookmark members. After this order there is exactly one declaration of `getAllSurahs`, `getSurahById`,
`findSurahByName` and `getAyahs` in the repository, and it lives in `:shared`.

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
app/src/main/java/com/example/domain/repository/QuranRepository.kt     MODIFY — replace whole file with §4 content
```

## 3. FILES YOU MUST NOT TOUCH

Every other file — in particular `data/repository/QuranRepository.kt` (`QuranRepositoryImpl`),
`di/AppModule.kt`, `ui/**`, `shared/**`, `app/src/test/**`, `app/build.gradle.kts`,
`gradle/libs.versions.toml`, `proguard-rules.pro`, `.github/**`, `CLAUDE.md`, `fleet_config.json`.
Do not delete any `hs_err_pid*.log` / `replay_pid*.log`.

---

## 4. STEPS

### Step 1 — Rewrite `domain/repository/QuranRepository.kt` so that its entire content is exactly:

```kotlin
package com.example.domain.repository

import com.aistudio.quranblind.domain.repository.QuranRepository as SharedQuranRepository
import com.example.data.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface QuranRepository : SharedQuranRepository {
    val allBookmarks: Flow<List<BookmarkEntity>>
    suspend fun toggleBookmark(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean
    suspend fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean
}
```

(11 lines, LF line endings as the file has today, single trailing newline.) The import alias is required
because both interfaces are named `QuranRepository`.

### Step 2 — Verify by grep only (NO Gradle, NO compile probes)

```bash
grep -rn "fun getAllSurahs\|fun getSurahById\|fun findSurahByName\|fun getAyahs" app/src shared/src
```

must show exactly **8 lines**: 4 in `shared/.../domain/repository/QuranRepository.kt` (declarations), and 4
`override fun` lines in `app/.../data/repository/QuranRepository.kt` (`QuranRepositoryImpl`). None in
`app/.../domain/repository/QuranRepository.kt`.

```bash
grep -rn "ar.alafasy" app/src shared/src
```

must return nothing.

Do **not** run `./gradlew` for any reason (the Commander compiles at the gate).

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | `git status --short` beyond the pre-existing baseline shows exactly one ` M`: the §2 file |
| A2 | `git diff --numstat` for it is **+2 / −7** (added: alias import, new `interface` line; removed: `Ayah`/`Surah` imports, old `interface` line, 4 member lines) |
| A3 | Step 2 grep #1 → exactly 8 lines as described; grep #2 → empty |
| A4 | `QuranRepositoryImpl`, `AppModule.kt`, `QuranViewModel.kt`, `shared/**`, tests untouched (`git diff --stat` on them empty) |
| A5 | No Gradle invocation, no test executed by you; no commit, no push |

## 6. REPORT-BACK

`fleet_orders/reports/ORDER_P0_010_REPORT_OPENCODE.md`, in **English**, sections: BASELINE (`git status
--short` before you start), FULL `git diff` of the file (verbatim), `git status --short` AFTER, STEP 2 GREP
OUTPUT verbatim (both greps), DEVIATIONS, BLOCKED ON, SELF-ASSESSMENT A1–A5. Your final chat summary must
also be in English (CLAUDE.md §1: everything except replies to the maintainer is English).

FORBIDDEN: any `./gradlew` invocation; running any test; touching any file outside §2; deleting log
files; git commit/push. STOP after reporting.
