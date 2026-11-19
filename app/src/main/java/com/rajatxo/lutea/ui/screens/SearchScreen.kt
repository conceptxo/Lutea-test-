package com.rajatxo.lutea.ui.screens

import android.content.Context
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.rajatxo.lutea.audio.TypewriterSound
import com.rajatxo.lutea.data.scanner.MusicScanner
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.CalSansFamily
import com.rajatxo.lutea.ui.foundation.LocalSearchHistoryOverlay
import com.rajatxo.lutea.ui.foundation.LocalSearchSoundOverlay
import com.rajatxo.lutea.ui.icons.LuteaIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH PAGE — a bare search line and the typewriter under it.
//
//  Top to bottom: the search icon + text box (nothing around them, straight on
//  the page — you type here) →
//  the T hammer → the cream carriage on its steel rail. The carriage follows the
//  caret; on every keystroke the hammer taps up under the text like a typebar.
//  The carriage's top slot shows the typing state:
//      green  = actively typing
//      yellow = keyboard open, paused
//      red    = backspace / ✕ pressed
//  Below the rail: the plain list of previous searches.
//  The three dots on the carriage are a speaker grille: always black.
//  Everything is drawn with gradients — no images.
//
//  The ensemble hangs at 57% of the page height, just below the visual centre.
//  Even a tall keyboard starts below that line, so the machine stays clear of
//  it — no IME padding, no movement, ever.
// ═══════════════════════════════════════════════════════════════════════════

private val TypeCream = Color(0xFFFCF2C7)   // butter cream — text + capsule

