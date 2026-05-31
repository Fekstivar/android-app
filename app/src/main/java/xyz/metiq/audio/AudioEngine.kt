package xyz.metiq.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

class AudioEngine(private val context: Context) {
    private data class Layer(
        val track: AudioTrack,
        var volume: Float,
    )

    private data class Pcm(
        val data: ShortArray,
        val sampleRate: Int,
        val channelMask: Int,
        val channelCount: Int,
    )

    private val layers = ConcurrentHashMap<String, Layer>()
    private val pcmCache = ConcurrentHashMap<String, Pcm>()
    private var masterVolume: Float = 1f

    suspend fun preload(id: String, assetPath: String) {
        if (pcmCache.containsKey(id)) return
        val pcm = withContext(Dispatchers.IO) { decodeAssetToPcm(assetPath) }
        pcmCache.putIfAbsent(id, pcm)
    }

    suspend fun preloadAll(assets: Map<String, String>) = coroutineScope {
        assets.map { (id, path) -> async { preload(id, path) } }.awaitAll()
        Unit
    }

    suspend fun startLayer(id: String, assetPath: String, volume: Float = 1f) {
        if (layers.containsKey(id)) return
        val pcm = pcmCache[id] ?: withContext(Dispatchers.IO) {
            decodeAssetToPcm(assetPath).also { pcmCache.putIfAbsent(id, it) }
        }
        val effective = (volume * masterVolume).coerceIn(0f, 1f)
        val track = withContext(Dispatchers.IO) {
            val bufferBytes = pcm.data.size * 2
            val t = buildTrack(pcm.sampleRate, pcm.channelMask, bufferBytes)
            t.write(pcm.data, 0, pcm.data.size, AudioTrack.WRITE_BLOCKING)
            val frameCount = pcm.data.size / pcm.channelCount
            t.setLoopPoints(0, frameCount, -1)
            t.setVolume(effective)
            t.play()
            t
        }
        layers[id] = Layer(track, volume)
    }

    fun stopLayer(id: String) {
        layers.remove(id)?.track?.let { t ->
            t.stop()
            t.release()
        }
    }

    fun setLayerVolume(id: String, volume: Float) {
        val l = layers[id] ?: return
        l.volume = volume
        l.track.setVolume((volume * masterVolume).coerceIn(0f, 1f))
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        for ((_, l) in layers) {
            l.track.setVolume((l.volume * masterVolume).coerceIn(0f, 1f))
        }
    }

    fun pauseAll() {
        for ((_, l) in layers) l.track.pause()
    }

    fun resumeAll() {
        for ((_, l) in layers) l.track.play()
    }

    fun release() {
        for ((_, l) in layers) {
            runCatching { l.track.stop() }
            runCatching { l.track.release() }
        }
        layers.clear()
        pcmCache.clear()
    }

    fun activeLayerIds(): Set<String> = layers.keys.toSet()

    suspend fun crossfadeTo(
        newId: String,
        newAssetPath: String,
        fadeDurationMs: Long = CROSSFADE_MS,
    ) {
        val outgoing = layers.keys.filter { it != newId }
        val outgoingInitials = outgoing.associateWith { layers[it]?.volume ?: 0f }
        try {
            if (!layers.containsKey(newId)) startLayer(newId, newAssetPath, volume = 0f)
            val stepMs = (fadeDurationMs / FADE_STEPS).coerceAtLeast(1L)
            for (i in 1..FADE_STEPS) {
                val t = i.toFloat() / FADE_STEPS
                setLayerVolume(newId, t)
                outgoing.forEach { id ->
                    setLayerVolume(id, (1f - t) * (outgoingInitials[id] ?: 0f))
                }
                delay(stepMs)
            }
            setLayerVolume(newId, 1f)
        } finally {
            outgoing.forEach { stopLayer(it) }
        }
    }

    suspend fun fadeOutAllAndStop(fadeDurationMs: Long = CROSSFADE_MS) {
        val targets = layers.keys.toList()
        if (targets.isEmpty()) return
        val initials = targets.associateWith { layers[it]?.volume ?: 0f }
        try {
            val stepMs = (fadeDurationMs / FADE_STEPS).coerceAtLeast(1L)
            for (i in 1..FADE_STEPS) {
                val t = i.toFloat() / FADE_STEPS
                targets.forEach { id ->
                    setLayerVolume(id, (1f - t) * (initials[id] ?: 0f))
                }
                delay(stepMs)
            }
        } finally {
            targets.forEach { stopLayer(it) }
        }
    }

    private fun buildTrack(sampleRate: Int, channelMask: Int, bufferBytes: Int): AudioTrack {
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate).setChannelMask(channelMask).build()
        return AudioTrack.Builder().setAudioAttributes(attrs).setAudioFormat(format)
            .setBufferSizeInBytes(bufferBytes).setTransferMode(AudioTrack.MODE_STATIC).build()
    }

    private fun decodeAssetToPcm(assetPath: String): Pcm {
        val afd: AssetFileDescriptor = context.assets.openFd(assetPath)
        val extractor = MediaExtractor()
        extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()

        val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        } ?: error("No audio track in $assetPath")
        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
        val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(inputFormat, null, null, 0)
        codec.start()

        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        val pcmBytes = java.io.ByteArrayOutputStream()

        while (!sawOutputEos) {
            if (!sawInputEos) {
                val inIx = codec.dequeueInputBuffer(10_000)
                if (inIx >= 0) {
                    val inBuf: ByteBuffer = codec.getInputBuffer(inIx)!!
                    val read = extractor.readSampleData(inBuf, 0)
                    if (read < 0) {
                        codec.queueInputBuffer(inIx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEos = true
                    } else {
                        codec.queueInputBuffer(inIx, 0, read, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIx = codec.dequeueOutputBuffer(info, 10_000)
            if (outIx >= 0) {
                val outBuf: ByteBuffer = codec.getOutputBuffer(outIx)!!
                outBuf.position(info.offset)
                outBuf.limit(info.offset + info.size)
                val chunk = ByteArray(info.size)
                outBuf.get(chunk)
                pcmBytes.write(chunk)
                codec.releaseOutputBuffer(outIx, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEos = true
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        val bytes = pcmBytes.toByteArray()
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

        val channelMask = when (channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> error("Unsupported channel count: $channelCount")
        }
        return Pcm(
            data = shorts,
            sampleRate = sampleRate,
            channelMask = channelMask,
            channelCount = channelCount,
        )
    }

    companion object {
        const val CROSSFADE_MS: Long = 500L
        private const val FADE_STEPS = 20
    }
}
