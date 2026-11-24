package com.rajatxo.lutea.data.store

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PlaybackStatsStore — accumulates real listened time per song, in
 * milliseconds, persisted to internal storage. Only time actually spent
 * playing (not paused / seeking) counts, in increments flushed by the
 * playback poll loop.
 *
 * Used by the song-list mini-cover badge ("1m / 1h / 12h / 1d") and by the
 * "Listening time" sort mode. Mirrors RiMusic's per-song play-time tracking.
 */
object PlaybackStatsStore {

    private lateinit var appContext: Context
    private val file get() = java.io.File(appContext.filesDir, "playback_stats.txt")

    private val _stats = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val stats: StateFlow<Map<Long, Long>> = _stats.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        load()
    }

    private fun load() {
        _stats.value = try {
            if (file.exists()) {
                file.readLines()
                    .mapNotNull { line ->
                        val parts = line.split('\t')
                        val id = parts.getOrNull(0)?.trim()?.toLongOrNull() ?: return@mapNotNull null
                        val ms = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: return@mapNotNull null
                        id to ms
                    }
                    .toMap()
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun persist() {
        try {
            val text = _stats.value.entries.joinToString("\n") { "${it.key}\t${it.value}" }
            file.writeText(text)
        } catch (_: Exception) { /* best-effort persistence */ }
    }

    /** Add `deltaMs` of played time to [songId] and persist. Clamped at >= 0. */
    fun addPlayedMs(songId: Long, deltaMs: Long) {
        if (deltaMs <= 0L) return
        val current = _stats.value
        val updated = current[songId]?.let { it + deltaMs } ?: deltaMs
        _stats.value = current + (songId to updated)
        persist()
    }

    fun playedMsFor(songId: Long): Long = _stats.value[songId] ?: 0L
}

/**
 * Human-readable short form for a played-time badge: the largest unit that
 * produces a value >= 1, e.g. "1m", "12h", "1d". Values under a minute round
 * to "0m" so the badge still renders for songs that were just started.
 *
 * Thresholds follow RiMusic: m → h → d (no weeks/months — a day is the
 * coarsest unit shown on a thumbnail).
 */
fun formatPlayedMs(ms: Long): String {
    val seconds = ms / 1000
    if (seconds < 60) return "0m"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h"
    val days = hours / 24
    return "${days}d"
}
