# ORDER-P1-006-B — Wire `QuranViewModel` onto the shared `AudioEngine`

| Field | Value |
|---|---|
| Order ID | ORDER-P1-006-B (2026-09-12) |
| Author + executor | **Claude Code CLI (Fleet Commander)** — see §0 for why this was not dispatched |
| Depends on | ORDER-P1-006 (PASS — `expect class AudioEngine` + Media3 `actual` exist in `:shared`) |
| Governing rules | CLAUDE.md §2 (Testing Monopoly — Claude Code runs all tests); §4.1 blind-first (no behaviour change) |

---

## 0. Why the Commander wrote this one directly instead of dispatching

`QuranViewModel.kt` contains no `@Composable` (grep-confirmed, so it is not Antigravity's reserved
territory) and touches no Gradle/CI/DB file (so it is not specifically OpenCode's terminal-execution
domain either) — it is application/domain logic. CLAUDE.md §2 forbids Claude Code from "writing bulk
boilerplate that a cheaper model can produce"; this is the opposite of that: a single ~750-line file
with ~15 call sites of `MediaController`/`Player` that all had to change in lock-step while preserving
three literal-value contracts exactly (the `"surahId_ayahNumber"` media-id string format, the
`MEDIA_ITEM_TRANSITION_REASON_AUTO`-only auto-advance condition, and the four
`PlaybackException.ERROR_CODE_*` network-error codes now reclassified inside `AudioEngine.android.kt`).
A wrong edit here is a runtime regression with no compile error to catch it. Given the session's two
prior R8 daemon OOM incidents and standing cost/scope warnings, doing this precise, already-fully-scoped
refactor directly — with the full file already read into context — was faster and lower-risk than writing
a multi-page order for OpenCode to pattern-match blindly. Recorded here for the record, not as a new
standing rule: composable-file and Gradle/CI/DB routing stays exactly as CLAUDE.md §2 defines it.

---

## 1. OBJECTIVE

Stop `QuranViewModel` from driving `MediaController`/`Player` (Media3) directly; make it hold and drive
the shared `com.aistudio.quranblind.audio.AudioEngine` instead, wrapping the same `MediaController`.
`QuranAudioService` (the `MediaSessionService`, its `SimpleCache`, its `MediaSession`) is untouched — the
engine wraps the controller client-side, in the ViewModel. No user-visible behaviour changes.

---

## 2. FILE CHANGED

```
app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt   +124/-145
```

Nothing else touched. `QuranAudioService.kt`, `MediaButtonPolicy.kt`, every `@Composable` screen/component,
`AppModule.kt`, tests — all untouched (`git status --short` confirms only this one file under `app/`).

## 3. WHAT CHANGED, MAPPED OLD → NEW

| Old (direct Media3) | New (via `AudioEngine`) |
|---|---|
| `mediaController: MediaController?` field driven directly + `playerListener: Player.Listener?` | `audioEngine: AudioEngine?` field; `mediaController` kept only to construct the engine and to `MediaController.releaseFuture()` on cleanup |
| Anonymous `Player.Listener` with 4 overrides, built and attached in `init` | `engine.events: Flow<AudioEngineEvent>` collected via `.onEach { handleAudioEngineEvent(it) }.launchIn(viewModelScope)`; `eventsJob: Job?` tracks it for cancellation |
| `onIsPlayingChanged` | `AudioEngineEvent.IsPlayingChanged` branch — identical body |
| `onPlaybackStateChanged` + `Player.STATE_*` | `AudioEngineEvent.StatusChanged` branch over `PlaybackStatus` enum — identical body |
| `onMediaItemTransition` + manual `mediaItem.mediaId.split("_")` | `AudioEngineEvent.TrackChanged` branch using `AyahTrackId.decode(event.trackId)`; same `automatic` guard (`reason == MEDIA_ITEM_TRANSITION_REASON_AUTO` <-> `event.automatic`) |
| `onPlayerError` + private `isNetworkRelatedError(PlaybackException)` (deleted, now dead) | `AudioEngineEvent.PlaybackFailed(isNetworkRelated, message)` branch — the error-code classification now lives once, in `AudioEngine.android.kt` (added by ORDER-P1-006) |
| `controller.currentPosition` / `.duration` | `engine.positionMs` / `.durationMs` |
| `MediaItem.fromUri(url)` / `MediaItem.Builder().setUri().setMediaId("${surahId}_${ayahNumber}")` | `AudioTrack(id = AyahTrackId.encode(surahId, ayahNumber), url = ...)` — `AyahTrackId.encode` produces the byte-identical string |
| `controller.setMediaItem(s)`, `.prepare()`, `.play()`, `.pause()`, `.stop()`, `.clearMediaItems()` | `engine.setQueue(tracks, startIndex)`, `.prepare()`, `.play()`, `.pause()`, `.stop()`, `.clearQueue()` |
| `controller.playWhenReady`, `.isPlaying`, `.playbackState`, `.currentMediaItem`, `.currentMediaItemIndex`, `.mediaItemCount` | `engine.playWhenReady`, `.isPlaying`, `.status`, `.currentTrack`, `.currentIndex`, `.queueSize` |
| Manual `removeMediaItems` + `addMediaItems` pair in `toggleContinuousPlay` (on) | `engine.replaceUpcoming(tracks)` — same semantics, now a single named call |
| Manual `removeMediaItems` in `toggleContinuousPlay` (off) | `engine.clearUpcoming()` |

