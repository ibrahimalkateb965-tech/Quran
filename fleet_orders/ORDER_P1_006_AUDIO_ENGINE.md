# ORDER-P1-006 — `expect class AudioEngine` in `:shared` with the Media3 `actual`

| Field | Value |
|---|---|
| Order ID | ORDER-P1-006 (rev. A, 2026-09-12) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — see §0.3 for why not Antigravity |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-12** |
| Depends on | ORDER-P0-009 / P0-010 (PASS — shared models and read contract are the only declarations left) |
| Governing rules | CLAUDE.md §2 (no tests, no Gradle *invocation*, no `@Composable` files); §3 (`expect class AudioEngine` → Media3 / AVPlayer); §4.1 blind-first |

---

## 0. GROUND TRUTH (read from disk 2026-09-12 by Claude Code CLI)

### 0.1 What `:app` does with audio today (unchanged by this order)

- `app/src/main/java/com/example/service/QuranAudioService.kt` — `MediaSessionService` owning the `ExoPlayer`,
  the 200 MB `SimpleCache`, the `MediaSession`, call-end media-button policy. **Not touched.**
- `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` — holds a `MediaController` (which *is* a
  `androidx.media3.common.Player`) and drives it directly: `setMediaItem(s)`, `addMediaItems`,
  `removeMediaItems`, `prepare`, `play`, `pause`, `stop`, `clearMediaItems`, reads `isPlaying`,
  `playWhenReady`, `playbackState`, `currentPosition`, `duration`, `currentMediaItem`,
  `currentMediaItemIndex`, `mediaItemCount`; listens to `onIsPlayingChanged`, `onPlaybackStateChanged`,
  `onMediaItemTransition` (only `MEDIA_ITEM_TRANSITION_REASON_AUTO`), `onPlayerError` (network-related =
  `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED`, `ERROR_CODE_IO_BAD_HTTP_STATUS`,
  `ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE`, `ERROR_CODE_TIMEOUT`). Media IDs are built as
  `"${ayah.surahId}_${ayah.numberInSurah}"` and parsed back with `split("_")`. **Not touched by this order** —
  the cut-over of the ViewModel onto `AudioEngine` is the next order (P1-006-B).

### 0.2 `:shared` today

- Existing platform abstractions follow two shapes: `expect object PlatformLogger` (`network/Logger.kt` +
  `Logger.android.kt` / `Logger.ios.kt`) and `expect fun createSecureStore(name)` (`store/SecureStore.kt` +
  `.android.kt` / `.ios.kt`). Files are named `<Name>.kt` / `<Name>.android.kt` / `<Name>.ios.kt`.
- `shared/build.gradle.kts` `androidMain.dependencies` currently: `ktor-client-okhttp`, `androidx-security-crypto`.
  There is **no** Media3 dependency in `:shared`; `gradle/libs.versions.toml` has `media3 = "1.5.1"` and the
  aliases `androidx-media3-exoplayer|session|ui|database` (lines 95–98) but **no `media3-common`** alias.
  `Player`, `MediaItem`, `PlaybackException`, `C` all live in the `media3-common` artifact.
- Kotlin 2.2.10. `expect`/`actual` classes emit the "Beta" warning; it is already emitted for
  `PlatformLogger` and is **not** an error (no `allWarningsAsErrors` anywhere). Do not add compiler flags.
- iOS targets cannot be compiled on this Windows host; `iosMain` sources are verified only by the (queued)
  P1-007 CI rewrite. The iOS `actual` in this order is therefore a **compile-shaped placeholder** (§4 Step 6);
  the real `AVQueuePlayer` + `AVAudioSession` + interruption implementation is ORDER-P1-006-IOS, to be issued
  when a macOS verifier exists.

### 0.3 Owner deviation

The Phase 1 queue lists `antigravity-ide` as owner of P1-006. Antigravity has no non-interactive channel on this
host (`agy --print` is the Electron launcher, not a CLI — verified 2026-09-12). OpenCode CLI takes the file
writing; the API design below is the Commander's. Recorded in `CURRENT_STATE.md`.

---

## 1. OBJECTIVE