/** The machine's rail/bottom edge as a fraction of the page height. */
private const val MachineBottomFraction = 0.57f

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
private val LightColor = Color(0xFF5CC8FF)      // cool ocean blue, not warm tungsten
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
    val history = remember { mutableStateListOf<String>() }
    var historyRevision by remember { mutableStateOf(0) }
    val historyOverlay = LocalSearchHistoryOverlay.current
    val soundState = LocalSearchSoundOverlay.current
    val typingSoundEnabled = soundState.typingSoundEnabled
    val typingSoundVolume = soundState.typingSoundVolume
    LaunchedEffect(typingSoundVolume) {
        TypewriterSound.setLevel(typingSoundVolume)
    }
    val statusBarInset = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

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

    // Searches live between launches. A fresh install is seeded with a few
    // examples so the simple list has something to show.
    LaunchedEffect(Unit) {
        val saved = loadSearchHistory(context)
        history.addAll(saved ?: DemoSearchHistory)
        saveSearchHistory(context, history)
    }

    fun addToHistory(entry: String) {
        history.removeAll { it.equals(entry, ignoreCase = true) }
        history.add(0, entry)
        while (history.size > 30) history.removeAt(history.lastIndex)
        saveSearchHistory(context, history)
        historyRevision++
    }

    fun submitSearch(raw: String, song: Song? = null) {
        val entry = (song?.title ?: raw).trim()
        if (entry.isEmpty()) return
        addToHistory(entry)
    }

    fun clearAllHistory() {
        history.clear()
        saveSearchHistory(context, history)
        historyOverlay.simpleClearVisible = false
        historyRevision++
    }

    // The glass button itself lives in LuteaScaffold, on the other side of the
    // layerBackdrop boundary. SearchScreen publishes the real action here.
    DisposableEffect(historyOverlay) {
        historyOverlay.onClearAll = { clearAllHistory() }
        onDispose {
            historyOverlay.onClearAll = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to PageBackground,
                    0.20f to PageBackground,
                    MachineBottomFraction to Color(0xFF0A0A0A),
                    MachineBottomFraction + 0.045f to Color.Black, // solid black under the rail
                    1f to Color.Black,
                ),
            ),
    ) {
        // The machine is parked at 57% of the page. The keyboard normally starts
        // around 63–68%, so there is still a deliberate air gap below the rail
        // even on a small phone with a tall keyboard.
        // Everything above the machine is the result list; it is drawn above
        // the carriage beam so the lamp only ever lights the background.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(MachineBottomFraction),
        ) {
            SearchResults(
                query = query,
                songs = results,
                loaded = libraryReady,
                topInset = statusBarInset,
                nowPlayingId = nowPlaying?.id,
                onPlay = { song ->
                    submitSearch(query, song)
                    PlaybackConnection.playSong(song, results)
                    keyboard?.hide()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // The typewriter beam is drawn by the machine below. Keep
                    // the result rows above it so the lamp stays a background
                    // light instead of washing out content.
                    .zIndex(1f),
            )

            SearchMachine(
                query = query,
                onQueryChange = { query = it },
                onSearch = { submitSearch(query) },
                typingSoundEnabled = typingSoundEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Simple search history below the machine. Every size here is a
        // fraction of the screen (or the machine's own scale).
        val screenH = LocalConfiguration.current.screenHeightDp.dp
        val scale = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.85f, 1.35f)
        val historyBottomInset = screenH * 0.11f
        val simpleFadeHeight = 8.dp
        val simpleTopInset = 4.dp
        // The history area starts right under the carriage instead of a screen-half below it.
        val liftPx = with(LocalDensity.current) {
            ((RailAreaHeight - RailCenterY - 18.dp) * scale).toPx()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .layout { measurable, constraints ->
                    val h = (constraints.maxHeight * (1f - MachineBottomFraction) + liftPx).roundToInt()
                        .coerceIn(0, constraints.maxHeight)
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = h, maxHeight = h),
                    )
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                // Rows that scroll past the top edge vanish here, under the fade
                // below, instead of sliding over the machine.
                .clipToBounds(),
        ) {
            SimpleSearchHistory(
                entries = history,
                resetKey = historyRevision,
                topInset = simpleTopInset,
                bottomInset = historyBottomInset,
                onPick = { entry -> query = entry },
                onClear = { entry ->
                    history.removeAll { it.equals(entry, ignoreCase = true) }
                    saveSearchHistory(context, history)
                },
                onClearVisibilityChange = { historyOverlay.simpleClearVisible = it },
                modifier = Modifier.fillMaxSize(),
            )

            // Same black the page gradient already has here → no colour seam.
            // Drawn after the history content so rows slide in behind it.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(simpleFadeHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black,
                            0.18f to Color.Black,
                            0.30f to Color.Black.copy(alpha = 0.88f),
                            0.42f to Color.Black.copy(alpha = 0.72f),
                            0.54f to Color.Black.copy(alpha = 0.54f),
                            0.66f to Color.Black.copy(alpha = 0.36f),
                            0.77f to Color.Black.copy(alpha = 0.20f),
                            0.88f to Color.Black.copy(alpha = 0.07f),
                            1f to Color.Transparent,
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
 * parked at its fixed 57% line, even while nothing is typed.
 */
@Composable
private fun SearchResults(
    query: String,
    songs: List<Song>,
    loaded: Boolean,
    topInset: Dp,
    nowPlayingId: Long?,
    onPlay: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (query.isNotBlank()) {
            SearchSongList(
                query = query,
                songs = songs,
                loaded = loaded,
                topInset = topInset,
                nowPlayingId = nowPlayingId,
                onPlay = onPlay,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SearchSongList(
    query: String,
    songs: List<Song>,
    loaded: Boolean,
    topInset: Dp,
    nowPlayingId: Long?,
    onPlay: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(query) {
        if (songs.isNotEmpty()) listState.scrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            if (loaded) {
                Text(
                    text = "No songs found",
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 18.dp),
                )
            }
        } else {
            // One clock drives every divider glint. Reading it inside the
            // Canvas keeps the shimmer in the draw phase, not recomposition.
            val shimmer = rememberDividerShimmer(active = listState.isScrollInProgress)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    // The list starts just below the status-bar inset when at
                    // rest. Once it scrolls, rows pass under the transparent
                    // status bar. The bottom mask makes the last row dissolve
                    // into the page instead of ending on a hard edge.
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Black,
                                0.78f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = topInset + 2.dp,
                    bottom = 10.dp,
                ),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SearchResultRow(
                        song = song,
                        playing = song.id == nowPlayingId,
                        onClick = { onPlay(song) },
                    )
                    if (index != songs.lastIndex) {
                        GlassDivider(shimmer)
                    }
                }
            }
        }
    }
}

/** Shared glint used by the song list and the simple history lines. It only
 *  animates while the list is moving, so an idle page costs no frames. */
