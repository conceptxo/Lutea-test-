package com.rajatxo.lutea.data.store

import android.content.Context
import com.rajatxo.lutea.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * SongEditStore — per-song user overrides that layer ON TOP of the device's
 * MediaStore metadata. We never rewrite the real audio file's tags (that needs
 * SAF write + per-format tag libraries and is fragile). Instead the app keeps an
 * editable copy: a custom title / artist / album, a custom cover image the user
 * picked, and imported lyrics text. The UI merges these overrides whenever it
 * shows or plays a song.
 *
 * Mochy-inspired edit feature, implemented from scratch for Lutea.
 */
object SongEditStore {

    @Serializable
    data class SongOverride(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        /** A content:// or file:// URI the user picked as a custom cover. */
        val coverUri: String? = null,
        /** Lyrics text (plain or LRC-timed). null = no lyrics set. */
        val lyrics: String? = null,
        /** Detected/declared lyrics format label, e.g. "LRC", "Plain". */
        val lyricsFormat: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private lateinit var appContext: Context
    private val file get() = java.io.File(appContext.filesDir, "song_overrides.json")

    private val _overrides = MutableStateFlow<Map<Long, SongOverride>>(emptyMap())
    val overrides: StateFlow<Map<Long, SongOverride>> = _overrides.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        load()
    }

    private fun load() {
        _overrides.value = try {
            if (file.exists()) {
                json.decodeFromString(
                    MapSerializer(LongSerializer, serializer<SongOverride>()),
                    file.readText(),
                )
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun persist() {
        try {
            file.writeText(
                json.encodeToString(
                    MapSerializer(LongSerializer, serializer<SongOverride>()),
                    _overrides.value,
                ),
            )
        } catch (e: Exception) { /* silent fail */ }
    }

    fun overrideFor(songId: Long): SongOverride = _overrides.value[songId] ?: SongOverride()

    /** Apply any overrides to a song, returning a new Song with edited fields. */
    fun applyTo(song: Song): Song {
        val o = _overrides.value[song.id] ?: return song
        return song.copy(
            title = o.title?.takeIf { it.isNotBlank() } ?: song.title,
            artist = o.artist?.takeIf { it.isNotBlank() } ?: song.artist,
            album = o.album?.takeIf { it.isNotBlank() } ?: song.album,
        )
    }

    /** The cover URI to display: the override if set, else the song's own art. */
    fun coverUriFor(song: Song): String? =
        _overrides.value[song.id]?.coverUri ?: song.albumArtUri?.toString()

    fun lyricsFor(songId: Long): Pair<String, String?>? {
        val o = _overrides.value[songId] ?: return null
        val l = o.lyrics ?: return null
        return l to o.lyricsFormat
    }

    fun save(
        songId: Long,
        title: String?,
        artist: String?,
        album: String?,
        coverUri: String?,
        lyrics: String?,
        lyricsFormat: String?,
    ) {
        val current = _overrides.value[songId] ?: SongOverride()
        // Drop an override entirely if it matches the original (keep the file lean).
        _overrides.value = _overrides.value + (songId to current.copy(
            title = title?.takeIf { it.isNotBlank() },
            artist = artist?.takeIf { it.isNotBlank() },
            album = album?.takeIf { it.isNotBlank() },
            coverUri = coverUri,
            lyrics = lyrics?.takeIf { it.isNotBlank() },
            lyricsFormat = lyricsFormat,
        ))
        persist()
    }

    fun clearCover(songId: Long) {
        val current = _overrides.value[songId] ?: return
        _overrides.value = _overrides.value + (songId to current.copy(coverUri = null))
        persist()
    }
}
