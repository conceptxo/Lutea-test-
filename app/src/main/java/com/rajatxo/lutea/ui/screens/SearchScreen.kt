package com.rajatxo.lutea.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import com.rajatxo.lutea.audio.TypewriterSound
import com.rajatxo.lutea.data.scanner.MusicScanner
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.foundation.LocalSearchHistoryOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH PAGE — a bare search line and the typewriter under it.
//
//  Search page: a bare search line, the typewriter under it, and the past
//  searches below the rail as tear-off paper tickets.
//
//  Top to bottom:  the search icon + text box (nothing around them, straight on
//  the page — you type here) →
//  the T hammer → the cream carriage on its steel rail. The carriage follows the
//  caret; on every keystroke the hammer taps up under the text like a typebar.
//  The carriage's top slot shows the typing state:
//      green  = actively typing
//      yellow = keyboard open, paused
//      red    = backspace / ✕ pressed
//  Below the rail: one ticket per search, torn off by its stub to clear it.
//  The three dots on the carriage are a speaker grille: always black.
//  Everything is drawn with gradients — no images.
//
//  The ensemble hangs in the top half of the page, its rail sitting on the
//  horizontal centre line. The keyboard starts below the middle of the screen,
//  so it can never cover it — no IME padding, no movement, ever.
// ═══════════════════════════════════════════════════════════════════════════

private val TypeCream = Color(0xFFFCF2C7)   // butter cream — text + capsule

/** One flat colour behind everything: a very slightly warm near-black, so the light reads like light on paper. */
private val PageBackground = Color(0xFF1E1C1A)

private val CapsuleWidth = 80.dp
private val CapsuleHeight = 34.dp
private val RailAreaHeight = 52.dp          // hammer on top, rail at 30dp, a little air under the pipe
private val RailCenterY = 30.dp             // centre of the rail inside the rail area

// The search box that sits right above the mechanism — just the icon and the
// text, printed straight onto the page. No capsule, no border, no fill.
private val CapsuleBarHeight = 42.dp
private val CapsuleSideMargin = 10.dp
private val CapsuleInnerPadding = 10.dp
private val TextStart = 58.dp               // margin + padding + glyph(18) + gap(20): where typed text begins

// ── the lamp: light released by the T hammer ──────────────────────────────
private val LightColor = Color(0xFFFFE3A3)      // warm tungsten cream
private const val BeamReachFactor = 1.30f       // reach = 1.3x the height above the slot, so it fades past the top
private const val BeamHalfAngleDeg = 66f        // 132 degree spread
private const val BeamLayers = 18               // fallback only (pre-Android 13) — stacked cones

/** Radial fall-off of the light (distance as a fraction of reach -> brightness): bright at the T, fading like real light. */
private val LightFalloff = arrayOf(
    0.00f to 1.00f, 0.03f to 0.96f, 0.08f to 0.82f, 0.15f to 0.62f, 0.25f to 0.40f,
    0.38f to 0.22f, 0.55f to 0.09f, 0.78f to 0.025f, 1.00f to 0.00f,
)

/** Weight of each stacked cone: bell-shaped, so brightness stays full in the middle and eases out smoothly to the edge. */
private val BeamWeights = FloatArray(BeamLayers).also { w ->
    var sum = 0f
    for (i in w.indices) {
        val t = (i + 0.5f) / BeamLayers
        w[i] = t * (1f - t)
        sum += w[i]
    }
    for (i in w.indices) w[i] /= sum
}

private enum class TypeState { IDLE, TYPING, PAUSED, CLEARED }

private val StateGreen = Color(0xFF4DE05F)
private val StateYellow = Color(0xFFFFC62E)
private val StateRed = Color(0xFFFF4A3D)

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

/** Lucide "chevrons-up-down" (MIT): the expand icon printed on every paper. */
private val ExpandGlyph: ImageVector = ImageVector.Builder(
    name = "Expand",
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
        moveTo(7f, 15f)
        lineTo(12f, 20f)
        lineTo(17f, 15f)
        moveTo(7f, 9f)
        lineTo(12f, 4f)
        lineTo(17f, 9f)
    }
}.build()

/** Lucide "chevrons-down-up" (MIT): shown instead while the list is open. */
private val CollapseGlyph: ImageVector = ImageVector.Builder(
    name = "Collapse",
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
        moveTo(7f, 20f)
        lineTo(12f, 15f)
        lineTo(17f, 20f)
        moveTo(7f, 4f)
        lineTo(12f, 9f)
        lineTo(17f, 4f)
    }
}.build()

/** The little curved arrow printed on the stub: "pull me off". */
private val TearGlyph: ImageVector = ImageVector.Builder(
    name = "Tear",
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
        moveTo(6f, 6f)
        curveTo(12f, 6f, 18f, 9f, 18f, 17f)
        moveTo(14f, 14f)
        lineTo(18f, 18f)
        lineTo(22f, 14f)
    }
}.build()

/**
 * AGSL (Android Graphics Shading Language) — this is the thing that paints real light on Android: a fragment
 * shader compiled by Skia at runtime. Every pixel works out its own distance and angle from the slot and fades
 * itself, so the cone is perfectly smooth — no stacked triangles, no banding, no visible layer edges.
 */
private const val BeamShaderSource = """
uniform float2 uApex;
uniform float uReach;
uniform float uHalf;
uniform float uBase;
uniform float uLevel;
uniform float4 uColor;
uniform float uInk;

half4 main(float2 p) {
    float2 d = p - uApex;
    float up = -d.y;
    if (up <= 0.0) {
        return half4(0.0);
    }
    float dist = length(d);
    float t = clamp(dist / max(uReach, 1.0), 0.0, 1.0);
    float ang = atan(abs(d.x), max(up, 0.0001));
    // a real slot has width: the beam leaves already ~uBase wide, then flares open
    float spread = max(up, 0.0001) * tan(min(uHalf, 1.5)) + uBase;
    float u = clamp(abs(d.x) / max(spread, 0.001), 0.0, 1.0);

    float cone = 1.0 - smoothstep(0.42, 1.0, u);
    float radial = pow(1.0 - t, 1.15);
    float spill = (1.0 - smoothstep(0.0, 0.30, t)) * cone * 0.38;
    float lightA = clamp((cone * radial + spill) * uLevel, 0.0, 1.0);

    float inkA = clamp(cone * (1.0 - smoothstep(0.10, 0.44, t)) * uLevel * 3.0, 0.0, 1.0);

    float a = mix(lightA, inkA, clamp(uInk, 0.0, 1.0));
    float dither = (fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0;
    a = clamp(a + dither * (1.0 - clamp(uInk, 0.0, 1.0)), 0.0, 1.0);

    return half4(uColor.rgb * a, a);
}
"""

