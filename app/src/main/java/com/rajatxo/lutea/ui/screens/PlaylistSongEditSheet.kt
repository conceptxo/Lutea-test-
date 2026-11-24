package com.rajatxo.lutea.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.lutea.data.store.SongEditStore
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.ui.components.CalSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PlaylistSongEditSheet — the per-song editor reached from the song menu's
 * "Edit" option. Mochy-inspired field set, built for Lutea's dark glass UI:
 *
 *   - Cover: shows the current art with a "Change cover" button that launches
 *     the system image picker; a "Use original" button clears a custom pick.
 *   - Song name (title) — pre-filled from the song / existing override.
 *   - Artist — pre-filled.
 *   - Album — pre-filled.
 *   - Lyrics: import a file (LRC / TTML / SRT / VTT / KRC / YRC / QRC / ASS /
 *     plain — auto-detected). The detected format + line count is shown; a
 *     "Remove lyrics" button clears it. The text is normalized to LRC form.
 *
 * Everything is saved to [SongEditStore] as per-song overrides layered over the
 * device's real metadata; the audio file's tags are never touched. The sheet is
 * defensive: a failure to read a cover/lyrics file is caught and shown inline,
 * never crashing the app.
 */
@Composable
fun PlaylistSongEditSheet(
    song: Song,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val existing = remember(song.id) { SongEditStore.overrideFor(song.id) }

    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var coverUri by remember(song.id) {
        mutableStateOf(existing.coverUri ?: song.albumArtUri?.toString())
    }
    var usingCustomCover by remember(song.id) { mutableStateOf(existing.coverUri != null) }
    var lyrics by remember(song.id) { mutableStateOf(existing.lyrics ?: "") }
    var lyricsFormat by remember(song.id) { mutableStateOf(existing.lyricsFormat) }
    var lyricsLineCount by remember(song.id) {
        mutableStateOf(existing.lyrics?.count { it == '\n' }?.plus(1) ?: 0)
    }
    var lyricsError by remember { mutableStateOf<String?>(null) }
    // The lyrics file read happens in a LaunchedEffect keyed off this URI.
    var pendingLyricsUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker for a custom cover.
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            coverUri = uri.toString()
            usingCustomCover = true
        }
    }
    // Document picker for a lyrics file (any text-ish MIME).
    val lyricsPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            lyricsError = null
            // The actual file read happens in a LaunchedEffect keyed off this URI
            // (it needs a coroutine scope + must run off the main thread).
            pendingLyricsUri = uri
        }
    }
    LaunchedEffect(pendingLyricsUri) {
        val u = pendingLyricsUri ?: return@LaunchedEffect
        try {
            val raw = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(u)?.bufferedReader()?.use { it.readText() }
            }
            if (raw == null) {
                lyricsError = "Couldn't read that file."
            } else if (raw.toByteArray().size > 1_000_000) {
                lyricsError = "Lyrics file is too large (max 1 MB)."
            } else {
                val format = LyricsFormat.detect(raw)
                val normalized = LyricsFormat.normalize(raw)
                lyrics = normalized
                lyricsFormat = format
                lyricsLineCount = normalized.count { it == '\n' }.plus(1)
            }
        } catch (e: Exception) {
            lyricsError = "Couldn't read that file."
        }
        pendingLyricsUri = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A1A), Color(0xFF000000)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 120.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Edit song",
                    color = Color.White,
                    fontFamily = CalSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
            }

            Spacer(Modifier.height(20.dp))

            // ── Cover ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val coverModel: Any? = coverUri?.let { Uri.parse(it) }
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // A simple note glyph so the placeholder is non-empty.
                        Text("♪", color = Color.White.copy(alpha = 0.35f), fontSize = 48.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                EditChipButton("Change cover") {
                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                Spacer(Modifier.width(10.dp))
                if (usingCustomCover) {
                    EditChipButton("Use original") {
                        coverUri = song.albumArtUri?.toString()
                        usingCustomCover = false
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Song name ───────────────────────────────────────────────────
            EditFieldLabel("Song name")
            EditTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Song name",
            )

            Spacer(Modifier.height(18.dp))

            // ── Artist ──────────────────────────────────────────────────────
            EditFieldLabel("Artist")
            EditTextField(
                value = artist,
                onValueChange = { artist = it },
                placeholder = "Artist",
            )

            Spacer(Modifier.height(18.dp))

            // ── Album ───────────────────────────────────────────────────────
            EditFieldLabel("Album")
            EditTextField(
                value = album,
                onValueChange = { album = it },
                placeholder = "Album",
            )

            Spacer(Modifier.height(28.dp))

            // ── Lyrics ──────────────────────────────────────────────────────
            EditFieldLabel("Lyrics")
            Text(
                "Import a lyrics file. LRC, TTML, SRT, VTT, KRC, YRC, QRC, ASS and plain text are auto-detected.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                fontFamily = CalSansFamily,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            // Dashed import box.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { lyricsPicker.launch(arrayOf("text/*", "application/octet-stream", "application/xml", "application/ttml+xml", "application/x-subrip")) }
                    .padding(18.dp),
            ) {
                if (lyrics.isBlank()) {
                    Text(
                        "Tap to choose a lyrics file",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontFamily = CalSansFamily,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${lyricsFormat ?: "Plain"} · $lyricsLineCount lines",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = CalSansFamily,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "Tap to replace",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 12.sp,
                                fontFamily = CalSansFamily,
                            )
                        }
                        // Remove lyrics.
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    lyrics = ""
                                    lyricsFormat = null
                                    lyricsLineCount = 0
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("×", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            lyricsError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(err, color = Color(0xFFFF6B6B), fontSize = 12.sp, fontFamily = CalSansFamily)
            }

            Spacer(Modifier.height(20.dp))
            // A small preview of the imported lyrics (first ~6 lines).
            if (lyrics.isNotBlank()) {
                Text(
                    "Preview",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = CalSansFamily,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    lyrics.lines().take(6).joinToString("\n"),
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontFamily = CalSansFamily,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(12.dp),
                )
            }
        }

        // ── Save bar (pinned bottom) ────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
            Spacer(Modifier.width(8.dp))
            val canSave = title.isNotBlank()
            TextButton(
                onClick = {
                    SongEditStore.save(
                        songId = song.id,
                        title = title.trim(),
                        artist = artist.trim(),
                        album = album.trim(),
                        coverUri = if (usingCustomCover) coverUri else null,
                        lyrics = lyrics.ifBlank { null },
                        lyricsFormat = lyricsFormat,
                    )
                    onDismiss()
                },
                enabled = canSave,
            ) {
                Text(
                    "Save",
                    color = if (canSave) Color.White else Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun EditFieldLabel(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 13.sp,
        fontFamily = CalSansFamily,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun EditTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.35f)) },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = 16.sp,
            fontFamily = CalSansFamily,
        ),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            cursorColor = Color.White,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EditChipButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontFamily = CalSansFamily)
    }
}
