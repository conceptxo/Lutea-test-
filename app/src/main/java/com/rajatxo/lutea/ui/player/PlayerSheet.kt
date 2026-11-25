package com.rajatxo.lutea.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.rajatxo.lutea.R
import com.rajatxo.lutea.playback.LoopMode
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.BillieEilishFamily
import com.rajatxo.lutea.ui.components.CalSansFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Same dark ink the nav capsule uses on its white glass. */
private val MiniInk = Color(0xFF1C1C1E)
private val PlayerBg = Color(0xFF0A0A0A)

/** Height of the collapsed capsule. */
private val MiniHeight = 56.dp

data class PlayerNavigation(
    val onAlbumClick: () -> Unit = {},
    val onArtistClick: () -> Unit = {},
)

val LocalPlayerNavigation = staticCompositionLocalOf { PlayerNavigation() }

/**
 * PlayerSheet — the mini player and the full player are ONE surface.
 *
 * The sheet's height, width and corner radius are driven by [expansion]
 * (0 = capsule above the nav, 1 = full screen). Dragging it vertically morphs
 * between the two, so the album art grows from a thumbnail into the hero
 * artwork while the capsule becomes the player. Tapping the capsule expands
 * it; swiping down (or the close button) collapses it.
 *
 * @param miniBottomPadding distance from the screen bottom to the capsule's
 *   bottom edge (the host's nav-bar space). Animates to zero as it expands.
 */
