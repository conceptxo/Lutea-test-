package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
import android.net.Uri
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import com.rajatxo.lutea.ui.components.LightRaysBackground
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH PAGE — everything below is this page and nothing else.
//
//  Self-contained on purpose: one file to read, one file to change.
//    0. LOOK    — palette, display font and the one icon the page uses
//    1. DATA    — the SearchScreen composable: scan, filter, state
//    2. HEADER  — headline + the card deck (glow cards, or song cards while searching)
//    3. FIELD   — typewriter search bar + filter chips
//    4. ROWS    — result rows (song / artist / album / playlist)
//
//  The only outside calls are the app services plus the shared light-ray
//  backdrop (the same one the playlist page uses):
//    MusicScanner.scanMusic(context)      -> List<Song>
//    PlaylistStore.playlists              -> StateFlow<List<Playlist>>
//    PlaybackConnection.playSong(song, queue)
//    LightRaysBackground(modifier)
// ═══════════════════════════════════════════════════════════════════════════

// ── 0. LOOK ────────────────────────────────────────────────────────────────

/** X — the main colour (selected / active). */
private val Mint = Color(0xFFD4FF9C)

/** Y — the secondary colour (inactive / ambient). */
private val Ice = Color(0xFF9EF9FF)

/** The playlist page's charcoal → black backdrop, reused here. */
private val PageTop = Color(0xFF3A3A3A)
private val PageBottom = Color.Black

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

/**
 * One card in the deck.
 *
 * With an empty query the deck shows playlists (or artists) as painted glow
 * cards. As soon as something is typed it switches to songs: [art] is the cover,
 * [matched] says whether the song still matches the query (misses stay in the
 * deck, blurred), and [song] is what a tap plays.
 */
private data class CarouselCard(
    val id: String,
    val tag: String,
    val title: String,
    val subtitle: String,
    val query: String,
    val tone: Int,
    val art: Uri? = null,
    val matched: Boolean = true,
    val song: Song? = null,
)

@Composable
fun SearchScreen() {
    // Haze state: everything inside the source box is what the header blur eats.
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    // The static header zone: the headline lives here, and it never moves.
    val headerZone = statusTop + 96.dp

    val history = remember { sampleHistory() }
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ── Everything the blur samples: background, rays, and the scrolling list ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            // Background: charcoal → black, the playlist page's gradient.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(PageTop, PageBottom))),
            )

            // The light rays.
            LightRaysBackground(modifier = Modifier.fillMaxSize())

            // ── History: starts right under the headline, slides up into the blur ──
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // No layout padding: the list runs all the way to the top of the
                // screen so rows slide UNDER the header blur instead of being
                // chopped at its edge. The padding only sets where row 1 starts.
                contentPadding = PaddingValues(top = headerZone + 4.dp, bottom = 180.dp),
            ) {
                items(history) { entry ->
                    HistoryRow(text = entry)
                }
            }
        }

        // ── The blur: pinned right under the headline, melting the list into it ──
        HeaderBlur(hazeState = hazeState, height = headerZone + BlurRun)

        // ── The headline: static, and always on top of the blur ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Headline()
        }
    }
}

/** How far the blur runs below the headline before it is fully gone. */
private val BlurRun = 132.dp

/** Blur strength right under the headline (0..1). */
private const val BlurPeak = 0.95f

/**
 * The wash over the band, as (position, alpha) pairs. Nearly opaque at the very
 * top so rows genuinely disappear behind the title, then a smooth melt to clear
 * at the bottom of the band.
 */
private val BlurScrim = arrayOf(
    0.00f to 0.86f,
    0.22f to 0.80f,
    0.44f to 0.50f,
    0.70f to 0.20f,
    1.00f to 0.00f,
)

/**
 * The header blur — one band from the very top of the screen down past the
 * headline, so there is no seam behind the title. Content scrolling up behind it
 * is blurred as a progressive vertical gradient: full strength at the top,
 * easing out to nothing at the bottom of the band. A dark scrim rides on top,
 * fading with it, so the band reads as a single sheet of glass.
 */
