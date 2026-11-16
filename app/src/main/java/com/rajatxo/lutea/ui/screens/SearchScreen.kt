package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rajatxo.lutea.R
import com.rajatxo.lutea.data.model.Playlist
import com.rajatxo.lutea.data.scanner.MusicScanner
import com.rajatxo.lutea.data.store.PlaylistStore
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.playback.PlaybackConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH PAGE — everything below is this page and nothing else.
//
//  Self-contained on purpose: one file to read, one file to change.
//    0. LOOK    — palette, display font and the one icon the page uses
//    1. DATA    — the SearchScreen composable: scan, filter, state
//    2. HEADER  — headline + glow-card carousel
//    3. FIELD   — search pill + filter chips
//    4. ROWS    — result rows (song / artist / album / playlist)
//
//  The only outside calls are the three app services:
//    MusicScanner.scanMusic(context)      -> List<Song>
//    PlaylistStore.playlists              -> StateFlow<List<Playlist>>
//    PlaybackConnection.playSong(song, queue)
// ═══════════════════════════════════════════════════════════════════════════

// ── 0. LOOK ────────────────────────────────────────────────────────────────

/** X — the main colour (selected / active). */
private val Mint = Color(0xFFD4FF9C)

/** Y — the secondary colour (inactive / ambient). */
private val Ice = Color(0xFF9EF9FF)

/** Cal Sans, the app's display face, inlined so the page owns its type. */
private val SearchSans = FontFamily(
    Font(R.font.cal_sans_semibold, FontWeight.Normal),
    Font(R.font.cal_sans_semibold, FontWeight.Medium),
    Font(R.font.cal_sans_semibold, FontWeight.SemiBold),
    Font(R.font.cal_sans_semibold, FontWeight.Bold),
)

/** Lucide search glyph (MIT), inlined so the page owns its icon. */
private val SearchGlyph: ImageVector = ImageVector.Builder(
    name = "Search",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
    ) {
        moveTo(19f, 11f)
        arcTo(8f, 8f, 0f, true, true, 3f, 11f)
        arcTo(8f, 8f, 0f, true, true, 19f, 11f)
        moveTo(21f, 21f)
        lineTo(16.66f, 16.66f)
    }
}.build()

// ── 1. DATA ────────────────────────────────────────────────────────────────

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
//  2. HEADER — headline, glow-card carousel and the card paint pipeline
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun Headline() {
    Text(
        text = "What do you\nfeel like playing?",
        color = Color.White,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = SearchSans,
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
        0 -> Mint
        1 -> Ice
        else -> lerp(Ice, Mint, 0.5f)
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
                    fontFamily = SearchSans,
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
//  3. FIELD — the typewriter search bar and the filter chips
//
//  A line of typewriter text sits above a metal rail. A cream 3D capsule
//  (the carriage) rides the rail and follows the caret as you type; a small
//  T-shaped hammer on top strikes on every keystroke and the first LED
//  flashes amber. Clearing the text slides the carriage back home.
// ═══════════════════════════════════════════════════════════════════════════

private val PaperCream = Color(0xFFF4EEDF)
private val CapsuleLight = Color(0xFFF8F3E8)
private val CapsuleDark = Color(0xFFD3C9B2)
private val LedAmber = Color(0xFFFFA23C)
private val LedPink = Color(0xFFE8828F)

private val CapsuleWidth = 64.dp
private val CapsuleHeight = 28.dp
private val RailAreaHeight = 52.dp
private val RailCenterY = 34.dp          // vertical centre of the rail inside the rail area
private val TextStart = 28.dp           // search glyph + gap, where the typed text begins

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var caretPx by remember { mutableStateOf(0f) }      // caret x inside the text box
    var viewportPx by remember { mutableStateOf(0f) }   // visible width of the text box
    var railPx by remember { mutableStateOf(0f) }       // rail width

    // Carriage target: centre the capsule under the caret, keep it between the end clamps.
    val capsuleWPx = with(density) { CapsuleWidth.toPx() }
    val textStartPx = with(density) { TextStart.toPx() }
    val edgePx = with(density) { 16.dp.toPx() }
    val minLeft = edgePx
    val maxLeft = (railPx - capsuleWPx - edgePx).coerceAtLeast(minLeft)
    val targetLeft = (textStartPx + minOf(caretPx, viewportPx) - capsuleWPx / 2f)
        .coerceIn(minLeft, maxLeft)
    val carriageX by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "carriage",
    )

    // One strike per keystroke: hammer jumps up, capsule dips, LED flashes.
    val strike = remember { Animatable(0f) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            strike.snapTo(1f)
            strike.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 700f))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focus.requestFocus()
                keyboard?.show()
            },
    ) {
        // ── typed line ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SearchGlyph,
                contentDescription = null,
                tint = PaperCream.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { viewportPx = it.width.toFloat() },
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search songs, artists, albums",
                        color = PaperCream.copy(alpha = 0.32f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PaperCream,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                    ),
                    cursorBrush = SolidColor(PaperCream),
                    onTextLayout = { layout ->
                        val end = layout.layoutInput.text.length
                        caretPx = layout.getCursorRect(query.length.coerceIn(0, end)).left
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
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

        Spacer(Modifier.height(4.dp))

        // ── rail + carriage ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RailAreaHeight)
                .onSizeChanged { railPx = it.width.toFloat() },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRail(RailCenterY.toPx()) }
            Box(
                modifier = Modifier
                    .size(CapsuleWidth, RailAreaHeight)
                    .graphicsLayer { translationX = carriageX }
                    .drawBehind { drawCarriage(strike.value) },
            )
        }
    }
}

