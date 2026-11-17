package com.rajatxo.lutea.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
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
 * A2DP at full strength on Bluetooth devices. The previous sonification
 * attribute made the clicks nearly inaudible on neckbands/BT headphones
 * because those devices aggressively attenuate the UI sonification stream.
 *
 * DEBOUNCE — IME composing / autocorrect can fire onValueChange in a tight
 * burst (e.g. accepting a suggestion inserts 2+ chars in one batch, but the
 * IME may then send a follow-up change a few ms later). [playKey] drops any
 * call that arrives within [MIN_INTERVAL_MS] of the previous one, so every
 * real keypress sounds like exactly one click — never two.
 */
object TypewriterSound {
    // Reduced from 0.42 → 0.30: the new samples are louder (extracted at +2.2×)
    // and shorter, so 0.30 reads as a clean, soft click instead of a thump.
    private const val VOLUME = 0.30f

    // Minimum gap between two plays. Below this, the second call is dropped.
    // 70ms is short enough that fast touch-typing still sounds natural, but
    // long enough to swallow the double-fire from IME composing events.
    private const val MIN_INTERVAL_MS = 70L

    private var soundPool: SoundPool? = null
    private var soundIds: IntArray = IntArray(0)
    private var nextSound = 0
    private var lastPlayTime = 0L

    fun init(context: Context) {
        if (soundPool != null) return

        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // Media stream: BT headphones route it over A2DP at full
                    // volume; the phone's media volume slider controls it.
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

    /**
     * Plays one key click. Call this once per text-insertion event, not once per
     * character: pasting a whole word should sound like one key, not a burst.
     *
     * Returns true if the sound actually played, false if it was debounced.
     */
    fun playKey(): Boolean {
        val pool = soundPool ?: return false
        if (soundIds.isEmpty()) return false

        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastPlayTime < MIN_INTERVAL_MS) return false
        lastPlayTime = now

        val index = nextSound % soundIds.size
        nextSound = (nextSound + 1) % soundIds.size

        // Tiny pitch/rate differences keep a fast sentence from sounding like
        // the exact same sample on repeat.
        val rate = 0.97f + Random.nextFloat() * 0.06f
        try {
            pool.play(soundIds[index], VOLUME, VOLUME, 1, 0, rate)
        } catch (_: Exception) {
            // A missing/unloaded sound must never interrupt typing.
            return false
        }
        return true
    }

    /** Stops any in-flight click immediately (used when the field loses focus). */
    fun stopAll() {
        val pool = soundPool ?: return
        soundIds.forEach { id ->
            try { pool.stop(id) } catch (_: Exception) { /* ignore */ }
        }
    }
}
