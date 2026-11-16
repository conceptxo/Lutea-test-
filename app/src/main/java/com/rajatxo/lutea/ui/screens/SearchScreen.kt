package com.rajatxo.lutea.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.lutea.data.model.Playlist
import com.rajatxo.lutea.data.scanner.MusicScanner
import com.rajatxo.lutea.data.store.PlaylistStore
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.CalSansFamily
import com.rajatxo.lutea.ui.icons.LuteaIcons
import com.rajatxo.lutea.ui.theme.LumenPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
//  Search
// ═══════════════════════════════════════════════════════════════════════════

private enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
}

/** One card in the top carousel. The field is drawn from nothing but [tone]. */
private data class GlowCard(
    val tag: String,
    val title: String,
    val subtitle: String,
    val query: String,
    val tone: Int,
)

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var scanning by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    val playlists by PlaylistStore.playlists.collectAsState()

    LaunchedEffect(Unit) {
        songs = withContext(Dispatchers.IO) { MusicScanner.scanMusic(context) }
        scanning = false
    }

    val q = query.trim()
    val cards = remember(playlists, songs) { buildCards(playlists, songs) }

    val songHits = remember(q, songs) {
        if (q.isEmpty()) emptyList()
        else songs.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }
    val artistHits = remember(q, songs) {
        if (q.isEmpty()) emptyList()
        else songs.filter { it.artist.contains(q, ignoreCase = true) }
            .groupBy { it.artist }
            .entries
            .sortedByDescending { it.value.size }
            .map { it.key to it.value }
    }
    val albumHits = remember(q, songs) {
        if (q.isEmpty()) emptyList()
        else songs.filter { it.album.contains(q, ignoreCase = true) }
            .groupBy { it.album }
            .entries
            .sortedByDescending { it.value.size }
            .map { it.key to it.value }
    }
    val playlistHits = remember(q, playlists) {
        if (q.isEmpty()) emptyList()
        else playlists.filter { it.name.contains(q, ignoreCase = true) }
    }
    val unexpected = remember(songs) {
        if (songs.isEmpty()) emptyList()
        else songs.shuffled(Random(songs.size * 31 + 7)).take(5)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 150.dp),
        ) {
            item { Headline() }

            if (cards.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    CardCarousel(
                        cards = cards,
                        onPick = { card ->
                            query = card.query
                            filter = SearchFilter.ALL
                        },
                    )
                }
            }

            item {
                Spacer(Modifier.height(26.dp))
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                )
            }

            item {
                Spacer(Modifier.height(14.dp))
                FilterChips(
                    selected = filter,
                    onSelect = { filter = it },
                )
            }

            if (q.isEmpty()) {
                item {
                    Spacer(Modifier.height(26.dp))
                    SectionTitle(
                        title = "Go unexpected",
                        subtitle = if (scanning) "Reading your library…" else "A few picks from your library",
                    )
                }
                items(unexpected, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        onClick = { PlaybackConnection.playSong(song, unexpected) },
                    )
                }
                if (!scanning && songs.isEmpty()) {
                    item {
                        EmptyNote("No music on this device yet.")
                    }
                }
            } else {
                val showSongs = filter == SearchFilter.ALL || filter == SearchFilter.SONGS
                val showArtists = filter == SearchFilter.ALL || filter == SearchFilter.ARTISTS
                val showAlbums = filter == SearchFilter.ALL || filter == SearchFilter.ALBUMS

                if (showSongs && songHits.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(22.dp))
                        SectionTitle("Songs", "${songHits.size} found")
                    }
                    items(
                        if (filter == SearchFilter.ALL) songHits.take(8) else songHits,
                        key = { it.id },
                    ) { song ->
                        SongRow(
                            song = song,
                            onClick = { PlaybackConnection.playSong(song, songHits) },
                        )
                    }
                }

                if (showArtists && artistHits.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        SectionTitle("Artists", null)
                    }
                    items(artistHits, key = { it.first }) { (artist, list) ->
                        ArtistRow(
                            name = artist,
                            count = list.size,
                            onClick = {
                                query = artist
                                filter = SearchFilter.ALL
                            },
                        )
                    }
                }

                if (showAlbums && albumHits.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        SectionTitle("Albums", null)
                    }
                    items(albumHits, key = { it.first }) { (album, list) ->
                        AlbumRow(
                            name = album,
                            artist = list.firstOrNull()?.artist.orEmpty(),
                            art = list.firstOrNull()?.albumArtUri,
                            onClick = {
                                query = album
                                filter = SearchFilter.ALL
                            },
                        )
                    }
                }

                if (filter == SearchFilter.ALL && playlistHits.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(18.dp))
                        SectionTitle("Playlists", null)
                    }
                    items(playlistHits, key = { it.id }) { playlist ->
                        val list = remember(playlist, songs) { songsOf(playlist, songs) }
                        PlaylistRow(
                            playlist = playlist,
                            art = list.firstOrNull()?.albumArtUri,
                            onClick = {
                                list.firstOrNull()?.let { first ->
                                    PlaybackConnection.playSong(first, list)
                                }
                            },
                        )
                    }
                }

                if (songHits.isEmpty() && artistHits.isEmpty() && albumHits.isEmpty() && playlistHits.isEmpty()) {
                    item {
                        Spacer(Modifier.height(30.dp))
                        EmptyNote("Nothing matches “$q”.")
                    }
                }
            }
        }
    }
}

