package xyz.metiq.audio

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(markerClass = [UnstableApi::class])
class PlaybackService : MediaSessionService() {
    private lateinit var engine: AudioEngine
    private lateinit var player: EnginePlayer
    private var session: MediaSession? = null
    private lateinit var audioManager: AudioManager
    private lateinit var focusRequest: AudioFocusRequest
    private var focusHeld = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    inner class EngineBinder : Binder() {
        val engine: AudioEngine get() = this@PlaybackService.engine
        fun setActiveColor(label: String?, tintArgb: Int?) {
            this@PlaybackService.player.setActiveColor(label, tintArgb)
        }
        fun requestAudioFocusNow(): Boolean = this@PlaybackService.requestAudioFocus()
    }

    private val engineBinder = EngineBinder()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hardStop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.notifyPausedExternally()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_IDLE) {
                abandonAudioFocus()
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) requestAudioFocus()
        }
    }

    override fun onCreate() {
        super.onCreate()
        engine = AudioEngine(this)
        player = EnginePlayer(engine, mainLooper)
        player.addListener(playerListener)
        session = MediaSession.Builder(this, player).build()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()

        scope.launch {
            engine.preloadAll(PRELOAD_ASSETS)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == ENGINE_BIND_ACTION) return engineBinder
        return super.onBind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        hardStop()
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        abandonAudioFocus()
        session?.run {
            player.removeListener(playerListener)
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    private fun hardStop() {
        engine.release()
        player.setActiveColor(null, null)
        player.notifyStopped()
        abandonAudioFocus()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    private fun requestAudioFocus(): Boolean {
        if (focusHeld) return true
        val result = audioManager.requestAudioFocus(focusRequest)
        focusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return focusHeld
    }

    private fun abandonAudioFocus() {
        if (!focusHeld) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        focusHeld = false
    }

    companion object {
        const val ENGINE_BIND_ACTION = "xyz.metiq.audio.ENGINE"

        private val PRELOAD_ASSETS = mapOf(
            "pink" to "audio/pink.ogg",
            "brown" to "audio/brown.ogg",
            "white" to "audio/white.ogg",
            "grey" to "audio/grey.ogg",
        )
    }
}
