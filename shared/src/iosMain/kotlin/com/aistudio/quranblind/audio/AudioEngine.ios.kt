@file:OptIn(ExperimentalForeignApi::class)

package com.aistudio.quranblind.audio

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * AVPlayer actual. The engine owns one AVPlayer and holds one AVPlayerItem at a time;
 * the queue lives in [TrackQueue] so replaceUpcoming/clearUpcoming are exact. Every
 * member must be called on the main thread; KVO callbacks are re-dispatched there.
 * AVAudioSession is set to playback and activated on the first play(); interruptions
 * (calls, Siri, VoiceOver announcements with audio ducking) pause and resume playback.
 */
actual class AudioEngine {

    private val player = AVPlayer()
    private val queue = TrackQueue()

    private val _events = MutableSharedFlow<AudioEngineEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    actual val events: Flow<AudioEngineEvent> = _events.asSharedFlow()

    private var wantPlay = false
    private var ended = false
    private var pausedByInterruption = false
    private var lastIsPlaying = false
    private var lastStatus = PlaybackStatus.IDLE
    private var released = false

    private val center = NSNotificationCenter.defaultCenter
    private val notificationTokens = mutableListOf<NSObjectProtocol>()

    private val kvo = object : NSObject() {
        override fun observeValueForKeyPath(
            keyPath: String?,
            ofObject: Any?,
            change: Map<Any?, *>?,
            context: COpaquePointer?
        ) {
            // AVFoundation may notify off the main thread; the engine's state is main-only.
            dispatch_async(dispatch_get_main_queue()) {
                if (released) return@dispatch_async
                when (keyPath) {
                    "timeControlStatus" -> publishState()
                    "status" -> onItemStatusChanged(ofObject as? AVPlayerItem)
                }
            }
        }
    }

    init {
        player.addObserver(kvo, forKeyPath = "timeControlStatus", options = NSKeyValueObservingOptionNew, context = null)
        notificationTokens += center.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification, null, NSOperationQueue.mainQueue
        ) { notification ->
            if (!released && notification?.`object` === player.currentItem) onPlayedToEnd()
        }
        notificationTokens += center.addObserverForName(
            AVPlayerItemFailedToPlayToEndTimeNotification, null, NSOperationQueue.mainQueue
        ) { notification ->
            if (!released && notification?.`object` === player.currentItem) {
                val error = notification?.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
                fail(error)
            }
        }
        notificationTokens += center.addObserverForName(
            AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue
        ) { notification ->
            if (!released) onInterruption(notification)
        }
    }

    actual val isPlaying: Boolean
        get() = player.timeControlStatus == AVPlayerTimeControlStatusPlaying
    actual val playWhenReady: Boolean
        get() = wantPlay
    actual val status: PlaybackStatus
        get() = computeStatus()
    actual val positionMs: Long
        get() = secondsToMs(CMTimeGetSeconds(player.currentTime()))
    actual val durationMs: Long
        get() {
            val item = player.currentItem ?: return -1L
            return secondsToMs(CMTimeGetSeconds(item.duration))
        }
    actual val currentTrack: AudioTrack?
        get() = queue.current
    actual val currentIndex: Int
        get() = queue.index
    actual val queueSize: Int
        get() = queue.size

    actual fun setQueue(tracks: List<AudioTrack>, startIndex: Int) {
        queue.set(tracks, startIndex)
        detachCurrentItem()
        _events.tryEmit(AudioEngineEvent.TrackChanged(trackId = queue.current?.id, automatic = false))
        publishState()
    }

    actual fun replaceUpcoming(tracks: List<AudioTrack>) {
        val hadCurrent = queue.current != null
        queue.replaceUpcoming(tracks)
        if (!hadCurrent && queue.current != null) {
            _events.tryEmit(AudioEngineEvent.TrackChanged(trackId = queue.current?.id, automatic = false))
        }
    }

    actual fun clearUpcoming() {
        queue.clearUpcoming()
    }

    actual fun prepare() {
        if (player.currentItem == null) loadCurrentItem()
    }

    actual fun play() {
        wantPlay = true
        pausedByInterruption = false
        activateSession()
        if (player.currentItem == null) loadCurrentItem()
        if (ended) {
            ended = false
            player.seekToTime(CMTimeMakeWithSeconds(0.0, 1))
        }
        player.play()
        publishState()
    }

    actual fun pause() {
        wantPlay = false
        pausedByInterruption = false
        player.pause()
        publishState()
    }

    actual fun stop() {
        wantPlay = false
        pausedByInterruption = false
        player.pause()
        detachCurrentItem()
        publishState()
    }

    actual fun clearQueue() {
        stop()
        queue.clear()
        _events.tryEmit(AudioEngineEvent.TrackChanged(trackId = null, automatic = false))
    }

    actual fun release() {
        if (released) return
        released = true
        player.pause()
        detachCurrentItem()
        player.removeObserver(kvo, forKeyPath = "timeControlStatus")
        notificationTokens.forEach { center.removeObserver(it) }
        notificationTokens.clear()
    }

    // --- internals -------------------------------------------------------------------

    private fun loadCurrentItem() {
        detachCurrentItem()
        val track = queue.current ?: return
        val url = NSURL(string = track.url)
        val item = AVPlayerItem.playerItemWithURL(url)
        item.addObserver(kvo, forKeyPath = "status", options = NSKeyValueObservingOptionNew, context = null)
        player.replaceCurrentItemWithPlayerItem(item)
        ended = false
        publishState()
    }

    private fun detachCurrentItem() {
        val item = player.currentItem ?: return
        item.removeObserver(kvo, forKeyPath = "status")
        player.replaceCurrentItemWithPlayerItem(null)
        ended = false
    }

    private fun onItemStatusChanged(item: AVPlayerItem?) {
        if (item == null || item !== player.currentItem) return
        if (item.status == AVPlayerItemStatusFailed) {
            fail(item.error)
        } else {
            publishState()
        }
    }

    private fun onPlayedToEnd() {
        if (queue.advance()) {
            loadCurrentItem()
            _events.tryEmit(AudioEngineEvent.TrackChanged(trackId = queue.current?.id, automatic = true))
            if (wantPlay) player.play()
        } else {
            ended = true
        }
        publishState()
    }

    private fun onInterruption(notification: NSNotification?) {
        val info = notification?.userInfo ?: return
        val type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedLongValue ?: return
        when (type) {
            AVAudioSessionInterruptionTypeBegan -> {
                if (wantPlay && isPlaying) {
                    pausedByInterruption = true
                    player.pause()
                    publishState()
                }
            }
            AVAudioSessionInterruptionTypeEnded -> {
                val options = (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedLongValue ?: 0uL
                val shouldResume = (options and AVAudioSessionInterruptionOptionShouldResume) != 0uL
                if (pausedByInterruption && shouldResume && wantPlay) {
                    activateSession()
                    player.play()
                }
                pausedByInterruption = false
                publishState()
            }
        }
    }

    private fun fail(error: NSError?) {
        val network = error?.domain == NSURLErrorDomain
        _events.tryEmit(AudioEngineEvent.PlaybackFailed(isNetworkRelated = network, message = error?.localizedDescription))
        publishState()
    }

    private fun activateSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    private fun computeStatus(): PlaybackStatus {
        val item = player.currentItem ?: return PlaybackStatus.IDLE
        if (ended) return PlaybackStatus.ENDED
        if (item.status == AVPlayerItemStatusFailed) return PlaybackStatus.IDLE
        val waiting = player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
        return if (item.status == AVPlayerItemStatusReadyToPlay && !waiting) PlaybackStatus.READY else PlaybackStatus.BUFFERING
    }

    /** Emits IsPlayingChanged / StatusChanged only when the value actually changed, like Media3. */
    private fun publishState() {
        val playing = isPlaying
        if (playing != lastIsPlaying) {
            lastIsPlaying = playing
            _events.tryEmit(AudioEngineEvent.IsPlayingChanged(playing))
        }
        val current = computeStatus()
        if (current != lastStatus) {
            lastStatus = current
            _events.tryEmit(AudioEngineEvent.StatusChanged(current))
        }
    }

    private fun secondsToMs(seconds: Double): Long {
        if (seconds.isNaN() || seconds.isInfinite() || seconds < 0.0) return -1L
        return (seconds * 1000.0).toLong()
    }
}
