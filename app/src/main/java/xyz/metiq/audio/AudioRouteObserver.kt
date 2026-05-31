package xyz.metiq.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AudioRouteObserver(context: Context) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val flow: Flow<AudioRoute> = callbackFlow {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                trySend(currentRoute())
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                trySend(currentRoute())
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        trySend(currentRoute())
        awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    private fun currentRoute(): AudioRoute {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val ranked = devices.sortedBy { priority(it.type) }
        val pick = ranked.firstOrNull() ?: return AudioRoute(AudioRoute.Kind.BUILTIN, "Phone speaker")
        return AudioRoute(kindOf(pick.type), displayName(pick))
    }

    private fun priority(type: Int): Int = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> 0
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> 1
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> 2
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> 3
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> 9
        else -> 5
    }

    private fun kindOf(type: Int): AudioRoute.Kind = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioRoute.Kind.BLUETOOTH
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> AudioRoute.Kind.USB
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioRoute.Kind.WIRED
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> AudioRoute.Kind.HDMI
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> AudioRoute.Kind.BUILTIN
        else -> AudioRoute.Kind.OTHER
    }

    private fun displayName(info: AudioDeviceInfo): String {
        val product = info.productName?.toString()?.takeIf { it.isNotBlank() }
        if (product != null) return product
        return when (kindOf(info.type)) {
            AudioRoute.Kind.BLUETOOTH -> "Bluetooth"
            AudioRoute.Kind.USB -> "USB audio"
            AudioRoute.Kind.WIRED -> "Wired headphones"
            AudioRoute.Kind.HDMI -> "HDMI"
            AudioRoute.Kind.BUILTIN -> "Phone speaker"
            AudioRoute.Kind.OTHER -> "Other"
        }
    }
}

data class AudioRoute(val kind: Kind, val displayName: String) {
    enum class Kind { BLUETOOTH, USB, WIRED, HDMI, BUILTIN, OTHER }
}