@Composable
private fun rememberDividerShimmer(active: Boolean): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0.36f) }
    return rememberInfiniteTransition(label = "dividerShimmer")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 3600
                    0f at 0
                    0f at 1800
                    1f at 3200 using LinearEasing
                    1f at 3600
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "dividerShimmerPhase",
        )
}

@Composable
private fun GlassDivider(shimmer: State<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp, end = 10.dp)
            .height(1.dp),
    ) {
        // A bright top edge with a soft lower reflection reads as glass.
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.42f),
                0.35f to Color.White.copy(alpha = 0.16f),
                0.70f to Color.White.copy(alpha = 0.07f),
                1f to Color.White.copy(alpha = 0.20f),
            ),
        )

        // Slow diagonal-style glint, matching the nav text's travelling light.
        val band = size.width * 0.30f
        val head = -band + (size.width + band * 2f) * shimmer.value
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.28f to Color.White.copy(alpha = 0.06f),
                0.50f to Color.White.copy(alpha = 0.58f),
                0.72f to Color.White.copy(alpha = 0.06f),
                1f to Color.Transparent,
                startX = head,
                endX = head + band,
            ),
        )
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
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
                fontSize = 16.sp,
                fontFamily = CalSansFamily,
                fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.48f),
                fontSize = 12.5.sp,
                fontFamily = CalSansFamily,
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
    typingSoundEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val keyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Everything on the rail is designed for a 360dp-wide phone and scales with the screen width.
    val k = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.85f, 1.35f)

    var viewportPx by remember { mutableStateOf(0f) }   // visible width of the text box
    var railPx by remember { mutableStateOf(0f) }       // rail width
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    // The parent can change the query from history/clear while the field is
    // composed. Keep the local TextFieldValue (and caret) in step, but never
    // reset the caret while the user is simply editing the same text.
    LaunchedEffect(query) {
        if (query != fieldValue.text) {
            fieldValue = TextFieldValue(query, TextRange(query.length))
        }
    }

    // TextFieldValue carries the insertion caret; BasicTextField's onTextLayout
    // alone only fires when the layout changes, so a tap in the middle of the
    // string would otherwise leave the carriage at the end.
    val caretOffset = fieldValue.selection.end.coerceIn(0, fieldValue.text.length)
    val caretPx = textLayout?.let { layout ->
        layout.getCursorRect(caretOffset.coerceIn(0, layout.layoutInput.text.length)).left
    } ?: 0f

    // Carriage target: centre the capsule on the actual caret, never off the rail.
    val capsuleWPx = with(density) { CapsuleWidth.toPx() } * k
    val textStartPx = with(density) { TextStart.toPx() }
    val halfGlyphPx = with(density) { 4.2.dp.toPx() }          // caret sits AFTER the last letter
    // The rail's end collars live in the first/last 17dp; the carriage must not cover them.
    val notchClearPx = with(density) { 20.dp.toPx() } * k
    val minLeft = notchClearPx
    val maxLeft = (railPx - capsuleWPx - notchClearPx).coerceAtLeast(minLeft)
    val caretLead = minOf(caretPx, viewportPx) -
        if (fieldValue.text.isEmpty()) 0f else halfGlyphPx
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

    // ── the lamp: only while the search field is focused and the keyboard is up ──
    val lightLevel = remember { Animatable(0f) }
    val release = remember { Animatable(0f) }
    LaunchedEffect(focused, keyboardVisible) {
        if (focused && keyboardVisible) {
            launch {
                release.animateTo(1f, tween(120, easing = LinearOutSlowInEasing))
                release.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 380f))
            }
            lightLevel.animateTo(1f, tween(850, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)))
        } else {
            lightLevel.animateTo(0f, tween(240, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)))
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
                    value = fieldValue,
                    onValueChange = { next ->
                        val oldText = fieldValue.text
                        val nextText = next.text
                        if (nextText.length < oldText.length) {
                            mode = TypeState.CLEARED       // backspace → red
                            clearTick++
                        } else if (nextText != oldText) {
                            mode = TypeState.TYPING        // a new letter → green
                            if (typingSoundEnabled) TypewriterSound.playKey()
                        }
                        fieldValue = next
                        if (nextText != oldText) onQueryChange(nextText)
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
                    onTextLayout = { textLayout = it },
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
                            fieldValue = TextFieldValue("")
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

/** The ocean-blue beam itself: added onto the page (BlendMode.Plus) so the jelly world catches the light. */
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
//  SEARCH HISTORY — a plain list of past queries
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

private const val HistoryPrefs = "lutea_search"
private const val HistoryKey = "history"

/** Older builds stored `{ "q": ..., "c": ... }` objects; accept both shapes. */
private fun loadSearchHistory(context: Context): List<String>? {
    val raw = context.getSharedPreferences(HistoryPrefs, Context.MODE_PRIVATE)
        .getString(HistoryKey, null) ?: return null
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val query = when (val item = arr.opt(i)) {
                    is String -> item
                    is JSONObject -> item.optString("q").ifBlank { item.optString("query") }
                    else -> ""
                }
                if (query.isNotBlank()) add(query)
            }
        }.distinct()
    }.getOrNull()
}