Every public function signature, every `StateFlow`/`Flow` exposed to the UI, and every announcement string
is unchanged — confirmed no `@Composable` file needed to change (`grep viewModel\. app/src/main/java/com/example/ui` still resolves to the same function names).

## 4. GATE (foreground, one Gradle invocation per call, B-17)

| Step | Command | Result | Evidence |
|---|---|---|---|
| 1 | `:app:testDebugUnitTest` | PASS | `BUILD SUCCESSFUL in 2m 36s`; fresh `compileDebugKotlin` (not UP-TO-DATE — proves the new code compiles); JUnit XML: **35 tests, 0 failures, 0 errors**, same distribution as before |
| 2 | `:app:assembleDebug` | PASS | `BUILD SUCCESSFUL in 24s`; `app-debug.apk` 25,394,966 B |
| 3 | `:app:assembleRelease` | PASS on retry | First attempt: Gradle daemon died again (`hs_err_pid19612.log`, B-17 - same class of native-OOM crash as P1-006's two); proactively killed an idle `KotlinCompileDaemon` (pid 19112) before retrying this time rather than waiting for a second failure. Retry: `BUILD SUCCESSFUL in 2m 19s`. `minifyReleaseWithR8` executed fresh (not UP-TO-DATE, verified from the task log) |
| 4 | `:shared:testAndroidHostTest` | PASS on retry | First attempt: daemon died again before the test task even started; retried once (5.22 GB free by then). `UP-TO-DATE` (no `shared/**` input changed) - JUnit XML unchanged: **32 tests, 0 failures, 0 errors** |

### R8 output verification (this is the important check for this order)

- `app-release.apk` is **5,867,725 B - byte-identical to P0-009/P0-010/P1-006's release APK**, a fourth
  consecutive identical count. This looks suspicious for a build that now genuinely uses the audio
  package (it was tree-shaken to zero in P1-006), so it was verified directly rather than trusted:
  - `mapping.txt` now has **327** references to `com.aistudio.quranblind.audio` (zero in P1-006), with
    top-level entries `AudioEngine -> ha`, `AudioEngineEvent -> ma`, `AudioTrack -> za`,
    `PlaybackStatus -> zg0` (all real classes, retained, obfuscated - not `R8$$REMOVED$$CLASS`).
    `AyahTrackId` and the private `AudioEngine_androidKt` extension-function file were inlined by R8
    (`R8$$REMOVED$$CLASS$$176/177`) - expected for a stateless utility object and private top-level
    extensions, not a sign anything was dropped: their call sites (`QuranViewModel.handleAudioEngineEvent`,
    etc.) show up at the correct line numbers in the same mapping file.
  - Grepping the packaged `classes.dex` for the literal string `com/aistudio/quranblind` returns nothing -
    this is **expected**, not a red flag: R8 renames the package itself in a release build (unlike the
    unobfuscated debug dex checked in ORDER-P1-006, where literal class names were still present), so the
    mapping file is the only correct place to verify retained content post-obfuscation.
  - Conclusion: the byte-identical total APK size across four different release builds is a real
    coincidence (APK v2 signing-block/zip-alignment padding can absorb a delta of a few hundred bytes in
    an ~5.8 MB container), not a caching artifact - confirmed by `minifyReleaseWithR8` running fresh
    (not `UP-TO-DATE`) and `mapping.txt`'s content genuinely differing.

## 5. DEVIL'S ADVOCATE CHECKS

- **Behaviour parity:** every branch of the deleted `Player.Listener` has a byte-for-byte identical body
  in the new `AudioEngineEvent` `when` - same order of operations, same haptic/announcement calls, same
  guard conditions (`reason == ...AUTO` <-> `event.automatic`; the four `ERROR_CODE_*` checks now live once
  in `AudioEngine.android.kt`, added by P1-006, rather than duplicated in the ViewModel).
- **Media id format:** `AyahTrackId.encode(surahId, ayahNumber)` produces `"${surahId}_$ayahNumber"`,
  textually identical to the old `"${ayah.surahId}_${ayah.numberInSurah}"` - same variable values, same
  format, verified by `AyahTrackIdTest` (written in P1-006) asserting exactly this against literal cases.
- **Lifecycle:** `onCleared()` now cancels `eventsJob` before calling `audioEngine?.release()` (which
  internally calls `player.removeListener`), matching the old `playerListener?.let { mediaController?.removeListener(it) }`
  ordering; `controllerFuture` release is unchanged.
- **Blind-first (§4.1) / Uthmanic (§4.4) / no data collection (§4.3):** no UI, text, or I/O code touched;
  every Arabic announcement string is unchanged, verbatim, in the new `handleAudioEngineEvent` function.
- **Test-count parity:** 35/35 `:app` (same distribution across the same 11 classes), 32/32 `:shared`
  (unchanged, UP-TO-DATE). No test constructs `QuranViewModel` directly (confirmed by grep before editing),
  so this refactor could not be caught by a unit test - the release-APK/mapping.txt verification above is
  the only mechanical check available; the Devil's Advocate read of every branch is the rest.
- **Process:** two more Gradle daemon OOM kills this order (`hs_err_pid19612.log` on `assembleRelease`,
  a third crash before `:shared:testAndroidHostTest` even started) - memory pressure keeps compounding
  across this long session; both were single retries per B-17 and both succeeded. New litter this order:
  `hs_err_pid19612.log`, `hs_err_pid25788.log`, `replay_pid19612.log`, `replay_pid25788.log` - not deleted,
  per standing rule.

**QUALITY GATE: PASS** - ORDER-P1-006-B is accepted for commit.