@Composable
fun PlayerSheet(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onLyricsClick: () -> Unit = {},
    onQueueOpen: () -> Unit = {},
    /** 0 = player only, 1 = a lyrics/queue page is fully open. Drives the
     *  control-hide animation (Mochy controlsHiddenState). */
    pageProgress: Float = 0f,
    hidden: Boolean = false,
    backdrop: LayerBackdrop? = null,
    navMatchedWidth: Dp? = null,
    miniBottomPadding: Dp = 0.dp,
) {
    val currentSong by PlaybackConnection.currentSong.collectAsState()
    val isPlaying by PlaybackConnection.isPlaying.collectAsState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expansion = remember { Animatable(if (expanded) 1f else 0f) }

    LaunchedEffect(expanded) {
        expansion.animateTo(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
        )
    }

    AnimatedVisibility(
        visible = (currentSong != null || expanded) && !hidden,
        enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(),
        exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(),
        modifier = modifier,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxW = maxWidth
            val maxH = maxHeight
            val e = expansion.value
            // Smoothstep: the capsule lets go of its shape quickly, then the
            // full-screen sheet settles.
            val eased = e * e * (3f - 2f * e)

            val statusTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
            val navBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

            val capsuleW = navMatchedWidth ?: (maxW - 32.dp)
            val sheetW = lerp(capsuleW, maxW, eased)
            val sheetH = lerp(MiniHeight, maxH, eased)
            val sheetBottom = lerp(miniBottomPadding, 0.dp, eased)
            val sheetShape = RoundedCornerShape(lerp(MiniHeight / 2f, 0.dp, eased))

            // The capsule glass fades out fast; the dark player surface fades
            // in behind it. The mini row and the player body cross-fade.
            val glassAlpha = (1f - e * 4f).coerceIn(0f, 1f)
            val darkAlpha = (e * 2.2f).coerceIn(0f, 1f)
            val miniAlpha = (1f - e * 3.5f).coerceIn(0f, 1f)
            val fullAlpha = ((e - 0.55f) / 0.35f).coerceIn(0f, 1f)

            // Album art travel: capsule thumbnail (left of the row) → hero art
            // centred between the top bar and the controls block.
            val artTop = statusTop + 64.dp
            val artBottom = maxH - navBottom - 268.dp
            val artSpace = (artBottom - artTop).value.coerceAtLeast(120f).dp
            val bigArt = minOf(maxW.value * 0.82f, artSpace.value).dp
            val artSize = lerp(40.dp, bigArt, eased)
            val artCx = lerp(32.dp, maxW / 2f, eased)
            val artCy = lerp(28.dp, (artTop + artBottom) / 2f, eased)

            val travelPx = with(density) {
                (maxH - MiniHeight - miniBottomPadding).toPx()
            }.coerceAtLeast(1f)

            val settle: (Float) -> Unit = { velocity ->
                val target = when {
                    velocity < -1200f -> 1f
                    velocity > 1200f -> 0f
                    expansion.value > 0.4f -> 1f
                    else -> 0f
                }
                scope.launch {
                    expansion.animateTo(
                        target,
                        spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
                    )
                }
                if ((target == 1f) != expanded) onExpandedChange(target == 1f)
            }

            val dragState = rememberDraggableState { dy ->
                scope.launch {
                    expansion.snapTo((expansion.value - dy / travelPx).coerceIn(0f, 1f))
                }
            }

            // ── Collapsed capsule glass, pinned where the mini player lives ──
            if (glassAlpha > 0f) {
                val capsuleShape = RoundedCornerShape(percent = 50)
                val glassSurface = if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { capsuleShape },
                        effects = { blur(16.dp.toPx()) },
                        onDrawSurface = {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.80f),
                                        Color.White.copy(alpha = 0.64f),
                                    )
                                )
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.White.copy(alpha = 0.30f),
                                    0.55f to Color.Transparent,
                                    1f to Color.Transparent,
                                )
                            )
                        },
                    )
                } else {
                    Modifier.background(
                        brush = Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFD8D8DC))
                        ),
                        shape = capsuleShape,
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = miniBottomPadding)
                        .width(capsuleW)
                        .height(MiniHeight)
                        .graphicsLayer { alpha = glassAlpha }
                        .shadow(elevation = 6.dp, shape = capsuleShape, clip = true)
                        .clip(capsuleShape)
                        .then(glassSurface)
                        .border(
                            width = 0.8.dp,
                            brush = Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.95f),
                                0.50f to Color.White.copy(alpha = 0.18f),
                                1f to Color.White.copy(alpha = 0.55f),
                            ),
                            shape = capsuleShape,
                        )
                )
            }

            // ── The morphing sheet ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = sheetBottom)
                    .width(sheetW)
                    .height(sheetH)
                    .clip(sheetShape)
                    .background(PlayerBg.copy(alpha = darkAlpha))
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> settle(velocity) },
                    )
                    // While open, swallow taps on the empty player surface so
                    // they cannot reach the screen underneath.
                    .then(
                        if (expanded) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {} else Modifier
                    )
            ) {
                // Album art — the one element shared by both states.
                Box(
                    modifier = Modifier
                        .offset {
                            val half = with(density) { artSize.toPx() } / 2f
                            IntOffset(
                                (with(density) { artCx.toPx() } - half).roundToInt(),
                                (with(density) { artCy.toPx() } - half).roundToInt(),
                            )
                        }
                        .size(artSize)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF2A2A2A))
                        .border(
                            width = 0.8.dp,
                            color = lerpColor(
                                MiniInk.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.10f),
                                eased,
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    currentSong?.albumArtUri?.let { artUri ->
                        // Fixed decode size: the art grows during the expand
                        // drag, so a layout-sized request would restart on
                        // every frame and settle on a small decode. 1080 keeps
                        // the real metadata art crisp without a huge bitmap.
                        val request = remember(artUri) {
                            ImageRequest.Builder(context)
                                .data(artUri)
                                .size(Size(1080, 1080))
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // ── Mini row: title/artist + play/pause + next ──
                if (miniAlpha > 0f) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = miniAlpha }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onExpandedChange(true) }
                            .padding(start = 64.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSong?.title ?: "",
                                color = MiniInk,
                                fontSize = 14.sp,
                                fontFamily = CalSansFamily,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = currentSong?.artist ?: "",
                                color = MiniInk.copy(alpha = 0.68f),
                                fontSize = 12.sp,
                                fontFamily = CalSansFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        MiniButton(
                            iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            onClick = { PlaybackConnection.playPause() },
                        )
                        Spacer(Modifier.width(8.dp))
                        MiniButton(
                            iconRes = R.drawable.ic_next,
                            contentDescription = "Next",
                            onClick = { PlaybackConnection.next() },
                        )
                    }
                }

                // ── Expanded player body ──
                if (fullAlpha > 0f) {
                    ExpandedPlayerBody(
                        alpha = fullAlpha,
                        onClose = { onExpandedChange(false) },
                        onLyricsClick = onLyricsClick,
                        onQueueOpen = onQueueOpen,
                        pageProgress = pageProgress,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MiniInk.copy(alpha = 0.06f))
            .border(0.6.dp, MiniInk.copy(alpha = 0.10f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MiniInk,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ExpandedPlayerBody(
    alpha: Float,
    onClose: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueOpen: () -> Unit,
    pageProgress: Float,
) {
    val currentSong by PlaybackConnection.currentSong.collectAsState()
    val isPlaying by PlaybackConnection.isPlaying.collectAsState()
    val positionMs by PlaybackConnection.positionMs.collectAsState()
    val durationMs by PlaybackConnection.durationMs.collectAsState()
    val loopMode by PlaybackConnection.loopMode.collectAsState()
    val favourites by com.rajatxo.lutea.data.store.FavouriteStore.favourites.collectAsState()
    val nav = LocalPlayerNavigation.current

    // Poll position only while the player body exists.
    LaunchedEffect(Unit) {
        while (true) {
            PlaybackConnection.pollPosition()
            delay(500)
        }
    }

    // Cover-derived seam colour for the gradient plate (samples the cover's
    // bottom edge, darkens, caches per URI). The cover fades INTO this colour
    // — the Mochy Flagship "cover blends into gradient" look.
    val coverUriStr = currentSong?.albumArtUri?.toString()
    val seamColor by com.rajatxo.lutea.ui.util.rememberCoverThemeColor(
        coverUri = coverUriStr,
        fallbackSeed = currentSong?.id?.toString(),
    )
    val gradientBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to seamColor,
            0.45f to seamColor.darken(0.3f),
            1.0f to Color.Black,
        )
    )

    // Controls hide as a page (lyrics/queue) opens: fade + drop down ~55% of
    // their height — Mochy's controlsHiddenState, driven by pageProgress.
    val controlsHidden = pageProgress.coerceIn(0f, 1f)
    val controlsAlpha = (1f - controlsHidden).coerceIn(0f, 1f)
    val animateControls by androidx.compose.animation.core.animateFloatAsState(
        targetValue = controlsAlpha,
        animationSpec = tween(420, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
        label = "controlsAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(gradientBrush)
    ) {
        // ── Edge-to-edge cover, top of screen, square, full width ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.TopCenter),
        ) {
            currentSong?.albumArtUri?.let { artUri ->
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Cover dissolves into the plate at its bottom edge — no hard seam.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.72f to Color.Transparent,
                                0.92f to seamColor.copy(alpha = 0.55f),
                                1.0f to seamColor,
                            )
                        )
                    )
            )
        }

        // ── Top bar: lyrics button (left) + close (right) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TopCircleButton(
                iconRes = R.drawable.ic_lyrics,
                contentDescription = "Lyrics",
                onClick = onLyricsClick,
            )
            TopCircleButton(
                iconRes = R.drawable.ic_close_circle,
                contentDescription = "Close player",
                iconSize = 20.dp,
                onClick = onClose,
            )
        }

        // ── Bottom control cluster, floating over the gradient plate ──
        // Fades + drops down as a page opens.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    this.alpha = animateControls
                    this.translationY = controlsHidden * (this.size.height * 0.55f)
                }
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title row: title/artist left, heart + chevron-up right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Nothing playing",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontFamily = BillieEilishFamily,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = currentSong != null,
                                onClick = { nav.onAlbumClick() },
                            )
                            .padding(vertical = 2.dp),
                    )
                    Text(
                        text = currentSong?.artist ?: "",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontFamily = CalSansFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = currentSong != null,
                                onClick = { nav.onArtistClick() },
                            )
                            .padding(vertical = 2.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                val isFav = currentSong?.id in favourites
                TopCircleButton(
                    iconRes = if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline,
                    contentDescription = if (isFav) "Unfavourite" else "Favourite",
                    iconSize = 20.dp,
                    tint = if (isFav) Color(0xFFFF6B6B) else Color.White,
                    onClick = { currentSong?.let { com.rajatxo.lutea.data.store.FavouriteStore.toggle(it.id) } },
                )
                Spacer(Modifier.width(6.dp))
                TopCircleButton(
                    iconRes = R.drawable.ic_chevron_up,
                    contentDescription = "Queue",
                    iconSize = 20.dp,
                    onClick = onQueueOpen,
                )
            }

            Spacer(Modifier.height(18.dp))

            // Seek bar — thin at rest, swells under the finger (Mochy style).
            SeekBarRow(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = { PlaybackConnection.seekTo(it) },
            )

            Spacer(Modifier.height(20.dp))

            // Transport: prev / play-pause / next, centred; loop at the edge.
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerButton(
                        iconRes = R.drawable.ic_prev,
                        contentDescription = "Previous",
                        size = 52.dp,
                        onClick = { PlaybackConnection.prev() },
                    )
                    Spacer(Modifier.width(30.dp))
                    PlayerButton(
                        iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        size = 68.dp,
                        filled = true,
                        onClick = { PlaybackConnection.playPause() },
                    )
                    Spacer(Modifier.width(30.dp))
                    PlayerButton(
                        iconRes = R.drawable.ic_next,
                        contentDescription = "Next",
                        size = 52.dp,
                        onClick = { PlaybackConnection.next() },
                    )
                }
                val loopActive = loopMode != LoopMode.OFF
                TopCircleButton(
                    iconRes = if (loopMode == LoopMode.ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                    contentDescription = when (loopMode) {
                        LoopMode.OFF -> "Loop off"
                        LoopMode.ALL -> "Loop queue"
                        LoopMode.ONE -> "Loop this song"
                    },
                    iconSize = 20.dp,
                    tint = Color.White.copy(alpha = if (loopActive) 1f else 0.45f),
                    bgAlpha = if (loopActive) 0.16f else 0f,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { PlaybackConnection.cycleLoopMode() },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Seek bar — thin at rest, swells under touch (Mochy Flagship seek bar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SeekBarRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val fraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    var seeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(0f) }
    // Swell under the finger: 3dp → 6dp track height.
    val trackHeight by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (seeking) 6.dp else 3.dp,
        animationSpec = tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "seekTrack",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(Color.White.copy(alpha = 0.24f))
                .pointerInput(Unit) {
                    androidx.compose.foundation.gestures.detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            seeking = true
                            seekValue = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (seeking) {
                                onSeek((seekValue * safeDuration).roundToLong())
                                seeking = false
                            }
                        },
                        onDragCancel = { seeking = false },
                        onHorizontalDrag = { change, _ ->
                            seekValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (seeking) seekValue else fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(trackHeight / 2))
                    .background(Color.White)
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(if (seeking) (seekValue * safeDuration).roundToLong() else positionMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = CalSansFamily,
            )
            Text(
                text = formatTime(durationMs),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = CalSansFamily,
            )
        }
    }
}

/** A small round icon button used in the top bar and the title row. */
@Composable
private fun TopCircleButton(
    iconRes: Int,
    contentDescription: String,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    bgAlpha: Float = 0.08f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val btnSize = iconSize + 18.dp
    Box(
        modifier = modifier
            .size(btnSize)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Darken a color by [fraction] (0..1) toward black. */
private fun Color.darken(fraction: Float): Color =
    Color(
        red = red * (1f - fraction),
        green = green * (1f - fraction),
        blue = blue * (1f - fraction),
        alpha = alpha,
    )

@Composable
private fun PlayerButton(
    iconRes: Int,
    contentDescription: String,
    size: Dp,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (filled) Color.White else Color.White.copy(alpha = 0.08f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (filled) Color.Black else Color.White,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = (totalSec % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