Add the platform-neutral audio contract to `:shared` so that a future Android ViewModel and the SwiftUI app
drive one API: `expect class AudioEngine` with a queue of `AudioTrack`s, imperative controls, snapshot
properties, and a `Flow<AudioEngineEvent>`. The Android `actual` wraps any `androidx.media3.common.Player`
(in `:app` that will be the existing `MediaController`), so the service, cache and session are untouched.
**No behaviour changes anywhere in `:app`. No existing file changes except the two Gradle lines.**

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
gradle/libs.versions.toml                                                            MODIFY — one line (§4 Step 1)
shared/build.gradle.kts                                                              MODIFY — one line (§4 Step 2)
shared/src/commonMain/kotlin/com/aistudio/quranblind/audio/AudioTrack.kt            CREATE (§4 Step 3)
shared/src/commonMain/kotlin/com/aistudio/quranblind/audio/AyahTrackId.kt           CREATE (§4 Step 3)
shared/src/commonMain/kotlin/com/aistudio/quranblind/audio/PlaybackStatus.kt        CREATE (§4 Step 3)
shared/src/commonMain/kotlin/com/aistudio/quranblind/audio/AudioEngineEvent.kt      CREATE (§4 Step 3)
shared/src/commonMain/kotlin/com/aistudio/quranblind/audio/AudioEngine.kt           CREATE (§4 Step 4)
shared/src/androidMain/kotlin/com/aistudio/quranblind/audio/AudioEngine.android.kt  CREATE (§4 Step 5)
shared/src/iosMain/kotlin/com/aistudio/quranblind/audio/AudioEngine.ios.kt          CREATE (§4 Step 6)
```

**Reserved to the Commander (written in parallel, do not create or touch):**
`shared/src/commonTest/kotlin/com/aistudio/quranblind/audio/AyahTrackIdTest.kt`. If you see it already
present, report it as an expected variance, not a deviation.

## 3. FILES YOU MUST NOT TOUCH

Everything else — in particular `app/**` (the ViewModel and service stay exactly as they are),
`shared/src/commonTest/**`, every other file under `shared/src/**`, `settings.gradle.kts`,
`app/build.gradle.kts`, `proguard-rules.pro`, `.github/**`, `CLAUDE.md`, `fleet_config.json`,
`opencode.json`, `.agents/**`. Do not delete any `hs_err_pid*.log` / `replay_pid*.log`.

---

## 4. STEPS

All files: ASCII, LF line endings, single trailing newline, 4-space indent. Content is given verbatim —
copy it, do not "improve" it.

### Step 1 — `gradle/libs.versions.toml`: add one alias

Directly **after** the line

```toml
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
```

insert

```toml
androidx-media3-common = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }
```

Nothing else in the file changes.

### Step 2 — `shared/build.gradle.kts`: one dependency

In `sourceSets { androidMain.dependencies { … } }`, directly **after**
`implementation(libs.androidx.security.crypto)`, add:

```kotlin
            implementation(libs.androidx.media3.common)
```

Nothing else in the file changes.

### Step 3 — commonMain value types

`AudioTrack.kt`:

```kotlin
package com.aistudio.quranblind.audio

/**
 * One queue entry. [id] is opaque to the engine and is echoed back in
 * [AudioEngineEvent.TrackChanged]; the app builds it with [AyahTrackId].
 */
data class AudioTrack(val id: String, val url: String)
```

`AyahTrackId.kt`:

```kotlin
package com.aistudio.quranblind.audio

/**
 * Encodes an ayah as a track id. The format is "<surahId>_<ayahNumber>" and must stay
 * byte-identical to the media ids the Android ViewModel builds today.
 */
object AyahTrackId {
    fun encode(surahId: Int, ayahNumber: Int): String = "${surahId}_$ayahNumber"

    /** Returns (surahId, ayahNumber) or null when [id] is not of the form "<int>_<int>". */
    fun decode(id: String): Pair<Int, Int>? {
        val parts = id.split("_")
        if (parts.size != 2) return null
        val surahId = parts[0].toIntOrNull() ?: return null
        val ayahNumber = parts[1].toIntOrNull() ?: return null
        return surahId to ayahNumber
    }
}
```

`PlaybackStatus.kt`:

```kotlin
package com.aistudio.quranblind.audio

/** Mirrors Media3's Player.STATE_* and the AVPlayer status/end-of-item pair. */
enum class PlaybackStatus { IDLE, BUFFERING, READY, ENDED }
```

`AudioEngineEvent.kt`:

```kotlin
package com.aistudio.quranblind.audio

sealed interface AudioEngineEvent {
    data class IsPlayingChanged(val isPlaying: Boolean) : AudioEngineEvent

    data class StatusChanged(val status: PlaybackStatus) : AudioEngineEvent