private fun saveSearchHistory(context: Context, entries: List<String>) {
    val arr = JSONArray()
    entries.forEach { arr.put(it) }
    context.getSharedPreferences(HistoryPrefs, Context.MODE_PRIVATE)
        .edit()
        .putString(HistoryKey, arr.toString())
        .apply()
}

/**
 * The no-frills history: just the queries, latest first, separated by thin
 * shimmering lines. It fills the whole area below the machine, scrolls
 * normally, and has no expand state or cards. Each row carries a history glyph,
 * a delete cross and a reuse arrow. The shared clear-all glass capsule appears
 * only once this list is actually scrollable and has been scrolled away from
 * the latest row.
 */
@Composable
private fun SimpleSearchHistory(
    entries: List<String>,
    resetKey: Int,
    topInset: Dp,
    bottomInset: Dp,
    onPick: (String) -> Unit,
    onClear: (String) -> Unit,
    onClearVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val currentOnClearVisibilityChange by rememberUpdatedState(onClearVisibilityChange)
    val scrolledPastTop by remember {
        derivedStateOf {
            val threshold = with(density) { 10.dp.roundToPx() }
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > threshold
        }
    }
    val canScroll by remember {
        derivedStateOf {
            listState.canScrollForward || listState.canScrollBackward
        }
    }
    // Four or five rows stay completely quiet: no clear capsule at all.
    // From six upward, wait until the user scrolls off the newest row.
    val showClearAll = entries.size > 5 && canScroll && scrolledPastTop
    LaunchedEffect(showClearAll) {
        currentOnClearVisibilityChange(showClearAll)
    }
    DisposableEffect(Unit) {
        onDispose { currentOnClearVisibilityChange(false) }
    }
    LaunchedEffect(resetKey) {
        if (entries.isNotEmpty()) listState.scrollToItem(0)
    }

    if (entries.isEmpty()) {
        Box(modifier)
        return
    }

    val shimmer = rememberDividerShimmer(active = listState.isScrollInProgress)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 22.dp,
            end = 16.dp,
            top = topInset,
            bottom = bottomInset + 10.dp,
        ),
    ) {
        itemsIndexed(entries) { index, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = LuteaIcons.History,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(15.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onPick(entry) },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = entry,
                        color = Color.White.copy(alpha = if (index == 0) 0.92f else 0.72f),
                        fontSize = 16.sp,
                        fontFamily = CalSansFamily,
                        fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onClear(entry) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = LuteaIcons.Close,
                        contentDescription = "Remove $entry",
                        tint = Color.White.copy(alpha = 0.48f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onPick(entry) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = LuteaIcons.ArrowUpRight,
                        contentDescription = "Use $entry",
                        tint = Color.White.copy(alpha = 0.62f),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            if (index != entries.lastIndex) {
                HistoryDivider(shimmer)
            }
        }
    }
}

/** Thin hairline between simple-history rows, with a slow travelling glint. */
@Composable
private fun HistoryDivider(shimmer: State<Float>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.6.dp),
    ) {
        drawRect(Color.White.copy(alpha = 0.28f))

        val band = size.width * 0.38f
        val head = -band + (size.width + band * 2f) * shimmer.value
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.28f to Color.White.copy(alpha = 0.05f),
                0.50f to Color.White.copy(alpha = 0.62f),
                0.72f to Color.White.copy(alpha = 0.05f),
                1f to Color.Transparent,
                startX = head,
                endX = head + band,
            ),
        )
    }
}