@Composable
private fun rememberBeamShader(): RuntimeShader? = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching { RuntimeShader(BeamShaderSource) }.getOrNull()
    } else {
        null
    }
}

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val history = remember { mutableStateListOf<SearchHistoryEntry>() }
    val scope = rememberCoroutineScope()
    var historyRevision by remember { mutableStateOf(0) }
    val historyOverlay = LocalSearchHistoryOverlay.current

    // The real library, scanned once off the main thread. Results are just the
    // songs whose title, artist or album matches what is typed above the machine.
    var library by remember { mutableStateOf<List<Song>>(emptyList()) }
    var libraryReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        library = runCatching {
            withContext(Dispatchers.IO) { MusicScanner.scanMusic(context) }
        }.getOrDefault(emptyList())
        libraryReady = true
    }
    val nowPlaying by PlaybackConnection.currentSong.collectAsState()
    val results = remember(query, library) { searchLibrary(library, query) }

    // Searches live between launches. A fresh install is seeded with a few papers
    // so the tear-off stack has something to show before real results are wired.
    // Older string-only entries are given a stable colour and rewritten as objects.
    LaunchedEffect(Unit) {
        val saved = loadSearchHistory(context)
        if (saved == null) {
            history.addAll(
                DemoSearchHistory.map { SearchHistoryEntry(it, stablePaperColor(it)) },
            )
        } else {
            history.addAll(saved)
        }
        saveSearchHistory(context, history)
    }

    fun addToHistory(entry: String, color: Int) {
        history.removeAll { it.query.equals(entry, ignoreCase = true) }
        history.add(0, SearchHistoryEntry(entry, color))
        while (history.size > 30) history.removeAt(history.lastIndex)
        saveSearchHistory(context, history)
        historyRevision++
    }

    /**
     * Work out the paper's colour before it is placed on the stack. A tapped
     * song is stored under its full title, so searching "moon" and tapping
     * "Moonlight" leaves a Moonlight paper. A submitted query keeps the typed
     * text; it takes the colour of its best match, or a random colour when
     * nothing matches.
     */
    fun submitSearch(raw: String, song: Song? = null) {
        val entry = (song?.title ?: raw).trim()
        if (entry.isEmpty()) return
        val previousColor = history.firstOrNull {
            it.query.equals(entry, ignoreCase = true)
        }?.color
        scope.launch {
            val source = song ?: results.firstOrNull()
            val albumRgb = withContext(Dispatchers.IO) {
                source?.albumArtUri?.let { uri -> dominantArtColor(context, uri) }
            }
            val color = albumRgb?.let { rgb -> paperColorFromSwatch(rgb) }
                ?: previousColor
                ?: randomPaperColor()
            addToHistory(entry, color)
        }
    }

    fun clearAllHistory() {
        history.clear()
        saveSearchHistory(context, history)
        historyOverlay.expanded = false
        historyRevision++
    }

    // The glass button itself lives in LuteaScaffold, on the other side of the
    // layerBackdrop boundary. SearchScreen publishes the real action here.
    DisposableEffect(historyOverlay) {
        historyOverlay.onClearAll = { clearAllHistory() }
        onDispose {
            historyOverlay.onClearAll = null
            historyOverlay.expanded = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to PageBackground,
                    0.20f to PageBackground,
                    0.50f to Color(0xFF0A0A0A),
                    0.545f to Color.Black,      // solid black just under the rail
                    1f to Color.Black,
                ),
            ),
    ) {
        // The machine is parked in the top half of the page with its rail sitting
        // on the horizontal centre line. The keyboard starts below the middle on
        // every phone, so it can never reach it — no insets, no movement.
        // Everything above the machine is the result list; it is drawn first so
        // the beam from the carriage lights it.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
        ) {
            SearchResults(
                query = query,
                songs = results,
                loaded = libraryReady,
                nowPlayingId = nowPlaying?.id,
                onPlay = { song ->
                    submitSearch(query, song)
                    PlaybackConnection.playSong(song, results)
                    keyboard?.hide()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            SearchMachine(
                query = query,
                onQueryChange = { query = it },
                onSearch = { submitSearch(query) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Search history: a stack of tear-off papers below the machine. Every size
        // here is a fraction of the screen (or the machine's own scale), never a
        // fixed dp, so it looks the same on a small phone and a tall one.
        val screenH = LocalConfiguration.current.screenHeightDp.dp
        val scale = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.85f, 1.35f)
        val fadeHeight = screenH * 0.05f            // the black fade under the machine
        val deckTopInset = fadeHeight * 0.45f       // first paper starts inside the fade's tail
        val deckBottomInset = screenH * 0.11f       // keeps the last paper clear of the bottom bar
        // The history area starts right under the carriage instead of a screen-half below it.
        val liftPx = with(LocalDensity.current) {
            ((RailAreaHeight - RailCenterY - 18.dp) * scale).toPx()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .layout { measurable, constraints ->
                    val h = (constraints.maxHeight * 0.5f + liftPx).roundToInt()
                        .coerceIn(0, constraints.maxHeight)
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = h, maxHeight = h),
                    )
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                // Papers that scroll past the top edge vanish here, under the fade
                // below, instead of sliding over the machine.
                .clipToBounds(),
        ) {
            SearchHistoryDeck(
                entries = history,
                resetKey = historyRevision,
                bottomInset = deckBottomInset,
                onExpandedChange = { historyOverlay.expanded = it },
                onPick = { entry -> query = entry.query },
                onClear = { entry ->
                    history.removeAll { it.query.equals(entry.query, ignoreCase = true) }
                    saveSearchHistory(context, history)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 14.dp, top = deckTopInset),
            )

            // Same black the page gradient already has here → no colour seam.
            // The top edge is a soft ramp: the black starts transparent at the
            // machine's edge, grows through the first few dp, then holds solid
            // behind the top paper before fading out toward the bottom.
            // Drawn after the deck, so papers always slide in behind it.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(fadeHeight)
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.08f to Color.Black.copy(alpha = 0.08f),
                            0.16f to Color.Black.copy(alpha = 0.28f),
                            0.24f to Color.Black.copy(alpha = 0.58f),
                            0.32f to Color.Black.copy(alpha = 0.84f),
                            0.40f to Color.Black.copy(alpha = 0.97f),
                            0.46f to Color.Black,
                            0.54f to Color.Black.copy(alpha = 0.72f),
                            0.62f to Color.Black.copy(alpha = 0.54f),
                            0.70f to Color.Black.copy(alpha = 0.36f),
                            0.78f to Color.Black.copy(alpha = 0.22f),
                            0.86f to Color.Black.copy(alpha = 0.09f),
                            0.94f to Color.Black.copy(alpha = 0.02f),
                            1.00f to Color.Transparent,
                        ),
                    ),
            )
        }

    }
}

/** Ranked library matches for the bare search line. */
private fun searchLibrary(songs: List<Song>, rawQuery: String): List<Song> {
    val q = rawQuery.trim()
    if (q.isEmpty()) return emptyList()
    return songs.asSequence()
        .filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<Song> { it.title.equals(q, ignoreCase = true) }
                .thenByDescending { it.title.startsWith(q, ignoreCase = true) }
                .thenBy { it.title.lowercase() },
        )
        .take(40)
        .toList()
}

/**
 * The area above the machine. It always occupies its weight so the machine stays
 * parked on the centre line, even while nothing is typed.
 */