    /**
     * The engine moved to another queue entry. [automatic] is true only when the previous
     * track finished on its own (Media3: MEDIA_ITEM_TRANSITION_REASON_AUTO), false for
     * setQueue / seek / repeat.
     */
    data class TrackChanged(val trackId: String?, val automatic: Boolean) : AudioEngineEvent

    /** [isNetworkRelated] drives the retry-with-backoff path; anything else is announced once. */
    data class PlaybackFailed(val isNetworkRelated: Boolean, val message: String?) : AudioEngineEvent
}
```

### Step 4 — commonMain `AudioEngine.kt` (the contract)

```kotlin
package com.aistudio.quranblind.audio

import kotlinx.coroutines.flow.Flow

/**
 * Platform audio engine. Android wraps a Media3 Player, iOS an AVQueuePlayer.
 *
 * Threading: every member must be used from the platform's main thread, like the
 * player it wraps. The engine never owns the player; [release] only detaches listeners.
 * Constructors are platform-specific and therefore not declared here.
 */
expect class AudioEngine {
    /** Hot stream of state changes; late subscribers miss earlier events. */
    val events: Flow<AudioEngineEvent>

    val isPlaying: Boolean
    val playWhenReady: Boolean
    val status: PlaybackStatus
    val positionMs: Long
    /** -1 when unknown. */
    val durationMs: Long
    val currentTrack: AudioTrack?
    /** Index of the current entry, or -1 when the queue is empty. */
    val currentIndex: Int
    val queueSize: Int

    /** Replaces the whole queue and positions on [startIndex] at 0 ms. Does not prepare or play. */
    fun setQueue(tracks: List<AudioTrack>, startIndex: Int)
    /** Drops every entry after the current one, then appends [tracks]. */
    fun replaceUpcoming(tracks: List<AudioTrack>)
    /** Drops every entry after the current one. */
    fun clearUpcoming()
    fun prepare()
    fun play()
    fun pause()
    fun stop()
    fun clearQueue()
    /** Detaches from the player. The engine must not be used afterwards. */
    fun release()
}
```

### Step 5 — androidMain `AudioEngine.android.kt` (Media3 actual)

```kotlin
package com.aistudio.quranblind.audio

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Media3 actual. Wraps any [Player] - in :app that is the MediaController bound to
 * QuranAudioService, so the service, its cache and its MediaSession stay untouched.
 * The caller owns the Player's lifecycle; [release] only removes the listener.
 */
actual class AudioEngine(private val player: Player) {

    private val _events = MutableSharedFlow<AudioEngineEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    actual val events: Flow<AudioEngineEvent> = _events.asSharedFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _events.tryEmit(AudioEngineEvent.IsPlayingChanged(isPlaying))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _events.tryEmit(AudioEngineEvent.StatusChanged(playbackState.toStatus()))
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _events.tryEmit(
                AudioEngineEvent.TrackChanged(
                    trackId = mediaItem?.mediaId,
                    automatic = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                )
            )
        }

        override fun onPlayerError(error: PlaybackException) {
            _events.tryEmit(AudioEngineEvent.PlaybackFailed(error.isNetworkRelated(), error.message))
        }
    }

    init {
        player.addListener(listener)
    }

    actual val isPlaying: Boolean get() = player.isPlaying
    actual val playWhenReady: Boolean get() = player.playWhenReady
    actual val status: PlaybackStatus get() = player.playbackState.toStatus()
    actual val positionMs: Long get() = player.currentPosition
    actual val durationMs: Long get() = player.duration.let { if (it == C.TIME_UNSET) -1L else it }
    actual val currentTrack: AudioTrack? get() = player.currentMediaItem?.toTrack()
    actual val currentIndex: Int get() = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
    actual val queueSize: Int get() = player.mediaItemCount

    actual fun setQueue(tracks: List<AudioTrack>, startIndex: Int) {
        player.setMediaItems(tracks.map { it.toMediaItem() }, startIndex, 0L)
    }

    actual fun replaceUpcoming(tracks: List<AudioTrack>) {
        clearUpcoming()
        player.addMediaItems(player.currentMediaItemIndex + 1, tracks.map { it.toMediaItem() })
    }

    actual fun clearUpcoming() {
        val next = player.currentMediaItemIndex + 1
        if (player.mediaItemCount > next) {
            player.removeMediaItems(next, player.mediaItemCount)
        }
    }

    actual fun prepare() = player.prepare()
    actual fun play() = player.play()
    actual fun pause() = player.pause()
    actual fun stop() = player.stop()
    actual fun clearQueue() = player.clearMediaItems()

    actual fun release() {
        player.removeListener(listener)
    }
}

