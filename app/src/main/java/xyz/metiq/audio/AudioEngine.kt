package xyz.metiq.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// Every volume ramp (start, stop, pause, resume) uses this duration, applied in
// FADE_STEP_MS-sized setVolume steps. Stopping keeps the track alive until its
// fade-out finishes, which makes color switches crossfade.
private const val FADE_MS = 400L
private const val FADE_STEP_MS = 20L

// Noise layers are streamed block-by-block through a live low-pass so Warmth can be
// applied on top of continuous playback (no track rebuild / restart).
private const val NOISE_BLOCK_FRAMES = 1024
// Per-block glide of the filter cutoff toward the target warmth — smooths coefficient
// steps so dragging the slider sweeps the tone instead of clicking between values.
private const val WARMTH_GLIDE = 0.2f

@OptIn(ExperimentalCoroutinesApi::class)
class AudioEngine(private val context: Context) {
    private data class Layer(
        val track: AudioTrack,
        var volume: Float,
        val warmthEligible: Boolean,
        // Non-null for streamed noise layers: the coroutine feeding + filtering the
        // track. It owns the track's teardown, so stopping just cancels this job.
        val feeder: Job? = null,
    ) {
        // 0..1 start/stop ramp multiplied on top of the configured volume, so a
        // stop mid-fade-in ramps down from wherever the fade-in got to.
        @Volatile
        var fadeFactor: Float = 1f
        var fadeJob: Job? = null
    }

    private val layers = ConcurrentHashMap<String, Layer>()

    private val feederScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var masterVolume: Float = 1f

    // Warmth 0f = off (bright); 1f = warmest. Read live by each noise feeder every
    // block, so a change takes effect on the next ~23 ms without touching the track.
    @Volatile
    private var warmth: Float = 0f

    // Single-threaded so a pause fade and a subsequent resume can never
    // write track volumes concurrently.
    private val fadeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var fadeJob: Job? = null

    // Human loudness perception is logarithmic: a linear amplitude ramp sounds
    // unchanged for most of its run, then falls off a cliff at the end. Map linear
    // fade progress to a dB-linear gain (60 dB range) so fades sound even.
    private fun fadeGain(progress: Float): Float =
        if (progress <= 0f) 0f else 10f.pow(-3f * (1f - progress.coerceAtMost(1f)))

    private fun appliedVolume(l: Layer): Float =
        (l.volume * masterVolume * fadeGain(l.fadeFactor)).coerceIn(0f, 1f)

    suspend fun startLayer(
        id: String,
        assetPath: String,
        volume: Float = 1f,
        warmthEligible: Boolean = false,
    ) {
        if (layers.containsKey(id)) return
        val pcm = PcmStore.awaitPcm(context, id, assetPath)
        val layer = if (warmthEligible) {
            // Streamed: a feeder loops the raw PCM through a live low-pass driven by
            // `warmth`, so the tone changes without ever restarting the sound.
            val track = withContext(Dispatchers.IO) { buildStreamTrack(pcm, 0f) }
            val feeder = launchNoiseFeeder(track, pcm)
            Layer(track, volume, warmthEligible = true, feeder = feeder)
        } else {
            // Ambient: hardware-looped static track, unaffected by Warmth.
            val track = withContext(Dispatchers.IO) {
                buildLoopingTrack(pcm, pcm.data, 0f)
            }
            Layer(track, volume, warmthEligible = false)
        }
        layer.fadeFactor = 0f
        layers[id] = layer
        rampLayer(layer, target = 1f, durationMs = FADE_MS)
    }

    // Ramps the layer's fade factor from wherever it currently is to `target`,
    // reapplying the track volume each step.
    private fun rampLayer(l: Layer, target: Float, durationMs: Long): Job {
        l.fadeJob?.cancel()
        val job = fadeScope.launch {
            val from = l.fadeFactor
            val steps = (durationMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                l.fadeFactor = from + (target - from) * i / steps
                runCatching { l.track.setVolume(appliedVolume(l)) }
                delay(FADE_STEP_MS)
            }
        }
        l.fadeJob = job
        return job
    }

