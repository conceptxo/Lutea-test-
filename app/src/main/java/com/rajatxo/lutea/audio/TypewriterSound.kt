package com.rajatxo.lutea.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.rajatxo.lutea.R
import kotlin.random.Random

/**
 * The small mechanical key clicks used by Lutea's typewriter-style text fields.
 *
 * The samples are Kenney's UI Audio clicks (CC0). They are loaded once with the
 * app and played on the sonification stream, so typing stays separate from the
 * music stream and follows the phone's UI-sound volume.
 */
object TypewriterSound {
    private const val VOLUME = 0.42f

    private var soundPool: SoundPool? = null
    private var soundIds: IntArray = IntArray(0)
    private var nextSound = 0

    fun init(context: Context) {
        if (soundPool != null) return

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
     */
    fun playKey() {
        val pool = soundPool ?: return
        if (soundIds.isEmpty()) return

        val index = nextSound % soundIds.size
        nextSound = (nextSound + 1) % soundIds.size

        // Tiny pitch/rate differences keep a fast sentence from sounding like
        // the exact same sample on repeat.
        val rate = 0.97f + Random.nextFloat() * 0.06f
        try {
            pool.play(soundIds[index], VOLUME, VOLUME, 1, 0, rate)
        } catch (_: Exception) {
            // A missing/unloaded sound must never interrupt typing.
        }
    }
}
