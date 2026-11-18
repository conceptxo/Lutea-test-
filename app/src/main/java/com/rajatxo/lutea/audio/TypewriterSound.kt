package com.rajatxo.lutea.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import com.rajatxo.lutea.R
import kotlin.random.Random

/**
 * The small mechanical key clicks used by Lutea's typewriter-style text fields.
 *
 * The samples are sharp mechanical key-strikes extracted from a single
 * BigSoundBank typewriter recording. They are loaded once with the app.
 *
 * ROUTING — the pool uses USAGE_MEDIA / CONTENT_TYPE_MUSIC so the clicks
 * follow the **media** volume (not the system/UI volume) and route through
 * A2DP on Bluetooth devices. A short click loses a lot of energy through an
 * A2DP sink, so Bluetooth gets a fixed loudness compensation while the phone
 * speaker is left at unity. The user volume (Sound section) then trims both
 * routes together, which keeps the speaker and Bluetooth much closer.
 *
 * DEBOUNCE — IME composing / autocorrect can fire onValueChange in a tight
 * burst (e.g. accepting a suggestion inserts 2+ chars in one batch, but the
 * IME may then send a follow-up change a few ms later). [playKey] drops any
 * call that arrives within [MIN_INTERVAL_MS] of the previous one, so every
 * real keypress sounds like exactly one click — never two.
 */
object TypewriterSound {
    /** Default user trim (0..1) shown by the Sound section. */
    const val DEFAULT_LEVEL = 0.30f

    // Full-scale output at a 100% user setting. A 30% default therefore plays
    // at 0.18 — 40% quieter than the previous fixed 0.30.
    private const val MAX_GAIN = 0.60f

    // The A2DP sink attenuates a short transient more than the phone speaker.
    // This is a route compensation, not a second volume control: the user trim
    // still scales both outputs together.
    private const val SPEAKER_GAIN = 1.00f
    private const val BLUETOOTH_GAIN = 2.80f

    // Minimum gap between two plays. Below this, the second call is dropped.
    // 70ms is short enough that fast touch-typing still sounds natural, but
    // long enough to swallow the double-fire from IME composing events.
    private const val MIN_INTERVAL_MS = 70L

    // Route checks are cheap enough, but no need to do one per keypress.
    private const val ROUTE_CACHE_MS = 1_500L

    private var soundPool: SoundPool? = null
    private var soundIds: IntArray = IntArray(0)
    private var nextSound = 0
    private var lastPlayTime = 0L

    private var appContext: Context? = null
    private var cachedBluetooth = false
    private var lastRouteCheck = 0L
    private var level = DEFAULT_LEVEL

    fun init(context: Context) {
        appContext = context.applicationContext
        if (soundPool != null) return

        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // Media stream: BT headphones route it over A2DP; the
                    // phone's media volume slider still controls the click.
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()

        soundPool = pool
        soundIds = intArrayOf(
            pool.load(context.applicationContext, R.raw.typewriter_key_1, 1),
            pool.load(context.applicationContext, R.raw.typewriter_key_2, 1),
            pool.load(context.applicationContext, R.raw.typewriter_key_3, 1),
        )
    }

    /** User trim from the Sound section. 0 = silent, 1 = full scale. */
    fun setLevel(level: Float) {
        this.level = level.coerceIn(0f, 1f)
    }

    /**
     * Plays one key click. Call this once per text-insertion event, not once per
     * character: pasting a whole word should sound like one key, not a burst.
     *
     * Returns true if the sound actually played, false if it was debounced.
     */
    fun playKey(): Boolean {
        val pool = soundPool ?: return false
        if (soundIds.isEmpty()) return false

        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayTime < MIN_INTERVAL_MS) return false
        lastPlayTime = now

        val index = nextSound % soundIds.size
        nextSound = (nextSound + 1) % soundIds.size

        // Tiny pitch/rate differences keep a fast sentence from sounding like
        // the exact same sample on repeat.
        val rate = 0.97f + Random.nextFloat() * 0.06f
        val gain = (level * MAX_GAIN * routeGain()).coerceIn(0f, 1f)
        try {
            pool.play(soundIds[index], gain, gain, 1, 0, rate)
        } catch (_: Exception) {
            // A missing/unloaded sound must never interrupt typing.
            return false
        }
        return true
    }

    /** Speaker/Bluetooth trim, cached briefly so route checks don't run per key. */
    private fun routeGain(): Float {
        val context = appContext ?: return SPEAKER_GAIN
        val now = SystemClock.elapsedRealtime()
        if (now - lastRouteCheck >= ROUTE_CACHE_MS) {
            cachedBluetooth = isBluetoothOutput(context)
            lastRouteCheck = now
        }
        return if (cachedBluetooth) BLUETOOTH_GAIN else SPEAKER_GAIN
    }

    /**
     * Media output is the phone speaker or an A2DP/BLE sink. Some OEMs report
     * the deprecated A2DP flag late, so connected Bluetooth devices are also
     * treated as a Bluetooth route.
     */
    @Suppress("DEPRECATION")
    private fun isBluetoothOutput(context: Context): Boolean {
        return runCatching {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return@runCatching false
            if (manager.isBluetoothA2dpOn || manager.isBluetoothScoOn) {
                return@runCatching true
            }
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true

                    else -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                }
            }
        }.getOrDefault(false)
    }

    /** Stops any in-flight click immediately (used when the field loses focus). */
    fun stopAll() {
        val pool = soundPool ?: return
        soundIds.forEach { id ->
            try { pool.stop(id) } catch (_: Exception) { /* ignore */ }
        }
    }
}