@Composable
private fun HeaderBlur(hazeState: HazeState, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .hazeEffect(state = hazeState) {
                blurRadius = 26.dp
                noiseFactor = 0f
                progressive = HazeProgressive.verticalGradient(
                    easing = EaseOutCubic,
                    startIntensity = BlurPeak,
                    endIntensity = 0f,
                )
            },
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    colorStops = BlurScrim.map { (at, alpha) -> at to Color.Black.copy(alpha = alpha) }
                        .toTypedArray(),
                ),
            ),
    )
}

/** One recent search — plain text, bright enough to read the blur on. */
@Composable
private fun HistoryRow(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.92f),
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = SearchSans,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

/** Placeholder recents, just to watch them slide under the blur. */
private fun sampleHistory(): List<String> = listOf(
    "coldplay",
    "colours",
    "cubic",
    "midnight city",
    "the weeknd",
    "sunflower",
    "lofi beats to study",
    "arctic monkeys",
    "do i wanna know",
    "tame impala",
    "let it happen",
    "pink floyd",
    "comfortably numb",
    "daft punk",
    "instant crush",
    "radiohead",
    "weird fishes",
    "kendrick lamar",
    "money trees",
    "frank ocean",
)

// ═══════════════════════════════════════════════════════════════════════════
//  1b. TOP BLUR — the header zone melts whatever sits under it
// ═══════════════════════════════════════════════════════════════════════════

/** How far past the header zone the blur runs before it has faded out. */
private val TopBlurRun = 96.dp

/** Blur strength at the very top of the band (0..1). */
private const val TopBlurPeak = 0.9f

/** Wash over the band, strongest at the top. Keeps the edge off the glass. */
private const val TopScrimPeak = 0.38f

/** Scrim stops — more stops means a smoother melt. */
private const val TopScrimStops = 12

/**
 * The top blur — a band across the header zone that blurs whatever is behind it
 * as a progressive vertical gradient: full strength at the status bar, easing
 * out to nothing [TopBlurRun] below the header. A soft scrim of the page colour
 * sits over it so the top reads as one sheet rather than a hard-edged strip.
 */
@Composable
private fun TopBlur(hazeState: HazeState) {
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val bandHeight = statusTop + 64.dp + TopBlurRun

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bandHeight)
            .hazeEffect(state = hazeState) {
                blurRadius = 24.dp
                noiseFactor = 0f
                progressive = HazeProgressive.verticalGradient(
                    easing = EaseOutCubic,
                    startIntensity = TopBlurPeak,
                    endIntensity = 0f,
                )
            },
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bandHeight)
            .background(
                Brush.verticalGradient(
                    colorStops = Array(TopScrimStops) { i ->
                        val t = i / (TopScrimStops - 1f)
                        t to PageTop.copy(alpha = TopScrimPeak * (1f - EaseOutCubic.transform(t)))
                    },
                ),
            ),
    )
}

private fun songMatches(song: Song, q: String): Boolean =
    song.title.contains(q, ignoreCase = true) ||
        song.artist.contains(q, ignoreCase = true) ||
        song.album.contains(q, ignoreCase = true)

/** Best match first: title prefix, then title, then artist prefix, then artist. */
private fun rankSongs(q: String, songs: List<Song>): List<Song> {
    if (q.isEmpty()) return emptyList()
    return songs.filter { songMatches(it, q) }
        .sortedWith(
            compareByDescending<Song> { it.title.startsWith(q, ignoreCase = true) }
                .thenByDescending { it.title.contains(q, ignoreCase = true) }
                .thenByDescending { it.artist.startsWith(q, ignoreCase = true) }
                .thenByDescending { it.artist.contains(q, ignoreCase = true) },
        )
        .take(8)
}