/** Metal pipe with a lit top edge, dark end clamps and a faint light spill below. */
private fun DrawScope.drawRail(centerY: Float) {
    val h = 10.dp.toPx()
    val top = centerY - h / 2f
    val clampW = 12.dp.toPx()
    val clampH = 22.dp.toPx()

    // light spill under the pipe
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            startY = top + h,
            endY = top + h + 12.dp.toPx(),
        ),
        topLeft = Offset(clampW, top + h),
        size = Size(size.width - clampW * 2f, 12.dp.toPx()),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    // pipe body — rounded cylinder shading
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF909498),
            0.28f to Color(0xFF5F6367),
            0.62f to Color(0xFF35383B),
            1f to Color(0xFF1B1C1E),
            startY = top,
            endY = top + h,
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, h),
        cornerRadius = CornerRadius(h / 2f),
    )
    // specular streak
    drawRoundRect(
        color = Color.White.copy(alpha = 0.30f),
        topLeft = Offset(clampW + 2.dp.toPx(), top + h * 0.20f),
        size = Size(size.width - clampW * 2f - 4.dp.toPx(), 1.2.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )
    // end clamps
    for (x in floatArrayOf(0f, size.width - clampW)) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF55585C),
                0.5f to Color(0xFF2A2C2F),
                1f to Color(0xFF111213),
                startY = centerY - clampH / 2f,
                endY = centerY + clampH / 2f,
            ),
            topLeft = Offset(x, centerY - clampH / 2f),
            size = Size(clampW, clampH),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            topLeft = Offset(x + 1.dp.toPx(), centerY - clampH / 2f + 1.dp.toPx()),
            size = Size(clampW - 2.dp.toPx(), 1.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
    }
}

/**
 * The capsule carriage: cream body with a lit top bevel, three LEDs and the T hammer.
 * Local origin = top-left of the capsule's box; the capsule sits low so the hammer has room above it.
 * @param strike 1 right after a keystroke, easing back to 0
 */
private fun DrawScope.drawCarriage(strike: Float) {
    val w = size.width
    val capH = CapsuleHeight.toPx()
    val capTop = (RailCenterY.toPx() - capH / 2f) + 1.dp.toPx() * strike.coerceIn(0f, 1f)
    val corner = CornerRadius(capH / 2f)
    val s = strike.coerceIn(-0.3f, 1f)

    // ── T hammer (drawn first so the capsule hides its foot) ──
    val cx = w * 0.52f
    val lift = 5.dp.toPx() * s
    val barW = 15.dp.toPx()
    val barH = 3.5.dp.toPx()
    val barTop = capTop - 17.dp.toPx() - lift
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFFC9C7C0),
            1f to Color(0xFF7C7D79),
            startX = cx - 1.5.dp.toPx(),
            endX = cx + 1.5.dp.toPx(),
        ),
        topLeft = Offset(cx - 1.5.dp.toPx(), barTop + barH),
        size = Size(3.dp.toPx(), capTop - barTop - barH + 4.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFF5F3EC),
            1f to Color(0xFFA9A8A2),
            startY = barTop,
            endY = barTop + barH,
        ),
        topLeft = Offset(cx - barW / 2f, barTop),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(1.5.dp.toPx()),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.35f),
        topLeft = Offset(cx - barW / 2f, barTop + barH),
        size = Size(barW, 1.dp.toPx()),
    )

    // ── contact shadow on the rail ──
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.60f),
        topLeft = Offset(3.dp.toPx(), capTop + 5.dp.toPx()),
        size = Size(w - 6.dp.toPx(), capH),
        cornerRadius = corner,
    )

    // ── body ──
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(CapsuleLight, CapsuleDark),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(0f, capTop),
        size = Size(w, capH),
        cornerRadius = corner,
    )
    // lit top bevel
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), Color.Transparent),
            startY = capTop,
            endY = capTop + capH * 0.6f,
        ),
        topLeft = Offset(0.6.dp.toPx(), capTop + 0.6.dp.toPx()),
        size = Size(w - 1.2.dp.toPx(), capH - 1.2.dp.toPx()),
        cornerRadius = corner,
        style = Stroke(width = 1.3.dp.toPx()),
    )
    // shaded underside
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
            startY = capTop + capH * 0.4f,
            endY = capTop + capH,
        ),
        topLeft = Offset(0.6.dp.toPx(), capTop + 0.6.dp.toPx()),
        size = Size(w - 1.2.dp.toPx(), capH - 1.2.dp.toPx()),
        cornerRadius = corner,
        style = Stroke(width = 1.3.dp.toPx()),
    )
    // hammer slot
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.40f),
        topLeft = Offset(cx - 5.dp.toPx(), capTop + 1.5.dp.toPx()),
        size = Size(10.dp.toPx(), 2.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )

    // ── LEDs: green → amber while a key is struck, dark, pink ──
    val ledR = 2.3.dp.toPx()
    val ledY = capTop + capH / 2f
    val ledX = 14.dp.toPx()
    val gap = 8.dp.toPx()
    val leds = arrayOf(
        lerp(Mint, LedAmber, s.coerceIn(0f, 1f)),
        Color(0xFF2B2B2B),
        LedPink,
    )
    for (i in 0 until 3) {
        val c = Offset(ledX + gap * i, ledY)
        if (i != 1) {
            drawCircle(color = leds[i].copy(alpha = 0.30f), radius = ledR * 2.6f, center = c)
        }
        drawCircle(color = leds[i], radius = ledR, center = c)
    }
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
//  4. ROWS — result rows: song, artist, album, playlist
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
                .background(Ice.copy(alpha = 0.14f))
                .border(1.dp, Ice.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = Ice,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = SearchSans,
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
                .background(Mint.copy(alpha = 0.10f)),
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