    // Maps 0..1 warmth to the low-pass corner. w=0 sits well above the audible band
    // (effectively bright/bypass); w=1 stops at ~2.2 kHz so the four colors stay
    // distinguishable, never fully muffled.
    private fun warmthCutoffHz(w: Float): Float {
        val fMax = 18000.0
        val fMin = 2200.0
        return (fMax * (fMin / fMax).pow(w.coerceIn(0f, 1f).toDouble())).toFloat()
    }

    private fun buildLoopingTrack(pcm: Pcm, data: ShortArray, effective: Float): AudioTrack {
        val bufferBytes = data.size * 2
        val t = buildTrack(pcm.sampleRate, pcm.channelMask, bufferBytes)
        t.write(data, 0, data.size, AudioTrack.WRITE_BLOCKING)
        val frameCount = data.size / pcm.channelCount
        t.setLoopPoints(0, frameCount, -1)
        t.setVolume(effective)
        t.play()
        return t
    }

    private fun buildStreamTrack(pcm: Pcm, effective: Float): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            pcm.sampleRate, pcm.channelMask, AudioFormat.ENCODING_PCM_16BIT,
        )
        // A few blocks of headroom so the feeder never starves during a filter pass.
        val desired = NOISE_BLOCK_FRAMES * pcm.channelCount * 2 * 4
        val t = buildTrack(pcm.sampleRate, pcm.channelMask, maxOf(minBuf, desired), stream = true)
        t.setVolume(effective)
        return t
    }

    // Streams the raw loop through a 2nd-order (Butterworth-Q) low-pass whose cutoff
    // glides toward the live `warmth` each block. Continuous playback, filtered on top —
    // changing warmth never restarts the sound. The feeder owns the track's teardown.
    private fun launchNoiseFeeder(track: AudioTrack, pcm: Pcm): Job =
        feederScope.launch {
            val src = pcm.data
            val ch = pcm.channelCount
            val frames = src.size / ch
            if (frames == 0) {
                runCatching { track.release() }
                return@launch
            }
            val sr = pcm.sampleRate
            val fcMax = (sr * 0.45f) // keep the cutoff safely below Nyquist
            val block = NOISE_BLOCK_FRAMES
            val total = block * ch
            val buf = ShortArray(total)
            // Per-channel biquad state (Direct Form I).
            val x1 = DoubleArray(ch); val x2 = DoubleArray(ch)
            val y1 = DoubleArray(ch); val y2 = DoubleArray(ch)
            // Start already at the target so opening at warmth>0 doesn't sweep in.
            var fc = warmthCutoffHz(warmth).coerceIn(2200f, fcMax)
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0
            fun recompute(f: Double) {
                val w0 = 2.0 * PI * f / sr
                val cw = cos(w0)
                val alpha = sin(w0) / (2.0 * 0.70710678)
                val a0 = 1.0 + alpha
                b0 = ((1.0 - cw) / 2.0) / a0
                b1 = (1.0 - cw) / a0
                b2 = b0
                a1 = (-2.0 * cw) / a0
                a2 = (1.0 - alpha) / a0
            }
            var pos = 0
            try {
                track.play()
                while (isActive) {
                    val target = warmthCutoffHz(warmth).coerceIn(2200f, fcMax)
                    fc += (target - fc) * WARMTH_GLIDE
                    recompute(fc.toDouble())
                    for (f in 0 until block) {
                        val base = pos * ch
                        for (c in 0 until ch) {
                            val x0 = src[base + c].toDouble()
                            val y0 = b0 * x0 + b1 * x1[c] + b2 * x2[c] - a1 * y1[c] - a2 * y2[c]
                            x2[c] = x1[c]; x1[c] = x0; y2[c] = y1[c]; y1[c] = y0
                            buf[f * ch + c] = y0.coerceIn(-32768.0, 32767.0).toInt().toShort()
                        }
                        pos++
                        if (pos >= frames) pos = 0
                    }
                    var off = 0
                    while (off < total && isActive) {
                        val n = track.write(buf, off, total - off, AudioTrack.WRITE_NON_BLOCKING)
                        if (n < 0) return@launch
                        off += n
                        // Buffer full (or track paused): yield instead of spinning. delay
                        // is a cooperative cancellation point, so stop takes effect here.
                        if (off < total) delay(5)
                    }
                }
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }

    // Warmth is read live by each noise feeder, so this just updates the value; no
    // track is rebuilt and playback is never interrupted.
    fun setWarmth(value: Float) {
        warmth = value.coerceIn(0f, 1f)
    }

    fun stopLayer(id: String) {
        // Removed from the map immediately so a quick re-tap starts a fresh layer;
        // this one fades out detached and tears itself down at the end.
        val l = layers.remove(id) ?: return
        l.fadeJob?.cancel()
        fadeScope.launch {
            val from = l.fadeFactor
            val steps = (FADE_MS / FADE_STEP_MS).toInt()
            for (i in 1..steps) {
                l.fadeFactor = from * (1f - i.toFloat() / steps)
                runCatching { l.track.setVolume(appliedVolume(l)) }
                delay(FADE_STEP_MS)
            }
            if (l.feeder != null) {
                // The feeder's finally block stops + releases the track once it unwinds.
                l.feeder.cancel()
            } else {
                runCatching { l.track.stop() }
                runCatching { l.track.release() }
            }
        }
    }

    fun setLayerVolume(id: String, volume: Float) {
        val l = layers[id] ?: return
        l.volume = volume
        l.track.setVolume(appliedVolume(l))
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        for ((_, l) in layers) {
            l.track.setVolume(appliedVolume(l))
        }
    }

    fun pauseAll(fade: Boolean = true) {
        fadeJob?.cancel()
        if (!fade) {
            for ((_, l) in layers) runCatching { l.track.pause() }
            return
        }
        fadeJob = fadeScope.launch {
            val steps = (FADE_MS / FADE_STEP_MS).toInt()
            for (i in 1..steps) {
                val factor = 1f - i.toFloat() / steps
                for ((_, l) in layers) {
                    runCatching { l.track.setVolume(appliedVolume(l) * fadeGain(factor)) }
                }
                delay(FADE_STEP_MS)
            }
            for ((_, l) in layers) {
                runCatching { l.track.pause() }
                // Restore so a later resume plays at the configured level.
                runCatching { l.track.setVolume(appliedVolume(l)) }
            }
        }
    }

    fun resumeAll() {
        fadeJob?.cancel()
        fadeJob = fadeScope.launch {
            for ((_, l) in layers) {
                runCatching {
                    l.track.setVolume(0f)
                    l.track.play()
                }
            }
            val steps = (FADE_MS / FADE_STEP_MS).toInt()
            for (i in 1..steps) {
                val factor = i.toFloat() / steps
                for ((_, l) in layers) {
                    runCatching { l.track.setVolume(appliedVolume(l) * fadeGain(factor)) }
                }
                delay(FADE_STEP_MS)
            }
        }
    }

    // Also reached via hardStop() while the engine object stays in use, so only
    // cancel the in-flight jobs — never the scopes.
    fun release() {
        fadeJob?.cancel()
        for ((_, l) in layers) {
            l.fadeJob?.cancel()
            if (l.feeder != null) {
                l.feeder.cancel()
            } else {
                runCatching { l.track.stop() }
                runCatching { l.track.release() }
            }
        }
        layers.clear()
    }

    fun activeLayerIds(): Set<String> = layers.keys.toSet()

    fun layerVolume(id: String): Float? = layers[id]?.volume

    suspend fun switchTo(newId: String, newAssetPath: String) {
        layers.keys.filter { it != newId }.forEach { stopLayer(it) }
        if (!layers.containsKey(newId)) {
            startLayer(newId, newAssetPath, volume = 1f, warmthEligible = true)
        } else {
            setLayerVolume(newId, 1f)
        }
    }

    fun stopAll() {
        layers.keys.toList().forEach { stopLayer(it) }
    }

    private fun buildTrack(
        sampleRate: Int,
        channelMask: Int,
        bufferBytes: Int,
        stream: Boolean = false,
    ): AudioTrack {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate).setChannelMask(channelMask).build()
        val mode = if (stream) AudioTrack.MODE_STREAM else AudioTrack.MODE_STATIC
        return AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(format)
            .setBufferSizeInBytes(bufferBytes).setTransferMode(mode).build()
    }

}