private fun buildCards(playlists: List<Playlist>, songs: List<Song>): List<CarouselCard> {
    if (playlists.isNotEmpty()) {
        return playlists.take(8).mapIndexed { i, p ->
            CarouselCard(
                id = "pl:${p.id}",
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
            CarouselCard(
                id = "ar:$artist",
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
//  2. HEADER — headline, the card deck and the card paint pipeline
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

/**
 * A deck, not a pager: one card sits in the middle and its neighbours peek in
 * from both edges, scaled down a little. It snaps on release, and typing always
 * brings the best match back to the centre.
 */
@Composable
private fun CardCarousel(
    cards: List<CarouselCard>,
    fraction: Float,
    resetKey: Any?,
    onPick: (CarouselCard) -> Unit,
) {
    val state = rememberLazyListState()
    LaunchedEffect(resetKey) {
        if (cards.isNotEmpty() && state.firstVisibleItemIndex != 0) state.scrollToItem(0)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * fraction
        val cardHeight = cardWidth * 1.2f
        val edge = (maxWidth - cardWidth) / 2f
        val cardPx = with(LocalDensity.current) { cardWidth.toPx() }
        val info = state.layoutInfo
        val viewportCentre = (info.viewportStartOffset + info.viewportEndOffset) / 2f

        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = edge),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            flingBehavior = rememberSnapFlingBehavior(state),
        ) {
            items(cards, key = { it.id }) { card ->
                val item = info.visibleItemsInfo.firstOrNull { it.key == card.id }
                val scale = if (item == null || cardPx <= 0f || info.visibleItemsInfo.isEmpty()) {
                    1f
                } else {
                    val centre = item.offset + item.size / 2f
                    (1f - abs(centre - viewportCentre) / cardPx * 0.15f).coerceIn(0.86f, 1f)
                }
                CardView(card, cardWidth, cardHeight, scale, onPick)
            }
        }
    }
}

@Composable
private fun CardView(
    card: CarouselCard,
    width: Dp,
    height: Dp,
    scale: Float,
    onPick: (CarouselCard) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width, height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(26.dp))
            .clickable(enabled = card.matched) { onPick(card) },
    ) {
        if (card.song != null) SongCard(card) else GlowCard(card)
    }
}

/** A search result: cover art, a scrim, and the song name + artist at the bottom. */
@Composable
private fun SongCard(card: CarouselCard) {
    val on = card.matched

    // Base tone, so a song without a cover still reads as a card.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(lerp(Mint, Color.Black, 0.55f), Color.Black))),
    )
    AsyncImage(
        model = card.art,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (on) 1f else 0.55f)
            .then(if (on) Modifier else Modifier.blur(9.dp)),
    )
    // Scrim so the type stays readable over any cover.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.42f to Color.Transparent,
                    0.72f to Color.Black.copy(alpha = 0.45f),
                    1f to Color.Black.copy(alpha = 0.92f),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (on) 0.16f else 0.05f),
                shape = RoundedCornerShape(26.dp),
            ),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            text = card.title,
            color = Color.White.copy(alpha = if (on) 1f else 0.55f),
            fontSize = 19.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SearchSans,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = card.subtitle,
            color = Color.White.copy(alpha = if (on) 0.62f else 0.35f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The no-query card: a tinted base, a bloom in one corner, a dark oval that
 * carves the middle, a second soft light on the opposite edge, an inner rim
 * that brightens toward the bottom, and grain.
 */
@Composable
private fun GlowCard(card: CarouselCard) {
    val hot = when (card.tone % 3) {
        0 -> Mint
        1 -> Ice
        else -> lerp(Ice, Mint, 0.5f)
    }
    val seed = card.tone * 977 + 41

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            },
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
//  One line of typewriter text sits above a steel rail on the black page. A
//  cream capsule (the carriage) rides the rail and follows the caret; the
//  T-shaped hammer on top strikes on every keystroke and the first LED flashes
//  amber. Clearing the text slides the carriage back home.
//  Everything is drawn with gradients and smooth layered shadows — no images.
// ═══════════════════════════════════════════════════════════════════════════

private val TypeCream = Color(0xFFFCF2C7)   // butter cream — text + capsule

private val CapsuleWidth = 80.dp
private val CapsuleHeight = 34.dp
private val RailAreaHeight = 60.dp
private val RailCenterY = 36.dp         // centre of the rail inside the rail area
private val TextSideInset = 24.dp
private val TextStart = 52.dp           // side inset + search glyph + gap: where typed text begins
private val TypedLineHeight = 26.dp     // kept short so the text sits right above the hammer

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Everything on the rail is designed for a 360dp-wide phone and scales with the screen width.
    val k = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.85f, 1.35f)

    var caretPx by remember { mutableStateOf(0f) }      // caret x inside the text box
    var viewportPx by remember { mutableStateOf(0f) }   // visible width of the text box
    var railPx by remember { mutableStateOf(0f) }       // rail (= card) width

    // Carriage target: centre the capsule under the caret, keep it between the end clamps.
    val capsuleWPx = with(density) { CapsuleWidth.toPx() } * k
    val textStartPx = with(density) { TextStart.toPx() }
    val edgePx = with(density) { 26.dp.toPx() } * k
    val maxLeft = (railPx - capsuleWPx - edgePx).coerceAtLeast(edgePx)
    val targetLeft = (textStartPx + minOf(caretPx, viewportPx) - capsuleWPx / 2f)
        .coerceIn(edgePx, maxLeft)
    val carriageX by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "carriage",
    )

    // One strike per keystroke: hammer jumps up, capsule dips, LED flashes.
    val strike = remember { Animatable(0f) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            strike.animateTo(1f, tween(40, easing = LinearOutSlowInEasing))   // hammer flies up to the text
            strike.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 900f))   // and drops back
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                .padding(horizontal = TextSideInset)
                .height(TypedLineHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SearchGlyph,
                contentDescription = null,
                tint = TypeCream.copy(alpha = 0.5f),
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
                        color = TypeCream.copy(alpha = 0.32f),
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
                        color = TypeCream,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                    ),
                    cursorBrush = SolidColor(TypeCream),
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

        // ── rail + carriage: edge to edge, scaled to the screen width ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RailAreaHeight * k)
                .onSizeChanged { railPx = it.width.toFloat() },
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * k, density.fontScale),
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
        Spacer(Modifier.height(4.dp))
    }
}

