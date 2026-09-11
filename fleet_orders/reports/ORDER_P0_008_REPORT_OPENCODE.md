# ORDER-P0-008 — Report

## Provenance

OpenCode CLI (`opencode/muse-spark-1.3-contributor-free`) executed Steps 1–3 of
`ORDER_P0_008_SANITIZE_CUTOVER.md` on 2026-09-11 (~22:10). Its own report was **never written**: the
first compile probe (`./gradlew :app:compileDebugKotlin`) hit the `lean-ctx` MCP request timeout on a cold
Gradle start, OpenCode retried, and the host's low-memory watchdog killed the whole process tree (B-17)
during the retry. Log: Commander's scratchpad `opencode_p0_008.log`; last line before the kill was the
retry of the first probe.

Everything below was produced by **Claude Code CLI** from the working tree OpenCode left behind, plus the
Commander's own reserved edit to `AyahCard.kt` (§2 of the order).

## BASELINE (already dirty before dispatch, untouched by this order)

`.agents/ACTIVE_CONTEXT_INJECTION.md`, `.agents/HOOKS_GUIDE.xlsx`, `.agents/MEMORY_STORE.md`,
`CLAUDE.md`, `fleet_config.json`, `opencode.json` (modified — Antigravity's OpenRouter fallback edits);
`remote_ios_dev_playbook_diagram.html`, `hs_err_pid*.log`, `replay_pid*.log` (untracked).

## DIFF — OpenCode's three files (`git diff --numstat`: added / removed)

| File | + | − | Order A3 target |
|---|---|---|---|
| `app/src/main/java/com/example/domain/repository/QuranRepository.kt` | 0 | 1 | +0 / −1 ✅ |
| `app/src/main/java/com/example/data/repository/QuranRepository.kt` | 1 | 10 | +1 / −10 ✅ |
| `app/src/test/java/com/example/data/repository/QuranRepositoryTest.kt` | 2 | 1 | +2 / −1 ✅ |

Interface — removed line:
```
-    fun sanitizeUthmanicText(text: String): String
```

`QuranRepositoryImpl` — added import after `android.content.Context`; removed the 9-line `override fun
sanitizeUthmanicText` (pass 1) plus its trailing blank line. Call sites at (now) lines 98 and 129 untouched
and resolving to the shared top-level function.

`QuranRepositoryTest` — added the same import; line 64 `repository.sanitizeUthmanicText(...)` →
`sanitizeUthmanicText(...)`; fixture and assertions unchanged.

## DIFF — Commander's reserved file

| File | + | − |
|---|---|---|
| `app/src/main/java/com/example/ui/components/player/AyahCard.kt` | 1 | 14 |

`+import com.aistudio.quranblind.domain.text.sanitizeUthmanicText` (alphabetical position, before
`com.example.accessibility`); removed lines 180–193: blank line, `private val bareNoonNextLetters`,
`private val noonSukoonPattern`, blank, and the 10-line `private fun sanitizeUthmanicText` (pass 2). No
`@Composable` body changed; `cleanText` at line 77 still calls `sanitizeUthmanicText(ayah.textArabic)`.

## PROBE OUTPUT

Not run by OpenCode (killed). Superseded by the gate below.

## A4 / A5 GREP OUTPUT

```
$ grep -rn "override fun sanitizeUthmanicText\|repository.sanitizeUthmanicText\|private fun sanitizeUthmanicText" app/src
(empty)
$ grep -rn "sanitizeUthmanicText" app/src/main/java
app/src/main/java/com/example/data/repository/QuranRepository.kt:4:import com.aistudio.quranblind.domain.text.sanitizeUthmanicText
app/src/main/java/com/example/data/repository/QuranRepository.kt:98:  ... sanitizeUthmanicText(it.textArabic) ...
app/src/main/java/com/example/data/repository/QuranRepository.kt:129: val text = sanitizeUthmanicText(item.getString("textArabic"))
app/src/main/java/com/example/ui/components/player/AyahCard.kt:32:import com.aistudio.quranblind.domain.text.sanitizeUthmanicText
app/src/main/java/com/example/ui/components/player/AyahCard.kt:77:        sanitizeUthmanicText(ayah.textArabic)
```

Exactly one definition remains in the repository: `shared/.../domain/text/UthmanicText.kt`.

## DEVIATIONS

None in the edits. Process deviation: no OpenCode-authored report (B-17 OOM kill, second time today).

## BLOCKED ON

Nothing.

## SELF-ASSESSMENT A1–A7 (by Claude Code CLI)

| # | Result |
|---|---|
| A1 | ✅ via gate (`compileDebugKotlin` / `compileDebugUnitTestKotlin` ran inside step 1) |
| A2 | ✅ exactly the three `M` files beyond baseline (plus the Commander's `AyahCard.kt`) |
| A3 | ✅ +0/−1, +1/−10, +2/−1 — exact |
| A4 | ✅ empty |
| A5 | ✅ import + two call sites in `QuranRepositoryImpl`, nothing else in `data/` or `domain/` |
| A6 | ✅ `git diff --stat` on `shared/`, `UthmanicTextTest.kt`, `build.gradle.kts`, `proguard-rules.pro` empty |
| A7 | ✅ no test, no commit, no push by OpenCode |

---

## QUALITY GATE — Claude Code CLI, 2026-09-11 22:20–22:27

Foreground, one Gradle invocation per call, daemons stopped before dispatch (B-17).

| Step | Command | Result | Evidence |
|---|---|---|---|
| 1 | `:app:testDebugUnitTest` | ✅ | `BUILD SUCCESSFUL in 55s`; JUnit XML: **35 tests, 0 failures, 0 errors** across 11 classes; `QuranRepositoryTest.testSanitizeUthmanicText_cleansSpecialZeroWidthCharacters` present and green (now exercising the shared function) |
| 2 | `:app:assembleDebug` | ✅ | `BUILD SUCCESSFUL in 15s`; `app-debug.apk` 25,454,340 B (22:24) |
| 3 | `:app:assembleRelease` | ✅ | `BUILD SUCCESSFUL in 2m 9s`; `minifyReleaseWithR8` clean, no "Missing class"; `app-release.apk` 5,851,341 B (22:26); `mapping.txt`: `com.aistudio.quranblind.domain.text.UthmanicTextKt -> jz0` with `noonSukoonPattern` retained, the function body **inlined** into callers with 9 source-line frames pointing at `UthmanicText.kt:7-14` (all 8 steps); **0** `AyahCardKt`/`QuranRepositoryImpl` sanitizer frames |
| 4 | `:shared:testAndroidHostTest` | ✅ | UP-TO-DATE (no `shared/**` input changed); JUnit XML: **29 tests, 0 failures, 0 errors**; `UthmanicTextTest` 8 cases incl. idempotence |

Devil's Advocate checks:
- **Uthmanic integrity (§4.4, the one that matters):** the deleted `AyahCard` copy was character-identical
  to the shared function (P0-003b A4), so the UI path is `pass2(x)` before and after. The repository path
  went from pass 1 to pass 2, so the screen now receives `pass2(pass2(x))` instead of `pass2(pass1(x))`;
  `pass2` is idempotent (tested) and pass 1 is a step-subset of pass 2 in the same order, so the rendered
  bytes are unchanged. `Ayah.textArabic` has exactly one reader (`AyahCard.kt:76-77`) — no other consumer
  sees the repository's output.
- **Room cache:** newly cached `AyahEntity.textArabic` rows are pass-2 output instead of pass-1; old rows
  still go through `AyahCard`'s pass 2. No migration, no visible change.
- **Blind-first (§4.1):** no composable body, semantics, or audio file touched — `AyahCard.kt` diff is
  one import and one private-helper deletion; `clearAndSetSemantics` block untouched.
- **R8 risk:** the shared function is a top-level `fun` in `commonMain`; R8 inlined it (fine — the regex
  static survives in `jz0`, the residual frames prove every step is present). No new keep rule needed.
- **Test-count parity:** 35 in `:app` unchanged (the repository test was repointed, not deleted).

**QUALITY GATE: PASS** — ORDER-P0-008 is accepted for commit.
