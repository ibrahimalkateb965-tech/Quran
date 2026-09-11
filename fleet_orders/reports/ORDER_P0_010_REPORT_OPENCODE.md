# ORDER-P0-010 Report — OpenCode CLI

| Field | Value |
|---|---|
| Order ID | ORDER-P0-010 (rev. A, 2026-09-11) |
| Executed by | OpenCode CLI (`opencode/muse-spark-1.3-contributor-free`) |
| Date (UTC) | 2026-09-11 |
| Scope | Steps 1–2 only. No Gradle, no tests, no commit/push. |

## BASELINE (`git status --short` before start)

```
M .agents/ACTIVE_CONTEXT_INJECTION.md
M .agents/HOOKS_GUIDE.xlsx
M .agents/MEMORY_STORE.md
M CLAUDE.md
M fleet_config.json
M opencode.json
?? app/hs_err_pid24408.log
?? app/replay_pid24408.log
?? fleet_orders/ORDER_P0_010_REPOSITORY_CONTRACT.md
?? hs_err_pid13492.log
?? hs_err_pid18096.log
?? hs_err_pid19008.log
?? hs_err_pid21684.log
?? hs_err_pid22308.log
?? remote_ios_dev_playbook_diagram.html
?? replay_pid18096.log
```

All of the above are pre-existing; none were created or modified by this order.

## STEP 1 — FILE REWRITE

`app/src/main/java/com/example/domain/repository/QuranRepository.kt` was replaced in full
with the exact 11-line content from §4 (LF line endings, single trailing newline).
Verified: `file` = `ASCII text` (no CRLF), `wc -l` = 11, zero CR characters.

## FULL `git diff` OF THE FILE (verbatim)

```diff
diff --git a/app/src/main/java/com/example/domain/repository/QuranRepository.kt b/app/src/main/java/com/example/domain/repository/QuranRepository.kt
index 58b3678..1537129 100644
--- a/app/src/main/java/com/example/domain/repository/QuranRepository.kt
+++ b/app/src/main/java/com/example/domain/repository/QuranRepository.kt
@@ -1,16 +1,11 @@
 package com.example.domain.repository

+import com.aistudio.quranblind.domain.repository.QuranRepository as SharedQuranRepository
 import com.example.data.local.BookmarkEntity
-import com.aistudio.quranblind.domain.model.Ayah
-import com.aistudio.quranblind.domain.model.Surah
 import kotlinx.coroutines.flow.Flow

-interface QuranRepository {
+interface QuranRepository : SharedQuranRepository {
     val allBookmarks: Flow<List<BookmarkEntity>>
     suspend fun toggleBookmark(surahId: Int, surahNameAr: String, ayahNumber: Int): Boolean
     suspend fun isBookmarked(surahId: Int, ayahNumber: Int): Boolean
-    fun getAllSurahs(): List<Surah>
-    fun getSurahById(id: Int): Surah?
-    fun findSurahByName(query: String): Surah?
-    fun getAyahs(surahId: Int, reciterIdentifier: String = "ar.alafasy"): Flow<List<Ayah>>
 }
```

`git diff --numstat` for the file: `2 + / 7 -` (matches A2).

## `git status --short` AFTER

```
M .agents/ACTIVE_CONTEXT_INJECTION.md
M .agents/HOOKS_GUIDE.xlsx
M .agents/MEMORY_STORE.md
M CLAUDE.md
M app/src/main/java/com/example/domain/repository/QuranRepository.kt
M fleet_config.json
M opencode.json
?? app/hs_err_pid24408.log
?? app/replay_pid24408.log
?? fleet_orders/ORDER_P0_010_REPOSITORY_CONTRACT.md
?? hs_err_pid13492.log
?? hs_err_pid18096.log
?? hs_err_pid19008.log
?? hs_err_pid21684.log
?? hs_err_pid22308.log
?? remote_ios_dev_playbook_diagram.html
?? replay_pid18096.log
```

Delta vs BASELINE is exactly one line: `M app/src/main/java/com/example/domain/repository/QuranRepository.kt`.

## STEP 2 GREP OUTPUT (verbatim, native `grep -rn`, exit codes included)

### Grep #1: `grep -rn "fun getAllSurahs\|fun getSurahById\|fun findSurahByName\|fun getAyahs" app/src shared/src` (EXIT:0)