/** Slate pipe: soft top edge, broad pale highlight band at ~40%, falling to near-black. */
private fun DrawScope.drawRail(centerY: Float) {
    val h = 13.5.dp.toPx()
    val top = centerY - h / 2f

    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF323639),
            0.077f to Color(0xFF3A3F42),
            0.154f to Color(0xFF4B5053),
            0.23f to Color(0xFF7A7F82),
            0.308f to Color(0xFFACB1B4),
            0.385f to Color(0xFFACB1B4),
            0.46f to Color(0xFF83888C),
            0.54f to Color(0xFF5F6468),
            0.615f to Color(0xFF575C60),
            0.69f to Color(0xFF4A4F52),
            0.77f to Color(0xFF373B3E),
            0.85f to Color(0xFF262A2B),
            0.92f to Color(0xFF1C2021),
            1f to Color(0xFF151515),
            startY = top,
            endY = top + h,
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, h),
    )

    // square charcoal collars at both ends
    val cw = 11.dp.toPx()
    val ch = 19.dp.toPx()
    val inset = 6.dp.toPx()
    for (left in booleanArrayOf(true, false)) {
        val x = if (left) inset else size.width - inset - cw
        val y = centerY - ch / 2f
        softShadow(
            topLeft = Offset(x, y + 2.dp.toPx()),
            size = Size(cw, ch),
            corner = 3.dp.toPx(),
            color = Color.Black,
            spread = 5.dp.toPx(),
            alpha = 0.7f,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF2B2D2C),
                0.12f to Color(0xFF323335),
                0.30f to Color(0xFF3D3E40),
                0.42f to Color(0xFF414244),
                0.55f to Color(0xFF3A3B3D),
                0.65f to Color(0xFF2E2F31),
                0.80f to Color(0xFF212224),
                0.90f to Color(0xFF181A19),
                1f to Color(0xFF0F0F0F),
                startY = y,
                endY = y + ch,
            ),
            topLeft = Offset(x, y),
            size = Size(cw, ch),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(x + 0.5.dp.toPx(), y + 0.5.dp.toPx()),
            size = Size(cw - 1.dp.toPx(), ch - 1.dp.toPx()),
            cornerRadius = CornerRadius(2.6.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** Smooth, blur-like shadow: stacked rounded rects with a quadratic falloff (no BlurMaskFilter needed). */
private fun DrawScope.softShadow(
    topLeft: Offset,
    size: Size,
    corner: Float,
    color: Color,
    spread: Float,
    alpha: Float,
    steps: Int = 12,
) {
    for (i in 0 until steps) {
        val t = i / (steps - 1f)
        val g = spread * t
        val a = alpha * (1f - t) * (1f - t) * 0.42f
        drawRoundRect(
            color = color.copy(alpha = a.coerceIn(0f, 1f)),
            topLeft = Offset(topLeft.x - g, topLeft.y - g),
            size = Size(size.width + g * 2f, size.height + g * 2f),
            cornerRadius = CornerRadius(corner + g),
        )
    }
}

/**
 * The carriage, to the reference's proportions: flat warm-white capsule with a soft bevel line near
 * the top, edge-shaded underside, a dark hammer slot, three small flat LEDs and a light-grey T hammer
 * with a dark cylinder bar.
 * Local origin = top-left of the capsule's box; the capsule sits low so the hammer has room above it.
 * @param strike 1 right after a keystroke, easing back to 0
 */
private fun DrawScope.drawCarriage(strike: Float) {
    val w = size.width
    val capH = CapsuleHeight.toPx()
    val s = strike.coerceIn(-0.3f, 1f)
    val capTop = (RailCenterY.toPx() - capH / 2f) + 1.dp.toPx() * s.coerceAtLeast(0f)
    val corner = CornerRadius(capH / 2f)
    val cx = w * 0.5f

    // ── smooth shadow cast by the capsule (visible on the pipe) ──
    softShadow(
        topLeft = Offset(4.dp.toPx(), capTop + 4.dp.toPx()),
        size = Size(w - 8.dp.toPx(), capH),
        corner = capH / 2f,
        color = Color.Black,
        spread = 8.dp.toPx(),
        alpha = 0.9f,
    )

    // ── T hammer: light-grey stem under a small dark cylinder bar ──
    val lift = 14.dp.toPx() * s
    val barW = 15.dp.toPx()
    val barH = 4.8.dp.toPx()
    val stemW = 3.4.dp.toPx()
    val stemLen = 11.dp.toPx()
    val barTop = capTop - stemLen - barH - lift
    softShadow(
        topLeft = Offset(cx - barW / 2f + 1.5.dp.toPx(), barTop + 2.dp.toPx()),
        size = Size(barW, barH),
        corner = 1.6.dp.toPx(),
        color = Color.Black,
        spread = 4.dp.toPx(),
        alpha = 0.5f,
        steps = 8,
    )
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFFD6D4CF),
            0.15f to Color(0xFFC7C5C2),
            0.80f to Color(0xFFC8C8C6),
            1f to Color(0xFFA9AAA4),
            startX = cx - stemW / 2f,
            endX = cx + stemW / 2f,
        ),
        topLeft = Offset(cx - stemW / 2f, barTop + barH - 0.5.dp.toPx()),
        size = Size(stemW, capTop - (barTop + barH) + 1.5.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF555A5D),
            0.25f to Color(0xFFA5AAAE),
            0.50f to Color(0xFF686D71),
            0.75f to Color(0xFF3F4346),
            1f to Color(0xFF202425),
            startY = barTop,
            endY = barTop + barH,
        ),
        topLeft = Offset(cx - barW / 2f, barTop),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(1.6.dp.toPx()),
    )
    // pale rims on the bar's rounded ends
    for (ex in floatArrayOf(cx - barW / 2f + 0.4.dp.toPx(), cx + barW / 2f - 1.2.dp.toPx())) {
        drawRect(
            color = Color(0xFFB8BCC0).copy(alpha = 0.55f),
            topLeft = Offset(ex, barTop + 0.8.dp.toPx()),
            size = Size(0.8.dp.toPx(), barH - 1.6.dp.toPx()),
        )
    }

    // ── body: almost flat butter cream (#FCF2C7) ──
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFFFF8DC),
            0.06f to Color(0xFFFDF4CD),
            0.20f to Color(0xFFFCF3CB),
            0.30f to Color(0xFFFCF2C9),
            0.45f to Color(0xFFFCF2C7),
            0.80f to Color(0xFFFCF2C7),
            0.88f to Color(0xFFF9EEBE),
            0.93f to Color(0xFFF2E3AB),
            0.97f to Color(0xFFE3D08C),
            1f to Color(0xFFC9B374),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(0f, capTop),
        size = Size(w, capH),
        cornerRadius = corner,
    )
    // hairline outline
    drawRoundRect(
        color = Color(0xFFE8D9A0).copy(alpha = 0.75f),
        topLeft = Offset(0.4.dp.toPx(), capTop + 0.4.dp.toPx()),
        size = Size(w - 0.8.dp.toPx(), capH - 0.8.dp.toPx()),
        cornerRadius = corner,
        style = Stroke(width = 0.8.dp.toPx()),
    )
    // soft shading rolling the underside away from the light (wide + narrow stroke = smooth)
    val shade = Color(0xFFB59F5A)
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.50f to shade.copy(alpha = 0f),
            1f to shade.copy(alpha = 0.30f),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(2.dp.toPx(), capTop + 2.dp.toPx()),
        size = Size(w - 4.dp.toPx(), capH - 4.dp.toPx()),
        cornerRadius = CornerRadius(capH / 2f - 2.dp.toPx()),
        style = Stroke(width = 4.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.62f to shade.copy(alpha = 0f),
            1f to shade.copy(alpha = 0.55f),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(0.8.dp.toPx(), capTop + 0.8.dp.toPx()),
        size = Size(w - 1.6.dp.toPx(), capH - 1.6.dp.toPx()),
        cornerRadius = CornerRadius(capH / 2f - 0.8.dp.toPx()),
        style = Stroke(width = 1.6.dp.toPx()),
    )
    // faint darkening along the two rounded ends
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFFCDB878).copy(alpha = 0.32f),
            0.10f to Color.Transparent,
            0.90f to Color.Transparent,
            1f to Color(0xFFCDB878).copy(alpha = 0.32f),
            startX = 0f,
            endX = w,
        ),
        topLeft = Offset(1.dp.toPx(), capTop + 1.dp.toPx()),
        size = Size(w - 2.dp.toPx(), capH - 2.dp.toPx()),
        cornerRadius = CornerRadius(capH / 2f - 1.dp.toPx()),
        style = Stroke(width = 3.dp.toPx()),
    )
    // bevel line: a pale curved highlight ~20% down that bends round both ends and fades out
    val inset = capH * 0.19f
    val bevelBrush = Brush.verticalGradient(
        0f to Color(0xFFFFFDEB).copy(alpha = 0.95f),
        1f to Color(0xFFFFFDEB).copy(alpha = 0f),
        startY = capTop + inset,
        endY = capTop + capH * 0.52f,
    )
    drawRoundRect(
        brush = bevelBrush,
        topLeft = Offset(inset, capTop + inset),
        size = Size(w - inset * 2f, capH - inset * 2f),
        cornerRadius = CornerRadius(capH / 2f - inset),
        style = Stroke(width = 2.6.dp.toPx()),
        alpha = 0.35f,
    )
    drawRoundRect(
        brush = bevelBrush,
        topLeft = Offset(inset, capTop + inset),
        size = Size(w - inset * 2f, capH - inset * 2f),
        cornerRadius = CornerRadius(capH / 2f - inset),
        style = Stroke(width = 1.1.dp.toPx()),
    )

    // ── hammer slot: dark recess, lighter toward its lower lip ──
    val slotW = 16.5.dp.toPx()
    val slotH = 3.6.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF1D1C18),
            0.30f to Color(0xFF1D1C18),
            0.50f to Color(0xFF36332E),
            0.80f to Color(0xFF5B574E),
            1f to Color(0xFF6E6A5F),
            startY = capTop,
            endY = capTop + slotH,
        ),
        topLeft = Offset(cx - slotW / 2f, capTop),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(1.2.dp.toPx()),
    )

    // ── LEDs: small and flat — green (amber while a key is struck), dark brown, dusty rose ──
    val ledR = 2.3.dp.toPx()
    val ledY = capTop + capH / 2f
    val ledX = w * 0.143f
    val gap = w * 0.113f
    val sc = s.coerceIn(0f, 1f)
    val c1 = Offset(ledX, ledY)
    drawCircle(lerp(Color(0xFFD0DAC6), Color(0xFFFFE3B8), sc), radius = ledR + 0.9.dp.toPx(), center = c1)
    drawCircle(
        brush = Brush.radialGradient(
            0f to lerp(Color(0xFF8CDA66), Color(0xFFFFB84D), sc),
            1f to lerp(Color(0xFF72C255), Color(0xFFF08F1F), sc),
            center = c1,
            radius = ledR,
        ),
        radius = ledR,
        center = c1,
    )
    val c2 = Offset(ledX + gap, ledY)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF3A130B),
            1f to Color(0xFF240A04),
            center = c2,
            radius = ledR,
        ),
        radius = ledR,
        center = c2,
    )
    val c3 = Offset(ledX + gap * 2f, ledY)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFD9A8A3),
            1f to Color(0xFFBF908B),
            center = c3,
            radius = ledR,
        ),
        radius = ledR,
        center = c3,
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