@Composable
private fun SearchResults(
    query: String,
    songs: List<Song>,
    loaded: Boolean,
    nowPlayingId: Long?,
    onPlay: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (query.isNotBlank()) {
            val listState = rememberLazyListState()
            LaunchedEffect(query) {
                if (songs.isNotEmpty()) listState.scrollToItem(0)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SONGS",
                        color = Color.White.copy(alpha = 0.36f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    if (songs.isNotEmpty()) {
                        Text(
                            text = songs.size.toString(),
                            color = Color.White.copy(alpha = 0.28f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                if (songs.isEmpty()) {
                    if (loaded) {
                        Text(
                            text = "No songs found",
                            color = Color.White.copy(alpha = 0.38f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 20.dp, top = 18.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 10.dp),
                    ) {
                        items(songs, key = { it.id }) { song ->
                            SearchResultRow(
                                song = song,
                                playing = song.id == nowPlayingId,
                                onClick = { onPlay(song) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    song: Song,
    playing: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF2A2622)),
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (playing) TypeCream else Color.White,
                fontSize = 14.sp,
                fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        if (playing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (bar in floatArrayOf(6f, 12f, 8f)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(bar.dp)
                            .clip(CircleShape)
                            .background(TypeCream.copy(alpha = 0.85f)),
                    )
                    Spacer(Modifier.width(2.dp))
                }
            }
        } else {
            Text(
                text = formatResultDuration(song.duration),
                color = Color.White.copy(alpha = 0.30f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun formatResultDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SearchMachine(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Everything on the rail is designed for a 360dp-wide phone and scales with the screen width.
    val k = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.85f, 1.35f)

    var caretPx by remember { mutableStateOf(0f) }      // caret x inside the text box
    var viewportPx by remember { mutableStateOf(0f) }   // visible width of the text box
    var railPx by remember { mutableStateOf(0f) }       // rail width

    // Carriage target: centre the capsule on the last glyph, never off the rail.
    val capsuleWPx = with(density) { CapsuleWidth.toPx() } * k
    val textStartPx = with(density) { TextStart.toPx() }
    val halfGlyphPx = with(density) { 4.2.dp.toPx() }          // caret sits AFTER the last letter
    // The rail's end collars live in the first/last 17dp; the carriage must not cover them.
    val notchClearPx = with(density) { 20.dp.toPx() } * k
    val minLeft = notchClearPx
    val maxLeft = (railPx - capsuleWPx - notchClearPx).coerceAtLeast(minLeft)
    val caretLead = minOf(caretPx, viewportPx) - if (query.isEmpty()) 0f else halfGlyphPx
    val targetLeft = (textStartPx + caretLead - capsuleWPx / 2f)
        .coerceIn(minLeft, maxLeft)
    val carriageX by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 900f),
        label = "carriage",
    )

    // One strike per keystroke: the typebar taps up under the text, then springs back.
    val strike = remember { Animatable(0f) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            strike.animateTo(1f, tween(40, easing = LinearOutSlowInEasing))
            strike.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 900f))
        }
    }

    // ── typing state → colour of the carriage slot + LEDs ──
    var focused by remember { mutableStateOf(false) }

    // ── the lamp: tapping the search bar makes the T hammer pop up and release the light ──
    val lightLevel = remember { Animatable(0f) }
    val release = remember { Animatable(0f) }
    LaunchedEffect(focused) {
        if (focused) {
            launch {
                release.animateTo(1f, tween(120, easing = LinearOutSlowInEasing))
                release.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 380f))
            }
            lightLevel.animateTo(1f, tween(850, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)))
        } else {
            lightLevel.animateTo(0f, tween(450, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)))
        }
    }
    var mode by remember { mutableStateOf(TypeState.IDLE) }
    var clearTick by remember { mutableStateOf(0) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            if (mode != TypeState.CLEARED) mode = TypeState.TYPING   // green while keys come in
            delay(650)
        }
        if (mode != TypeState.CLEARED) {
            mode = if (focused) TypeState.PAUSED else TypeState.IDLE   // yellow: paused, keyboard still open
        }
    }
    LaunchedEffect(clearTick) {
        if (clearTick > 0) {
            delay(900)                          // red flash after ✕
            if (mode == TypeState.CLEARED) {
                mode = if (focused) TypeState.PAUSED else TypeState.IDLE
            }
        }
    }
    val idleSlot = Color(0xFF1D1C18)
    val slotColor by animateColorAsState(
        targetValue = when (mode) {
            TypeState.TYPING -> StateGreen
            TypeState.PAUSED -> StateYellow
            TypeState.CLEARED -> StateRed
            TypeState.IDLE -> idleSlot
        },
        animationSpec = tween(160),
        label = "slot",
    )

    // Beam geometry lives in the Column's own pixel space. The light comes out of
    // the slot the T stands in — the little bar on the carriage's top edge — never
    // out of the T's head, so the T is a dark silhouette in front of the beam.
    val unit = density.density * k                                   // px per scaled dp
    val railTopPx = with(density) { (CapsuleBarHeight + 2.dp).toPx() }
    val slotPx = railTopPx + with(density) { 13.dp.toPx() } * k      // top edge of the carriage
    val beamPath = remember { Path() }
    val beamShader = rememberBeamShader()
    var colRoot by remember { mutableStateOf(Offset.Zero) }
    val apexProvider: () -> Offset = { Offset(carriageX + capsuleWPx / 2f, slotPx) }
    val reachProvider: () -> Float = {
        val apex = apexProvider()
        // cover the whole page above the machine and fade out past the top edge
        ((apex.y + colRoot.y) * BeamReachFactor).coerceAtLeast(320f * unit)
    }
    val levelProvider: () -> Float = {
        (lightLevel.value * (1f + 0.05f * strike.value.coerceAtLeast(0f))).coerceIn(0f, 1.2f)
    }
    // backspace / ✕ → the lamp itself goes red, matching the carriage slot
    val beamColor by animateColorAsState(
        targetValue = if (mode == TypeState.CLEARED) Color(0xFFFF5347) else LightColor,
        animationSpec = tween(160),
        label = "beamColor",
    )

    Column(
        modifier = modifier
            .onGloballyPositioned { colRoot = it.positionInRoot() }
            .drawBehind {
                val level = levelProvider()
                if (level > 0.003f) {
                    val apex = apexProvider()
                    val reach = reachProvider()
                    if (beamShader != null) {
                        drawShaderBeam(beamShader, apex, reach, level * 0.36f, beamColor, 0f, 12.dp.toPx())
                    } else {
                        drawLightBeam(beamPath, apex, reach, level * 0.35f, unit)
                    }
                }
            },
    ) {
        // ── the search box: just the icon and the text, straight on the page ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CapsuleSideMargin)
                .height(CapsuleBarHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                .padding(horizontal = CapsuleInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SearchGlyph,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { viewportPx = it.width.toFloat() },
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search songs, artists, albums",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.70f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 6f,
                            ),
                        ),
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { next ->
                        if (next.length < query.length) {
                            mode = TypeState.CLEARED       // backspace → red
                            clearTick++
                        } else if (next.length > query.length) {
                            mode = TypeState.TYPING        // a new letter → green
                            TypewriterSound.playKey()
                        }
                        onQueryChange(next)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.75f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 6f,
                        ),
                    ),
                    cursorBrush = SolidColor(TypeCream),
                    onTextLayout = { layout ->
                        val end = layout.layoutInput.text.length
                        caretPx = layout.getCursorRect(query.length.coerceIn(0, end)).left
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                        .onFocusChanged {
                            focused = it.isFocused
                            if (!it.isFocused) {
                                if (mode != TypeState.CLEARED) mode = TypeState.IDLE
                            } else if (mode == TypeState.IDLE) {
                                mode = TypeState.PAUSED
                            }
                        },
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable {
                            mode = TypeState.CLEARED       // red
                            clearTick++
                            onQueryChange("")
                        },
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

        Spacer(Modifier.height(2.dp))

        // ── rail + carriage + hammer: edge to edge, scaled to the screen width ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RailAreaHeight * k)
                .onSizeChanged { railPx = it.width.toFloat() }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                },
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * k, density.fontScale),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawRail(RailCenterY.toPx()) }
                Box(
                    modifier = Modifier
                        .size(CapsuleWidth, RailAreaHeight)
                        .graphicsLayer { translationX = carriageX }
                        .drawBehind { drawCarriage(maxOf(strike.value, release.value), slotColor, lightLevel.value) },
                )
            }
        }
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
 * the top, edge-shaded underside, a dark hammer slot on its underside, three small flat LEDs and a light-grey T hammer
 * with a dark cylinder bar.
 * Local origin = top-left of the capsule's box; the capsule sits low so the hammer has room above it.
 * @param strike 1 right after a keystroke, easing back to 0
 * @param slot colour of the top slot (green typing / yellow paused / red cleared)
 * @param glow 0..1 how much light the T hammer is giving off
 */
