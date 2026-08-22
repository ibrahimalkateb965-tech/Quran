package com.example.service

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity

class QuranAudioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var cache: SimpleCache
    private var audioManager: AudioManager? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        cache = getCache(this)

        val upstreamFactory = DefaultDataSource.Factory(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady) {
                    abandonSystemAudioFocus()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                    abandonSystemAudioFocus()
                }
            }
        })

        val sessionActivityPendingIntent = TaskStackBuilder.create(this).run {
            addNextIntent(Intent(this@QuranAudioService, MainActivity::class.java))
            getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val isSelf = controller.packageName == packageName
                val isSystemOrTrusted = controller.packageName == "com.android.systemui" ||
                        controller.packageName == "com.google.android.projection.gearhead" ||
                        controller.packageName == "com.google.android.googlequicksearchbox" ||
                        session.isMediaNotificationController(controller)

                if (isSelf || isSystemOrTrusted) {
                    return super.onConnect(session, controller)
                }
                return MediaSession.ConnectionResult.reject()
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }

                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.keyCode
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                        // If player is explicitly paused/stopped, prevent unsolicited background resume (e.g. from telecom/headset disconnect)
                        if (!player.playWhenReady) {
                            return true // Consume event safely without starting playback
                        }
                    }
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(sessionCallback)
            .build()
    }

    private fun abandonSystemAudioFocus() {
        try {
            audioManager?.abandonAudioFocus(null)
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = mediaSession?.player ?: return
        if (!currentPlayer.playWhenReady || currentPlayer.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        private const val CACHE_MAX_BYTES = 200L * 1024 * 1024 // 200 MB

        @Volatile
        private var cacheInstance: SimpleCache? = null

        @OptIn(UnstableApi::class)
        fun getCache(context: Context): SimpleCache {
            return cacheInstance ?: synchronized(this) {
                cacheInstance ?: run {
                    val cacheDir = context.cacheDir.resolve("mueen_audio_cache").also { it.mkdirs() }
                    val evictor = LeastRecentlyUsedCacheEvictor(CACHE_MAX_BYTES)
                    val databaseProvider = StandaloneDatabaseProvider(context)
                    SimpleCache(cacheDir, evictor, databaseProvider).also { cacheInstance = it }
                }
            }
        }

    }
}