private fun buildCards(playlists: List<Playlist>, songs: List<Song>): List<GlowCard> {
    if (playlists.isNotEmpty()) {
        return playlists.take(8).mapIndexed { i, p ->
            GlowCard(
                tag = "Playlist",
                title = p.name,
                subtitle = if (p.songIds.size == 1) "1 song" else "${p.songIds.size} songs",
                query = p.name,
                tone = i,
            )
        }
    }
    return songs.groupBy { it.artist }
        .entries
        .sortedByDescending { it.value.size }
        .take(8)
        .mapIndexed { i, (artist, list) ->
            GlowCard(
                tag = "Artist",
                title = artist,
                subtitle = if (list.size == 1) "1 song" else "${list.size} songs",
                query = artist,
                tone = i,
            )
        }
}

private fun songsOf(playlist: Playlist, all: List<Song>): List<Song> {
    if (all.isEmpty()) return emptyList()
    val byId = all.associateBy { it.id }
    return playlist.songIds.mapNotNull { byId[it] }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Header + carousel
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun Headline() {
    Text(
        text = "What do you\nfeel like playing?",
        color = Color.White,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CalSansFamily,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 14.dp),
    )
}

@Composable
private fun CardCarousel(
    cards: List<GlowCard>,
    onPick: (GlowCard) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.63f
        val cardHeight = cardWidth * 1.16f
        val edge = (maxWidth - cardWidth) / 2f
        LazyRow(
            contentPadding = PaddingValues(horizontal = edge),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(cards) { card ->
                GlowCardView(
                    card = card,
                    width = cardWidth,
                    height = cardHeight,
                    onClick = { onPick(card) },
                )
            }
        }
    }
}

/**
 * The card artwork is a small paint pipeline, not a single brush: a tinted base,
 * a bloom in one corner, a dark oval that carves the middle, a second soft light
 * on the opposite edge, an inner rim that brightens toward the bottom, and grain.
 * Same idea as the reference card — the gradient carries the whole card.
 */