private fun DrawScope.drawCarriage(strike: Float, slot: Color, glow: Float = 0f) {
    val w = size.width
    val capH = CapsuleHeight.toPx()
    val s = strike.coerceIn(-0.3f, 1f)
    val capTop = RailCenterY.toPx() - capH / 2f
    val corner = CornerRadius(capH / 2f)
    val cx = w * 0.5f
    // The lamp sits behind the T: with the light on, the metal reads as a dark silhouette.
    val backlit = glow.coerceIn(0f, 1f) * 0.82f
    fun silhouette(c: Color): Color = lerp(c, Color(0xFF0B0A08), backlit)

    // ── smooth shadow cast by the capsule (visible on the pipe) ──
    softShadow(
        topLeft = Offset(4.dp.toPx(), capTop + 4.dp.toPx()),
        size = Size(w - 8.dp.toPx(), capH),
        corner = capH / 2f,
        color = Color.Black,
        spread = 8.dp.toPx(),
        alpha = 0.9f,
    )

    // ── T hammer: sits above the capsule and strikes UP at the search capsule ──
    val lift = 18.dp.toPx() * s
    val barW = 15.dp.toPx()
    val barH = 4.8.dp.toPx()
    val stemW = 3.4.dp.toPx()
    val stemLen = 7.dp.toPx()
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
            0f to silhouette(Color(0xFFD6D4CF)),
            0.15f to silhouette(Color(0xFFC7C5C2)),
            0.80f to silhouette(Color(0xFFC8C8C6)),
            1f to silhouette(Color(0xFFA9AAA4)),
            startX = cx - stemW / 2f,
            endX = cx + stemW / 2f,
        ),
        topLeft = Offset(cx - stemW / 2f, barTop + barH - 0.5.dp.toPx()),
        size = Size(stemW, capTop - (barTop + barH) + 1.5.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to silhouette(Color(0xFF555A5D)),
            0.25f to silhouette(Color(0xFFA5AAAE)),
            0.50f to silhouette(Color(0xFF686D71)),
            0.75f to silhouette(Color(0xFF3F4346)),
            1f to silhouette(Color(0xFF202425)),
            startY = barTop,
            endY = barTop + barH,
        ),
        topLeft = Offset(cx - barW / 2f, barTop),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(1.6.dp.toPx()),
    )
    for (ex in floatArrayOf(cx - barW / 2f + 0.4.dp.toPx(), cx + barW / 2f - 1.2.dp.toPx())) {
        drawRect(
            color = silhouette(Color(0xFFB8BCC0)).copy(alpha = 0.55f),
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

    // ── status slot on the top edge: flat colour, no glow ──
    val slotW = 16.5.dp.toPx()
    val slotH = 3.6.dp.toPx()
    val slotLeft = cx - slotW / 2f
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to lerp(slot, Color.Black, 0.45f),
            0.55f to slot,
            1f to lerp(slot, Color.White, 0.30f),
            startY = capTop,
            endY = capTop + slotH,
        ),
        topLeft = Offset(slotLeft, capTop),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(1.2.dp.toPx()),
    )

    // ── speaker grille: three flat black holes, never coloured ──
    val dotR = 2.3.dp.toPx()
    val dotY = capTop + capH / 2f
    val dotX = w * 0.143f
    val gap = w * 0.113f
    for (i in 0..2) {
        drawCircle(
            color = Color(0xFF141414),
            radius = dotR,
            center = Offset(dotX + gap * i, dotY),
        )
    }
}


// ═══════════════════════════════════════════════════════════════════════════
//  THE LIGHT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Stacks narrow-to-wide cones, each filled with a radial fall-off from the lamp. Because every cone has a
 * slightly different angle, the sum is a beam that is full in the middle and fades gently towards its edges.
 */
private fun DrawScope.drawBeamLayers(
    path: Path,
    apex: Offset,
    reach: Float,
    halfRad: Float,
    brush: Brush,
    strength: Float,
    blend: BlendMode,
) {
    for (i in 0 until BeamLayers) {
        val ang = halfRad * (i + 1) / BeamLayers
        val len = reach / cos(ang)                 // far edge always sits beyond the gradient's end
        val sx = sin(ang) * len
        val sy = cos(ang) * len
        path.reset()
        path.moveTo(apex.x, apex.y)
        path.lineTo(apex.x - sx, apex.y - sy)
        path.lineTo(apex.x + sx, apex.y - sy)
        path.close()
        drawPath(
            path = path,
            brush = brush,
            alpha = (BeamWeights[i] * strength).coerceIn(0f, 1f),
            blendMode = blend,
        )
    }
}

/** The warm beam itself: added onto the page (BlendMode.Plus) so the page brightens like paper under a lamp. */
private fun DrawScope.drawLightBeam(path: Path, apex: Offset, reach: Float, level: Float, unit: Float) {
    val half = (BeamHalfAngleDeg * PI / 180.0).toFloat()

    // faint bounce light around the whole upper page
    clipRect(left = -100000f, top = -100000f, right = 100000f, bottom = apex.y + 2f * unit) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to LightColor.copy(alpha = 0.07f * level),
                0.5f to LightColor.copy(alpha = 0.025f * level),
                1f to Color.Transparent,
                center = apex,
                radius = reach * 0.95f,
            ),
            radius = reach * 0.95f,
            center = apex,
            blendMode = BlendMode.Plus,
        )
    }

    // the beam
    val brush = Brush.radialGradient(
        colorStops = Array(LightFalloff.size) { LightFalloff[it].first to LightColor.copy(alpha = LightFalloff[it].second) },
        center = apex,
        radius = reach,
    )
    drawBeamLayers(path, apex, reach, half, brush, strength = 0.85f * level, blend = BlendMode.Plus)

    // hot spot right at the slot
    val hotR = 46f * unit
    drawCircle(
        brush = Brush.radialGradient(
            0f to LightColor.copy(alpha = 0.45f * level),
            0.4f to LightColor.copy(alpha = 0.16f * level),
            1f to Color.Transparent,
            center = apex,
            radius = hotR,
        ),
        radius = hotR,
        center = apex,
        blendMode = BlendMode.Plus,
    )
}

/**
 * The real thing: one fragment shader fills the whole cone. Every pixel computes its own distance and angle from
 * the slot, so the light is perfectly smooth — no stacked triangles, no banding, no clipped bands.
 */
private fun DrawScope.drawShaderBeam(
    shader: RuntimeShader,
    apex: Offset,
    reach: Float,
    level: Float,
    color: Color,
    ink: Float,
    base: Float,
) {
    shader.setFloatUniform("uApex", apex.x, apex.y)
    shader.setFloatUniform("uReach", reach)
    shader.setFloatUniform("uHalf", (BeamHalfAngleDeg * PI / 180.0).toFloat())
    shader.setFloatUniform("uBase", base)
    shader.setFloatUniform("uLevel", level)
    shader.setFloatUniform("uColor", color.red, color.green, color.blue, 1f)
    shader.setFloatUniform("uInk", ink)
    drawRect(
        brush = ShaderBrush(shader),
        topLeft = Offset(apex.x - reach * 1.5f, apex.y - reach),
        size = Size(reach * 3f, reach),
        blendMode = if (ink > 0.5f) BlendMode.SrcAtop else BlendMode.Plus,
    )
}


// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH HISTORY — one tear-off paper ticket per past search
//
//  A port of the Kokonut UI "tear button": the stub swings on a stiff spring,
//  the perforation gives hole by hole (stick-slip), a tick per hole, then the
//  stub snaps off under gravity. Drag it, or just tap it and it tears itself.
// ═══════════════════════════════════════════════════════════════════════════

private val DemoSearchHistory = listOf(
    "Kesariya",
    "Apocalypse",
    "Nightcall",
    "Luther",
    "Sofia",
    "Starboy",
    "Evergreen",
)

/** One past search and the paper colour it earned. */
private data class SearchHistoryEntry(
    val query: String,
    val color: Int,
)

private val HistoryPaperHeight = 64.dp
private val HistoryStubWidth = 74.dp
private val HistoryBadgeSize = 34.dp
private val HistoryNotchRadius = 7.dp

/**
 * Card-switcher layout: the front paper is full size, every other paper is a
 * slim pill above or below it. Gaps are in dp; pills get narrower with depth.
 */
private val HistoryPillHeight = 36.dp
private val HistoryStackGap = 8.dp
private val HistoryListGap = 12.dp

/** Float lerp (the imported `lerp` is the Color one). */
private fun mixF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Clips a paper to a pill: corner radius follows the height, capped at the paper's own 18dp. */
private val PillClipShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = minOf(size.height / 2f, 18f * density.density)
        return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
    }
}

/** Ink is picked from the paper's real luminance, never from a fixed theme. */
private val PaperInkDark = Color(0xFF201A12)
private val PaperInkLight = Color(0xFFFFFBF0)

private const val HistoryPrefs = "lutea_search"
private const val HistoryKey = "history"

/**
 * A procedural sheet of paper: fine warm grain, darker flecks and short fibres
 * baked into one small repeating shader. It tints the paper underneath, so the
 * same texture reads on every album colour without washing the colour out.
 */
private val PaperTexture: Brush by lazy {
    val side = 224
    val rnd = Random(0x5EEDBEEF)
    val pixels = IntArray(side * side)
    for (i in pixels.indices) {
        val alpha = rnd.nextInt(0, 22)
        val rgb = if (rnd.nextBoolean()) 0xFFFFFF else 0x000000
        pixels[i] = (alpha shl 24) or rgb
    }
    // A handful of darker/brighter fibres; one pixel wide, like real pulp.
    repeat(260) {
        val x0 = rnd.nextInt(side)
        val y0 = rnd.nextInt(side)
        val length = 3 + rnd.nextInt(15)
        val horizontal = rnd.nextBoolean()
        val dark = rnd.nextBoolean()
        val alpha = 10 + rnd.nextInt(15)
        val rgb = if (dark) 0x1A1510 else 0xFFF8E8
        for (k in 0 until length) {
            val x = if (horizontal) (x0 + k) % side else x0
            val y = if (horizontal) y0 else (y0 + k) % side
            pixels[y * side + x] = (alpha shl 24) or rgb
        }
    }
    val bitmap = Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888)
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