```
app/src/main/java/com/example/data/local/AyahDao.kt:11:    fun getAyahsForSurah(surahId: Int, reciterIdentifier: String): List<AyahEntity>
app/src/main/java/com/example/data/repository/QuranRepository.kt:55:    override fun getAllSurahs(): List<Surah> {
app/src/main/java/com/example/data/repository/QuranRepository.kt:59:    override fun getSurahById(id: Int): Surah? {
app/src/main/java/com/example/data/repository/QuranRepository.kt:63:    override fun findSurahByName(query: String): Surah? {
app/src/main/java/com/example/data/repository/QuranRepository.kt:80:    override fun getAyahs(
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt:8:    fun getAllSurahs(): List<Surah>
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt:9:    fun getSurahById(id: Int): Surah?
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt:10:    fun findSurahByName(query: String): Surah?
shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/repository/QuranRepository.kt:11:    fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>>
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/FakeQuranRepository.kt:12:    override fun getAllSurahs(): List<Surah> = SurahData.SURAH_LIST
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/FakeQuranRepository.kt:14:    override fun getSurahById(id: Int): Surah? = SurahData.SURAH_LIST.find { it.id == id }
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/FakeQuranRepository.kt:16:    override fun findSurahByName(query: String): Surah? =
shared/src/commonTest/kotlin/com/aistudio/quranblind/domain/FakeQuranRepository.kt:19:    override fun getAyahs(surahId: Int, reciterIdentifier: String): Flow<List<Ayah>> =
```

13 lines total, NOT 8. Analysis (see DEVIATIONS):
- 4 declarations in `shared/.../domain/repository/QuranRepository.kt` — as expected.
- 4 `override fun` in `app/.../data/repository/QuranRepository.kt` — as expected.
- **Zero** hits in the edited `app/.../domain/repository/QuranRepository.kt` — as expected.
- 4 extra hits in `shared/.../FakeQuranRepository.kt` (implements the shared interface; untouched).
- 1 extra hit in `app/.../data/local/AyahDao.kt:11` (`fun getAyahsForSurah` — substring match of the `fun getAyahs` prefix; unrelated pre-existing DAO method).

### Grep #2: `grep -rn "ar.alafasy" app/src shared/src` (EXIT:1, no output)

```
(empty — no matches)
```

As required: the dead default value is gone from the codebase.

## DEVIATIONS

1. **Step 2 grep #1 line count (13 vs 8 stated in A3).** The order's "exactly 8 lines"
   expectation overlooked two pre-existing sources that the same BRE pattern matches:
   (a) `shared/src/commonTest/.../FakeQuranRepository.kt` (4 override lines — the fake
   named in §0.2 as implementing the shared interface, untouched per §3), and
   (b) `app/src/main/java/com/example/data/local/AyahDao.kt:11`
   (`fun getAyahsForSurah...` — prefix-substring match, unrelated DAO, pre-existing).
   The load-bearing assertions all hold: 4 shared declarations + 4 impl overrides +
   0 in the edited file. No file was touched to force the count to 8; the extra hits
   are outside this order's scope (§3 forbids touching them).
2. **None otherwise.** File content is byte-exact per §4; no other file modified;
   no Gradle/test invocation; no commit/push; no log files deleted.

## BLOCKED ON

Nothing. Steps 1–2 complete. Awaiting Commander's Quality Gate (compile + tests are
Commander's monopoly per CLAUDE.md §2).

## SELF-ASSESSMENT A1–A5

| # | Criterion | Verdict |
|---|---|---|
| A1 | `git status --short` delta vs baseline is exactly one `M`: the §2 file | **PASS** — delta is the single `M app/src/main/java/com/example/domain/repository/QuranRepository.kt` line (all other entries identical to BASELINE). |
| A2 | `git diff --numstat` for it is +2 / −7 | **PASS** — verbatim `2 7 app/src/main/java/com/example/domain/repository/QuranRepository.kt`. |
| A3 | Step 2 grep #1 → exactly 8 lines; grep #2 → empty | **PARTIAL** — grep #2 empty: PASS. Grep #1: core assertions PASS (4 shared + 4 impl overrides + 0 in edited file), but raw count is 13, not 8, due to 5 pre-existing out-of-scope hits documented above (FakeQuranRepository ×4, AyahDao substring ×1). |
| A4 | Impl, AppModule, QuranViewModel, shared, tests untouched | **PASS** — `git diff --stat` on `data/repository/QuranRepository.kt`, `di/AppModule.kt`, `ui/viewmodel/QuranViewModel.kt`, `shared/`, `app/src/test/` is empty. |
| A5 | No Gradle, no test, no commit/push | **PASS** — only `grep`, `git status/diff`, `file`, `wc -l` (read-only) were run; no `./gradlew`, no tests, no commit/push. |