private fun AudioTrack.toMediaItem(): MediaItem =
    MediaItem.Builder().setUri(url).setMediaId(id).build()

private fun MediaItem.toTrack(): AudioTrack =
    AudioTrack(id = mediaId, url = localConfiguration?.uri?.toString() ?: "")

private fun Int.toStatus(): PlaybackStatus = when (this) {
    Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
    Player.STATE_READY -> PlaybackStatus.READY
    Player.STATE_ENDED -> PlaybackStatus.ENDED
    else -> PlaybackStatus.IDLE
}

private fun PlaybackException.isNetworkRelated(): Boolean = when (errorCode) {
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
    PlaybackException.ERROR_CODE_TIMEOUT -> true
    else -> false
}
```

### Step 6 — iosMain `AudioEngine.ios.kt` (placeholder, see §0.2)

```kotlin
package com.aistudio.quranblind.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS placeholder so SharedKit links. The AVQueuePlayer + AVAudioSession implementation
 * (interruption handling, VoiceOver-safe focus) is ORDER-P1-006-IOS and needs a macOS
 * verifier; nothing on iOS calls this yet.
 */
actual class AudioEngine {
    actual val events: Flow<AudioEngineEvent> = emptyFlow()

    actual val isPlaying: Boolean get() = false
    actual val playWhenReady: Boolean get() = false
    actual val status: PlaybackStatus get() = PlaybackStatus.IDLE
    actual val positionMs: Long get() = 0L
    actual val durationMs: Long get() = -1L
    actual val currentTrack: AudioTrack? get() = null
    actual val currentIndex: Int get() = -1
    actual val queueSize: Int get() = 0

    actual fun setQueue(tracks: List<AudioTrack>, startIndex: Int) = notReady()
    actual fun replaceUpcoming(tracks: List<AudioTrack>) = notReady()
    actual fun clearUpcoming() = notReady()
    actual fun prepare() = notReady()
    actual fun play() = notReady()
    actual fun pause() = notReady()
    actual fun stop() = notReady()
    actual fun clearQueue() = notReady()
    actual fun release() = notReady()

    private fun notReady(): Nothing =
        throw NotImplementedError("AudioEngine on iOS is ORDER-P1-006-IOS")
}
```

### Step 7 — Verify by grep only (NO Gradle, NO compile probes)

```bash
grep -rn "actual class AudioEngine\|expect class AudioEngine" shared/src
```

must show exactly 3 lines (commonMain expect, androidMain actual, iosMain actual).

```bash
grep -rn "media3" gradle/libs.versions.toml shared/build.gradle.kts
```

must show 6 lines in the toml (`media3 =`, exoplayer, common, session, ui, database) and exactly 1 line in
`shared/build.gradle.kts`.

```bash
grep -rn "com.aistudio.quranblind.audio" app/src
```

must return nothing (this order does not touch `:app`).

Do **not** run `./gradlew` for any reason (the Commander compiles at the gate).

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | `git status --short` beyond the pre-existing baseline shows exactly: ` M gradle/libs.versions.toml`, ` M shared/build.gradle.kts`, and `??` for the 7 new files under `shared/src/**/audio/` (plus, as expected variance, the Commander's `AyahTrackIdTest.kt`) |
| A2 | `git diff --numstat` is **+1 / −0** for each of the two Gradle files |
| A3 | Step 7 greps match |
| A4 | Each new file's content is verbatim §4 (ASCII, LF, single trailing newline) |
| A5 | No Gradle invocation, no test executed by you; no commit, no push; nothing under `app/` changed |

## 6. REPORT-BACK

`fleet_orders/reports/ORDER_P1_006_REPORT_OPENCODE.md`, in **English**, sections: BASELINE (`git status
--short` before you start), `git diff` of the two Gradle files (verbatim), `git status --short` AFTER, STEP 7
GREP OUTPUT verbatim (all three greps), DEVIATIONS, BLOCKED ON, SELF-ASSESSMENT A1–A5. Your final chat summary
must also be in English (CLAUDE.md §1).

FORBIDDEN: any `./gradlew` invocation; running any test; touching any file outside §2; deleting log files;
git commit/push. STOP after reporting.