private fun relativeLuminance(color: Int): Float {
    fun channel(c: Int): Float {
        val s = (c and 0xFF) / 255f
        return if (s <= 0.04045f) s / 12.92f else ((s + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(color shr 16) +
        0.7152f * channel(color shr 8) +
        0.0722f * channel(color)
}

/** Pure black or warm white, chosen so the paper's own colour stays readable. */
private fun inkForPaper(color: Int): Color =
    if (relativeLuminance(color) > 0.30f) PaperInkDark else PaperInkLight

private fun blendPaperColor(from: Int, to: Int, fraction: Float): Int {
    val t = fraction.coerceIn(0f, 1f)
    fun mix(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}

/**
 * Turn any hue into a real paper tone. Light papers always receive the dark
 * ink, deep ones the warm-white ink; the builder deliberately avoids the muddy
 * middle where neither ink has enough contrast.
 */
private fun buildPaperColor(hue: Float, saturation: Float, dark: Boolean): Int {
    val safeHue = ((hue % 360f) + 360f) % 360f
    val sat = saturation.coerceIn(0.22f, 0.62f)
    val hsv = floatArrayOf(safeHue, sat, if (dark) 0.36f else 0.90f)
    var color = AndroidColor.HSVToColor(hsv)
    if (dark) {
        var guard = 0
        while (relativeLuminance(color) > 0.16f && guard < 10) {
            hsv[2] = (hsv[2] * 0.86f).coerceAtLeast(0.18f)
            color = AndroidColor.HSVToColor(hsv)
            guard++
        }
    } else {
        var mix = 0.10f
        while (relativeLuminance(color) < 0.46f && mix <= 0.80f) {
            color = blendPaperColor(color, 0xFFFFFBF2.toInt(), mix)
            mix += 0.12f
        }
    }
    return color or (0xFF shl 24)
}

private fun randomPaperColor(): Int = buildPaperColor(
    hue = Random.nextFloat() * 360f,
    saturation = 0.30f + Random.nextFloat() * 0.30f,
    dark = Random.nextFloat() < 0.30f,
)

private fun stablePaperColor(query: String): Int {
    val h = query.hashCode()
    return buildPaperColor(
        hue = (((h ushr 3) and 0xFF) / 255f) * 360f,
        saturation = 0.30f + (((h ushr 11) and 0xFF) / 255f) * 0.30f,
        dark = ((h ushr 19) and 1) == 1,
    )
}

/** Keep the cover's hue, but move it into a paper-friendly light or deep tone. */
private fun paperColorFromSwatch(rgb: Int): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(rgb, hsv)
    return buildPaperColor(
        hue = hsv[0],
        saturation = hsv[1],
        dark = relativeLuminance(rgb) < 0.18f,
    )
}

/** Decode a small copy of the cover and pull its overall dominant colour. */
private fun dominantArtColor(context: Context, uri: Uri): Int? {
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (bounds.outWidth / sample > 96 || bounds.outHeight / sample > 96) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null
        try {
            val palette = Palette.from(bitmap).maximumColorCount(24).generate()
            val swatch = palette.dominantSwatch
                ?: palette.mutedSwatch
                ?: palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
            swatch?.rgb
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()
}

/** Tear geometry, same numbers as the original component. */
private const val TearAngle = 34f
private const val TearHoles = 7
private const val TearHold = 0.55f
private const val TearHoldShare = 0.3f
private const val TearStiffness = 900f
private const val TearDamping = 42f
private const val TearReturnStiffness = 520f
private const val TearReturnDamping = 22f
private const val TearGravity = 2600f       // dp / s²
private const val TearFallDistance = 190f   // dp

/** Ragged edge of the tear, offsets in dp along the perforation. */
private val TearFibres = floatArrayOf(
    3.8f, 1.1f, 5.2f, 2.4f, 4.3f, 0.8f, 5.6f, 2.9f, 1.6f, 4.8f,
    3.1f, 0.6f, 5.0f, 2.0f, 4.0f, 1.2f, 5.4f, 2.7f, 1.5f, 4.5f,
)

/**
 * History is a JSON array of `{ "q": ..., "c": ... }` objects. Old versions
 * stored bare strings; those are given a stable colour and upgraded in place
 * the next time the screen saves the list.
 */
private fun loadSearchHistory(context: Context): List<SearchHistoryEntry>? {
    val raw = context.getSharedPreferences(HistoryPrefs, Context.MODE_PRIVATE)
        .getString(HistoryKey, null) ?: return null
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                when (val item = arr.opt(i)) {
                    is String -> if (item.isNotBlank()) {
                        add(SearchHistoryEntry(item, stablePaperColor(item)))
                    }
                    is JSONObject -> {
                        val query = item.optString("q").ifBlank { item.optString("query") }
                        if (query.isNotBlank()) {
                            val savedColor = item.optInt("c", 0)
                            add(
                                SearchHistoryEntry(
                                    query,
                                    if (savedColor == 0) stablePaperColor(query) else savedColor,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }.getOrNull()
}

private fun saveSearchHistory(context: Context, entries: List<SearchHistoryEntry>) {
    val arr = JSONArray()
    entries.forEach { entry ->
        arr.put(
            JSONObject()
                .put("q", entry.query)
                .put("c", entry.color),
        )
    }
    context.getSharedPreferences(HistoryPrefs, Context.MODE_PRIVATE)
        .edit()
        .putString(HistoryKey, arr.toString())
        .apply()
}

/** Raw pull (0..1) → how far the tear has run, holding at each hole. */
private fun stickSlip(pull: Float): Float {
    val scaled = pull.coerceIn(0f, 1f) * TearHoles
    val hole = minOf(scaled.toInt(), TearHoles - 1)
    val f = scaled - hole
    val given = if (f < TearHold) {
        (f / TearHold) * TearHoldShare
    } else {
        TearHoldShare + ((f - TearHold) / (1f - TearHold)) * (1f - TearHoldShare)
    }
    return (hole + given) / TearHoles
}

/**
 * One side of the tear as a polygon: ragged down to `tip`, straight below.
 * `side` is 1 for the body's right edge, -1 for the stub's left edge.
 */
private fun buildTearPath(
    path: Path,
    width: Float,
    height: Float,
    tip: Float,
    side: Int,
    fibres: FloatArray,
) {
    path.reset()
    val far = if (side == 1) 0f else width
    val edge = if (side == 1) width else 0f
    path.moveTo(far, 0f)
    val rows = ceil((tip / height) * fibres.size).toInt()
    for (k in 0..rows) {
        val y = minOf((k / fibres.size.toFloat()) * height, tip)
        val x = edge - side * fibres[k % fibres.size]
        path.lineTo(x, y)
    }
    path.lineTo(edge, tip)
    path.lineTo(edge, height)
    path.lineTo(far, height)
    path.close()
}

/** Paper fill, grain and a soft top-light/underside for a card face. */
private fun DrawScope.drawPaperFace(paper: Brush, texture: Brush) {
    drawRect(brush = paper, size = size)
    drawRect(brush = texture, size = size, alpha = 0.88f)
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.13f),
            0.14f to Color.Transparent,
            0.86f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.09f),
        ),
        size = size,
    )
}

/** Paper face of the stub: fill, printed contents, then the dotted perforation. */
private fun DrawScope.drawStubFace(
    paper: Brush,
    texture: Brush,
    ink: Color,
    notch: Float,
    content: DrawScope.() -> Unit,
) {
    drawPaperFace(paper, texture)
    content()
    drawLine(
        color = ink.copy(alpha = 0.30f),
        start = Offset(1.dp.toPx(), notch + 1.dp.toPx()),
        end = Offset(1.dp.toPx(), size.height - notch - 1.dp.toPx()),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(0.1f, 4.4.dp.toPx()),
            0f,
        ),
    )
}

private class TearSim {
    var angle by mutableFloatStateOf(0f)
    var speed = 0f
    var target = 0f
    var held = false
    var auto = false
    var hole = 0
    var flying by mutableStateOf(false)
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var vx = 0f
    var vy = 0f
    var spin = 0f
    var grabAngle = 0f
    var px = 0f
    var py = 0f
    var pt = 0L
    var pvx = 0f
    var pvy = 0f
}

/** The device's default vibrator, whichever API way is available. */
@Suppress("DEPRECATION")
private fun defaultVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

/**
 * The history as a card-switcher. Collapsed: the front paper is full size and
 * the others are slim pills peeking below (and above, once you have moved on).
 * Dragging or tapping a pill springs it forward while the old front paper
 * shrinks into a pill. Tapping a paper reuses its query; tapping its expand
 * icon opens every search as a full ticket in a scrollable list, and tapping
 * the icon again folds it back. Only a full-size paper can be torn; the rest
 * of the list closes the gap.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryDeck(
    entries: List<SearchHistoryEntry>,
    resetKey: Int,
    bottomInset: Dp,
    onExpandedChange: (Boolean) -> Unit,
    onPick: (SearchHistoryEntry) -> Unit,
    onClear: (SearchHistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnPick by rememberUpdatedState(onPick)
    // Plain float states, not Animatables: the drag loop runs inside Compose's
    // restricted pointer scope. Springs on release are driven from `scope`.
    val scroll = remember { mutableFloatStateOf(0f) }       // stack position, in cards
    val listScroll = remember { mutableFloatStateOf(0f) }   // list position, in px
    val viewportPx = remember { mutableFloatStateOf(0f) }
    val expandT = remember { mutableFloatStateOf(0f) }      // 0 = stack, 1 = list (follows the finger)
    val closeP = remember { Animatable(0f) }                // 0..1 while a torn paper closes up
    var expanded by remember { mutableStateOf(false) }
    var closingIndex by remember { mutableStateOf(-1) }
    var snapJob by remember { mutableStateOf<Job?>(null) }
    var expandJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(expanded) { currentOnExpandedChange(expanded) }

    val cardHeightPx = with(density) { HistoryPaperHeight.toPx() }
    val pillPx = with(density) { HistoryPillHeight.toPx() }
    val gapPx = with(density) { HistoryStackGap.toPx() }
    val listGapPx = with(density) { HistoryListGap.toPx() }
    val bottomInsetPx = with(density) { bottomInset.toPx() }
    val maxIndex = entries.lastIndex.coerceAtLeast(0)

    val activeIndex by remember {
        derivedStateOf {
            scroll.floatValue.roundToInt()
                .coerceIn(0, entries.lastIndex.coerceAtLeast(0))
        }
    }

    // Which papers are worth composing right now (stack neighbours, list window, or both mid-morph).
    val range by remember(maxIndex) {
        derivedStateOf {
            val a = scroll.floatValue.roundToInt().coerceIn(0, maxIndex)
            var lo = (a - 2).coerceAtLeast(0)
            var hi = (a + 2).coerceAtMost(maxIndex)
            val t = expandT.floatValue
            if (expanded || t > 0.001f) {
                val step = cardHeightPx + listGapPx
                val l0 = (floor(listScroll.floatValue / step).toInt() - 1).coerceIn(0, maxIndex)
                val l1 = (ceil((listScroll.floatValue + viewportPx.floatValue) / step).toInt() + 1)
                    .coerceIn(0, maxIndex)
                if (t > 0.985f) {
                    lo = l0
                    hi = l1
                } else {
                    lo = minOf(lo, l0)
                    hi = maxOf(hi, l1)
                }
            }
            lo..hi
        }
    }

    fun maxListScroll(): Float =
        (entries.size * (cardHeightPx + listGapPx) - listGapPx - viewportPx.floatValue + bottomInsetPx)
            .coerceAtLeast(0f)

    // Every new search puts its paper on top of the deck.
    LaunchedEffect(resetKey) {
        snapJob?.cancel()
        scroll.floatValue = 0f
        listScroll.floatValue = 0f
    }

    // A torn paper shortens the stack; never leave the deck pointing past it.
    LaunchedEffect(entries.size) {
        val max = entries.lastIndex.coerceAtLeast(0).toFloat()
        if (scroll.floatValue > max) {
            snapJob?.cancel()
            scroll.floatValue = max
        }
        val maxList = maxListScroll()
        if (listScroll.floatValue > maxList) listScroll.floatValue = maxList
        if (entries.isEmpty()) {
            expanded = false
            expandT.floatValue = 0f
        }
    }

    // Stack <-> list morph: one soft spring with a tiny overshoot, starting from
    // wherever the finger left it.
    fun settleExpand(open: Boolean) {
        expandJob?.cancel()
        expanded = open
        expandJob = scope.launch {
            animate(
                initialValue = expandT.floatValue,
                targetValue = if (open) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
            ) { value, _ ->
                expandT.floatValue = value
            }
        }
    }

    fun focusIndex(target: Int) {
        snapJob?.cancel()
        snapJob = scope.launch {
            animate(
                initialValue = scroll.floatValue,
                targetValue = target.coerceIn(0, maxIndex).toFloat(),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 300f),
            ) { value, _ ->
                scroll.floatValue = value
            }
        }
    }

    fun expandDeckAt(index: Int) {
        snapJob?.cancel()
        val target = index.coerceIn(0, maxIndex)
        scroll.floatValue = target.toFloat()
        val step = cardHeightPx + listGapPx
        listScroll.floatValue = (target * step).coerceIn(0f, maxListScroll())
        settleExpand(true)
    }

    fun collapseDeck(to: Int) {
        snapJob?.cancel()
        scroll.floatValue = to.coerceIn(0, maxIndex).toFloat()
        settleExpand(false)
    }

    /** A torn paper folds shut while everything after it slides up, then it is really removed. */
    fun startClose(entry: SearchHistoryEntry) {
        if (closingIndex >= 0) return
        val idx = entries.indexOfFirst { it.query == entry.query }
        if (idx < 0) return
        closingIndex = idx
        scope.launch {
            val newMaxIndex = (entries.size - 2).coerceAtLeast(0).toFloat()
            if (scroll.floatValue > newMaxIndex) {
                launch {
                    animate(
                        initialValue = scroll.floatValue,
                        targetValue = newMaxIndex,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
                    ) { value, _ -> scroll.floatValue = value }
                }
            }
            val newMaxList = ((entries.size - 1) * (cardHeightPx + listGapPx) - listGapPx -
                viewportPx.floatValue + bottomInsetPx).coerceAtLeast(0f)
            if (listScroll.floatValue > newMaxList) {
                launch {
                    animate(
                        initialValue = listScroll.floatValue,
                        targetValue = newMaxList,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 300f),
                    ) { value, _ -> listScroll.floatValue = value }
                }
            }
            closeP.snapTo(0f)
            closeP.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            onClear(entry)
            closingIndex = -1
            closeP.snapTo(0f)
        }
    }

    // ── geometry: one paper's place in the stack, the list, or anywhere between ──
    fun relOf(i: Int): Float {
        val c = closingIndex
        val shift = if (c in 0 until i) closeP.value else 0f
        return (i - scroll.floatValue) - shift
    }

    fun eOf(i: Int): Float {
        val stackE = (1f - abs(relOf(i))).coerceIn(0f, 1f)
        return mixF(stackE, 1f, expandT.floatValue)
    }

    fun hOf(i: Int): Float = mixF(pillPx, cardHeightPx, eOf(i))

    fun wFractionOf(i: Int): Float {
        val stackW = 1f - 0.07f * abs(relOf(i)).coerceAtMost(3f)
        return mixF(stackW, 1f, eOf(i)).coerceAtMost(1f)
    }

    fun yOf(i: Int): Float {
        val rel = relOf(i)
        // Once you have moved past the first paper, the one you left sits above as a pill.
        val base = (pillPx + gapPx) * scroll.floatValue.coerceIn(0f, 1f)
        val raw = when {
            rel >= 1f -> (cardHeightPx + gapPx) + (rel - 1f) * (pillPx + gapPx)
            rel >= 0f -> rel * (cardHeightPx + gapPx)
            else -> rel * (pillPx + gapPx)
        }
        val c = closingIndex
        val shift = if (c in 0 until i) closeP.value else 0f
        val listY = i * (cardHeightPx + listGapPx) - listScroll.floatValue -
            shift * (cardHeightPx + listGapPx)
        return mixF(base + raw, listY, expandT.floatValue)
    }

    fun alphaOf(i: Int): Float {
        val rel = relOf(i)
        // Exactly three papers show in the stack: a pill above, the big one, a pill below.
        val stackA = (2f - abs(rel)).coerceIn(0f, 1f)
        val fade = if (i == closingIndex) 1f - closeP.value else 1f
        return (mixF(stackA, 1f, expandT.floatValue) * fade).coerceIn(0f, 1f)
    }

    if (entries.isEmpty()) {
        Box(modifier)
        return
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .onSizeChanged { viewportPx.floatValue = it.height.toFloat() }
            .pointerInput(entries.size) {
                if (entries.size < 2) return@pointerInput
                awaitEachGesture {
                    // Watched on the Initial pass: the papers' own tap handlers would
                    // otherwise swallow the touch before the deck ever saw it.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    // A finger that lands on a tearable stub belongs to the stub.
                    if (down.position.x >= size.width - HistoryStubWidth.toPx()) {
                        for (i in range) {
                            if (!(expanded || i == activeIndex)) continue
                            val top = yOf(i)
                            if (eOf(i) > 0.9f && down.position.y >= top &&
                                down.position.y <= top + hOf(i)
                            ) {
                                return@awaitEachGesture
                            }
                        }
                    }
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var previousY = down.position.y
                    var travel = 0f
                    var dragging = false
                    val listMode = expanded
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        if (!change.pressed) break
                        val dy = change.position.y - previousY
                        previousY = change.position.y
                        travel += dy
                        if (!dragging && abs(travel) > viewConfiguration.touchSlop) {
                            dragging = true
                        }
                        if (dragging) {
                            snapJob?.cancel()
                            if (listMode) {
                                listScroll.floatValue =
                                    (listScroll.floatValue - dy).coerceIn(0f, maxListScroll())
                            } else {
                                // Swipe up/down: the next paper grows while this one shrinks.
                                scroll.floatValue = (scroll.floatValue - dy / cardHeightPx)
                                    .coerceIn(0f, maxIndex.toFloat())
                            }
                            change.consume()
                        }
                    }
                    if (dragging) {
                        val vy = tracker.calculateVelocity().y
                        snapJob?.cancel()
                        if (listMode) {
                            val target = (listScroll.floatValue - vy * 0.22f)
                                .coerceIn(0f, maxListScroll())
                            snapJob = scope.launch {
                                animate(
                                    initialValue = listScroll.floatValue,
                                    targetValue = target,
                                    animationSpec = tween(520, easing = LinearOutSlowInEasing),
                                ) { value, _ ->
                                    listScroll.floatValue = value
                                }
                            }
                        } else {
                            val fling = if (abs(vy) > 650f) -sign(vy) else 0f
                            val target = (scroll.floatValue.roundToInt() + fling)
                                .coerceIn(0f, maxIndex.toFloat())
                            snapJob = scope.launch {
                                animate(
                                    initialValue = scroll.floatValue,
                                    targetValue = target,
                                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
                                ) { value, _ ->
                                    scroll.floatValue = value
                                }
                            }
                        }
                    }
                }
            },
    ) {
        val active = activeIndex
        for (i in range) {
            val entry = entries.getOrNull(i) ?: continue
            key(entry.query) {
                Box(
                    modifier = Modifier
                        // Fixed order (newer on top): it never flips while papers swap
                        // sizes, so shadows cannot pop over a neighbour mid-animation.
                        .zIndex(1000f - i)
                        // Outer layer: where the paper sits and how visible it is.
                        .graphicsLayer {
                            translationY = yOf(i)
                            alpha = alphaOf(i)
                        }
                        .drawBehind {
                            softShadow(
                                topLeft = Offset(3.dp.toPx(), 7.dp.toPx()),
                                size = Size(size.width - 6.dp.toPx(), size.height),
                                corner = 18.dp.toPx(),
                                color = Color.Black,
                                spread = 12.dp.toPx(),
                                alpha = mixF(0.30f, 0.70f, eOf(i).coerceIn(0f, 1f)),
                                steps = 8,
                            )
                        }
                        // Inner layer: trims a collapsed paper to its pill. A full-size
                        // paper is never clipped so the torn stub can swing and fall freely.
                        .graphicsLayer {
                            clip = eOf(i) < 0.995f
                            shape = PillClipShape
                        }
                        // The paper itself is always laid out full size; the card just
                        // shows a slice of it (its middle line), so the query text stays
                        // readable as a pill and nothing is ever squashed.
                        .layout { measurable, constraints ->
                            val fullW = constraints.maxWidth
                            val fullH = cardHeightPx.roundToInt()
                            val w = (fullW * wFractionOf(i)).roundToInt().coerceIn(0, fullW)
                            val h = hOf(i).roundToInt().coerceAtLeast(0)
                            val placeable = measurable.measure(Constraints.fixed(fullW, fullH))
                            layout(w, h) {
                                placeable.place((w - fullW) / 2, (h - fullH) / 2)
                            }
                        },
                ) {
                    SearchHistoryPaper(
                        entry = entry,
                        interactive = expanded || i == active,
                        expanded = expanded,
                        expansion = { eOf(i) },
                        onPick = {
                            if (!expanded && i != active) focusIndex(i)
                            currentOnPick(entry)
                        },
                        onToggle = {
                            if (expanded) collapseDeck(i)
                            else expandDeckAt(i)
                        },
                        onClear = { startClose(entry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryPaper(
    entry: SearchHistoryEntry,
    interactive: Boolean,
    expanded: Boolean,
    expansion: () -> Float,
    onPick: () -> Unit,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val view = LocalView.current
    val currentOnClear by rememberUpdatedState(onClear)
    val currentOnPick by rememberUpdatedState(onPick)
    val currentOnToggle by rememberUpdatedState(onToggle)
    val bodyTapSource = remember { MutableInteractionSource() }
    val expandTapSource = remember { MutableInteractionSource() }
    val stubTapSource = remember { MutableInteractionSource() }
    val sim = remember { TearSim() }
    var wakeToken by remember { mutableStateOf(0) }

    // A real Vibrator, not View haptics: short one-shots are felt even when the
    // system's touch-feedback haptics are off, which is what the tear needs.
    val vibrator = remember(context) { defaultVibrator(context) }

    val fibrePx = remember(density) {
        FloatArray(TearFibres.size) { TearFibres[it] * density.density }
    }
    val notchPx = with(density) { HistoryNotchRadius.toPx() }
    val fallPx = TearFallDistance * density.density
    val heightPx = with(density) { HistoryPaperHeight.toPx() }

    val bodyTear = remember { Path() }
    val stubTear = remember { Path() }
    val bodyShape = remember {
        RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 1.dp, bottomEnd = 1.dp)
    }
    val stubShape = remember {
        RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp, topStart = 1.dp, bottomStart = 1.dp)
    }

    // Every paper carries its own colour. The cover colour is softened into a
    // paper tint; the ink is computed from that final colour, not guessed.
    val base = Color(entry.color)
    val paperTop = remember(entry.color) { lerp(base, Color.White, 0.10f) }
    val paperBottom = remember(entry.color) { lerp(base, Color.Black, 0.09f) }
    val paperBrush = remember(entry.color) {
        Brush.verticalGradient(
            0f to paperTop,
            0.48f to base,
            1f to paperBottom,
        )
    }
    val ink = remember(entry.color) { inkForPaper(entry.color) }
    val texture = PaperTexture

    fun buzz(ms: Long, amplitude: Int) {
        val v = vibrator
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(ms, amplitude))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun tick() = buzz(12L, 200)

    // While the paper is held, springing back, or flying, this advances the sim
    // once a frame. Reads happen inside graphicsLayer / drawWithContent, so the
    // ticket animates without recomposing the stack on every frame.
    LaunchedEffect(wakeToken) {
        val d = density.density
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 1f / 120f
            else ((now - last) / 1_000_000_000f).coerceIn(1f / 240f, 0.05f)
            last = now

            if (sim.flying) {
                sim.vy += TearGravity * d * dt
                sim.x += sim.vx * dt
                sim.y += sim.vy * dt
                sim.angle += sim.spin * dt
                if (sim.y > fallPx) {
                    currentOnClear()
                    break
                }
            } else {
                val active = sim.held || sim.auto
                if (sim.auto) {
                    sim.target = (sim.target + dt * 60f).coerceAtMost(TearAngle * 1.05f)
                }
                val stiffness = if (active) TearStiffness else TearReturnStiffness
                val damping = if (active) TearDamping else TearReturnDamping
                val force = (sim.target - sim.angle) * stiffness - sim.speed * damping
                sim.speed += force * dt
                sim.angle += sim.speed * dt

                // A tick for each hole that gives.
                val hole = ((sim.angle / TearAngle).coerceIn(0f, 1f) * TearHoles).toInt()
                if (hole > sim.hole) {
                    sim.hole = hole
                    tick()
                }

                if (active && sim.angle >= TearAngle * 0.995f) {
                    sim.held = false
                    buzz(32L, 255)
                    // Leaves with the swing it had, plus the hand's throw.
                    val rad = sim.angle * PI.toFloat() / 180f
                    val tangential = (sim.speed * PI.toFloat() / 180f) * heightPx
                    sim.x = 0f
                    sim.y = 0f
                    sim.vx = cos(rad) * tangential +
                        sim.pvx.coerceIn(-900f, 900f) * 0.6f + 30f * d
                    sim.vy = sin(rad) * tangential +
                        sim.pvy.coerceIn(-900f, 900f) * 0.6f - 45f * d
                    sim.spin = sim.speed * 0.6f + 140f
                    sim.flying = true
                } else if (!active && abs(sim.angle) < 0.02f && abs(sim.speed) < 0.05f) {
                    sim.angle = 0f
                    sim.speed = 0f
                    sim.hole = 0
                    break
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HistoryPaperHeight),
    ) {
        // ── the body: chip and query; its right edge tears away ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = HistoryStubWidth)
                .clip(bodyShape)
                .clickable(
                    interactionSource = bodyTapSource,
                    indication = null,
                ) { currentOnPick() }
                .drawWithContent {
                    val tip = (sim.angle / TearAngle).coerceIn(0f, 1f) * size.height
                    if (tip > 0.5f) {
                        buildTearPath(bodyTear, size.width, size.height, tip, 1, fibrePx)
                        // The paper itself lives inside the ragged clip so the
                        // grain vanishes with the torn edge instead of leaving
                        // a clean, coloured rectangle behind.
                        clipPath(bodyTear) {
                            drawPaperFace(paperBrush, texture)
                            this@drawWithContent.drawContent()
                        }
                    } else {
                        drawPaperFace(paperBrush, texture)
                        this@drawWithContent.drawContent()
                    }
                    // Ticket notches: the black behind the paper shows through
                    // the seam, exactly where the perforation meets the edges.
                    drawCircle(Color.Black, notchPx, Offset(size.width, 0f))
                    drawCircle(Color.Black, notchPx, Offset(size.width, size.height))
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        // The badge shrinks with the paper: ~60% size as a pill.
                        .layout { measurable, _ ->
                            val e = expansion().coerceIn(0f, 1f)
                            val side = (HistoryBadgeSize.toPx() * mixF(0.60f, 1f, e)).roundToInt()
                            val placeable = measurable.measure(Constraints.fixed(side, side))
                            layout(side, side) { placeable.place(0, 0) }
                        }
                        .clip(CircleShape)
                        .background(ink.copy(alpha = 0.10f))
                        .clickable(
                            interactionSource = expandTapSource,
                            indication = null,
                        ) { currentOnToggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(
                        targetState = expanded,
                        animationSpec = tween(220),
                        label = "expandGlyph",
                    ) { isOpen ->
                        Icon(
                            imageVector = if (isOpen) CollapseGlyph else ExpandGlyph,
                            contentDescription = if (isOpen) "Collapse history" else "Expand history",
                            tint = ink.copy(alpha = 0.75f),
                            modifier = Modifier.fillMaxSize(0.47f),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Text(
                        text = entry.query,
                        color = ink,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                    )
                }
            }
        }

        // ── the stub: swings on the foot of the perforation, then falls ──
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(HistoryStubWidth)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 1f)
                    rotationZ = sim.angle
                    if (sim.flying) {
                        translationX = sim.x
                        translationY = sim.y
                        val fade = 1f - ((sim.y - fallPx * 0.5f) / (fallPx * 0.5f))
                        alpha = fade.coerceIn(0f, 1f)
                    }
                }
                .clip(stubShape)
                .drawWithContent {
                    val tip = (sim.angle / TearAngle).coerceIn(0f, 1f) * size.height
                    if (tip > 0.5f) {
                        buildTearPath(stubTear, size.width, size.height, tip, -1, fibrePx)
                        clipPath(stubTear) {
                            drawStubFace(paperBrush, texture, ink, notchPx) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    } else {
                        drawStubFace(paperBrush, texture, ink, notchPx) {
                            this@drawWithContent.drawContent()
                        }
                    }
                    drawCircle(Color.Black, notchPx, Offset(0f, 0f))
                    drawCircle(Color.Black, notchPx, Offset(0f, size.height))
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val e = expansion().coerceIn(0f, 1f)
                    alpha = e * e
                },
            ) {
                Icon(
                    imageVector = TearGlyph,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.68f),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Clear",
                    color = ink.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Gesture surface: a still sibling over the resting stub. If the gesture
        // lived on the rotating stub, Compose would feed the pointer back through
        // the rotation and the angle would chase its own tail. Only the front
        // paper owns this surface; the papers behind it are a pure visual stack.
        if (!interactive) {
            // A paper waiting behind the front one: tapping anywhere on it brings it forward.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(HistoryStubWidth)
                    .clickable(
                        interactionSource = stubTapSource,
                        indication = null,
                    ) { currentOnToggle() },
            )
        }
        if (interactive) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(HistoryStubWidth)
                    .pointerInput(entry.query) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (sim.flying) return@awaitEachGesture
                            down.consume()
                            val h = size.height.toFloat()
                            sim.held = true
                            sim.auto = false
                            sim.grabAngle =
                                atan2(down.position.y - h, down.position.x) * 180f / PI.toFloat() -
                                sim.angle
                            sim.px = down.position.x
                            sim.py = down.position.y
                            sim.pt = down.uptimeMillis
                            sim.pvx = 0f
                            sim.pvy = 0f
                            sim.hole = ((sim.angle / TearAngle).coerceIn(0f, 1f) * TearHoles).toInt()
                            wakeToken++

                            var dragged = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.pressed) {
                                    val pos = change.position
                                    val dt = (change.uptimeMillis - sim.pt).coerceAtLeast(1L)
                                    sim.pvx = sim.pvx * 0.5f + ((pos.x - sim.px) / dt) * 500f
                                    sim.pvy = sim.pvy * 0.5f + ((pos.y - sim.py) / dt) * 500f
                                    sim.px = pos.x
                                    sim.py = pos.y
                                    sim.pt = change.uptimeMillis
                                    if ((pos - down.position).getDistance() > 8.dp.toPx()) {
                                        dragged = true
                                    }
                                    val raw =
                                        atan2(pos.y - h, pos.x) * 180f / PI.toFloat() - sim.grabAngle
                                    // Pushing the stub back into the body only gives a little.
                                    val pull = if (raw < 0f) raw * 0.15f else raw
                                    sim.target = if (pull >= TearAngle) {
                                        TearAngle * 1.05f
                                    } else {
                                        stickSlip(pull.coerceAtLeast(0f) / TearAngle) * TearAngle +
                                            minOf(pull, 0f)
                                    }
                                    change.consume()
                                } else {
                                    sim.held = false
                                    if (!sim.flying) {
                                        if (!dragged &&
                                            change.uptimeMillis - down.uptimeMillis < 320L
                                        ) {
                                            sim.auto = true   // a tap tears it hole by hole
                                        }
                                        sim.target = 0f
                                    }
                                    change.consume()
                                    break
                                }
                            }
                        }
                    },
            )
        }
    }
}