---

## COMMANDER VERIFICATION + QUALITY GATE — Claude Code CLI, 2026-09-11 23:55 – 2026-09-12 00:05

### Verification

- File content is byte-identical to §4 of the order (11 lines, ASCII, LF, single trailing newline);
  `git diff --numstat` = **+2 / −7**; nothing else in `app/`, `shared/`, or `fleet_orders/` beyond the order and
  this report changed.
- **A3 correction (Commander's error, not OpenCode's):** the order said grep #1 should return 8 lines. It
  returns **13**: the expected 4 shared declarations + 4 `QuranRepositoryImpl` overrides, plus 4
  `FakeQuranRepository` overrides (`shared/commonTest`) and `AyahDao.getAyahsForSurah` — all pre-existing and
  out of scope. The assertion that matters — **zero** declarations left in the `:app` interface — holds.
  OpenCode reported the variance instead of forcing the count; correct behaviour.

### Gate (foreground, one Gradle invocation per call, B-17)

| Step | Command | Result | Evidence |
|---|---|---|---|
| 1 | `:app:testDebugUnitTest` | ✅ | `BUILD SUCCESSFUL in 1m 15s`; `compileDebugKotlin` + `compileDebugUnitTestKotlin` executed; JUnit XML: **35 tests, 0 failures, 0 errors** across 11 classes (23:58) |
| 2 | `:app:assembleDebug` | ✅ | `BUILD SUCCESSFUL in 9s`; `app-debug.apk` 27,111,913 B (23:58) — same size as P0-009 |
| 3 | `:app:assembleRelease` | ✅ on retry | **First attempt: Gradle daemon died inside `minifyReleaseWithR8`** — `hs_err_pid25108.log`: "insufficient memory … Native memory allocation (mmap) failed to map 299892736 bytes … G1 virtual space" (B-17, 16 GB host; 5.4 GB free at the time with one 1.36 GB daemon alive). Retry: `BUILD SUCCESSFUL in 2m 17s`, no "Missing class"; `app-release.apk` 5,867,725 B (00:03) — **byte-count identical to P0-009**; `mapping.txt`: `com.example.domain.repository.QuranRepository -> ik0`, `QuranRepositoryImpl` kept by name (Hilt); the shared super-interface does not appear — R8 merged it into the sub-interface (nothing else in the APK implements it), which is the expected outcome |
| 4 | `:shared:testAndroidHostTest` | ✅ | UP-TO-DATE (no `shared/**` input changed); JUnit XML: **29 tests, 0 failures, 0 errors** across 5 classes |

### Devil's Advocate checks

- **Behaviour:** the only removed semantics is the default `reciterIdentifier = "ar.alafasy"`, which no
  caller used (`QuranViewModel:291` passes both arguments; `grep "ar.alafasy"` over `app/src` + `shared/src`
  is empty). Identical release APK size corroborates "no code-path change".
- **Hilt (§3):** `@Binds bindQuranRepository(impl): QuranRepository` still binds the `:app` interface;
  `QuranRepositoryImpl` still declares `: QuranRepository` and overrides all 7 members — the 4 read members
  now satisfy the shared declarations through inheritance. Hilt never sees the shared type.
- **iOS direction (ADR-004):** `com.aistudio.quranblind.domain.repository.QuranRepository` is now the single
  declaration of the read contract; Android adds Room bookmarks on top. A future iOS implementation
  implements the shared interface only.
- **Blind-first (§4.1) / Uthmanic (§4.4) / no data collection (§4.3):** no UI, text, or I/O code touched.
- **Test-count parity:** 35 / 29 unchanged.
- **Process:** second consecutive OpenCode run with grep-only verification → completed, own report, final
  summary in English as instructed. New litter: `hs_err_pid25108.log` (root) from the R8 daemon crash — not
  deleted, per standing rule.

**QUALITY GATE: PASS** — ORDER-P0-010 is accepted for commit.