@Composable
private fun GlowCardView(
    card: GlowCard,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
) {
    val hot = when (card.tone % 3) {
        0 -> LumenPalette.Mint
        1 -> LumenPalette.Ice
        else -> lerp(LumenPalette.Ice, LumenPalette.Mint, 0.5f)
    }
    val seed = card.tone * 977 + 41
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(28.dp))
            .drawWithCache {
                val rnd = Random(seed)
                val grain = List(220) {
                    floatArrayOf(
                        rnd.nextFloat() * size.width,
                        rnd.nextFloat() * size.height,
                        0.02f + rnd.nextFloat() * 0.05f,
                        0.7f + rnd.nextFloat() * 1.3f,
                    )
                }
                onDrawBehind {
                    paintGlowField(hot)
                    grain.forEach { g ->
                        drawCircle(
                            color = Color.White.copy(alpha = g[2]),
                            radius = g[3],
                            center = Offset(g[0], g[1]),
                        )
                    }
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(11.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = card.tag,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Column {
                Text(
                    text = card.title,
                    color = Color.White,
                    fontSize = 23.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = CalSansFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.subtitle,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun DrawScope.paintGlowField(hot: Color) {
    val w = size.width
    val h = size.height

    // 1. Tinted base, light in the top-right corner falling to black.
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                lerp(hot, Color.Black, 0.38f),
                lerp(hot, Color.Black, 0.82f),
                Color.Black,
            ),
            start = Offset(w, 0f),
            end = Offset(w * 0.15f, h),
        ),
    )

    // 2. The bloom that lights the top corner.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                hot.copy(alpha = 0.92f),
                hot.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = Offset(w * 0.82f, h * 0.14f),
            radius = w * 0.92f,
        ),
        topLeft = Offset(0f, -h * 0.22f),
        size = Size(w * 1.05f, h * 0.95f),
    )

    // 3. Dark oval rising from the bottom — carves the middle of the field.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.96f),
                Color.Black.copy(alpha = 0.62f),
                Color.Transparent,
            ),
            center = Offset(w * 0.46f, h * 1.06f),
            radius = w * 0.84f,
        ),
        topLeft = Offset(-w * 0.16f, h * 0.36f),
        size = Size(w * 1.32f, h * 1.02f),
    )

    // 4. A quieter light on the opposite edge, so the card is not one-sided.
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(hot.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(w * 0.04f, h * 0.60f),
            radius = w * 0.55f,
        ),
        topLeft = Offset(-w * 0.30f, h * 0.12f),
        size = Size(w * 0.92f, h * 0.92f),
    )

    // 5. Inner rim, brighter along the bottom edge.
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, hot.copy(alpha = 0.55f)),
            startY = h * 0.25f,
            endY = h,
        ),
        topLeft = Offset(1.2f, 1.2f),
        size = Size(w - 2.4f, h - 2.4f),
        cornerRadius = CornerRadius(w * 0.11f),
        style = Stroke(width = 1.4f),
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Search field + chips
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
        ),
        cursorBrush = SolidColor(LumenPalette.Ice),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = LuteaIcons.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search songs, artists, albums",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable { onQueryChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun FilterChips(
    selected: SearchFilter,
    onSelect: (SearchFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(SearchFilter.entries.toList()) { f ->
            val on = f == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) Color.White else Color.White.copy(alpha = 0.07f))
                    .border(
                        width = 1.dp,
                        color = if (on) Color.White else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                    )
                    .clickable { onSelect(f) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = f.label,
                    color = if (on) Color.Black else Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Rows
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.48f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = formatSearchDuration(song.duration),
            color = Color.White.copy(alpha = 0.38f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ArtistRow(name: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(LumenPalette.Ice.copy(alpha = 0.14f))
                .border(1.dp, LumenPalette.Ice.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = LumenPalette.Ice,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = CalSansFamily,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (count == 1) "1 song" else "$count songs",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun AlbumRow(name: String, artist: String, art: android.net.Uri?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = art,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, art: android.net.Uri?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = art,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(LumenPalette.Mint.copy(alpha = 0.10f)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (playlist.songIds.size == 1) "Playlist · 1 song" else "Playlist · ${playlist.songIds.size} songs",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.40f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatSearchDuration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
