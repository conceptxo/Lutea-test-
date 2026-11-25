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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
) {
    val currentSong by PlaybackConnection.currentSong.collectAsState()
    val isPlaying by PlaybackConnection.isPlaying.collectAsState()
    val positionMs by PlaybackConnection.positionMs.collectAsState()
    val durationMs by PlaybackConnection.durationMs.collectAsState()
    val loopMode by PlaybackConnection.loopMode.collectAsState()
    val nav = LocalPlayerNavigation.current

    // Poll position only while the player body exists.
    LaunchedEffect(Unit) {
        while (true) {
            PlaybackConnection.pollPosition()
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Top: handle + lyrics + close ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLyricsClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lyrics),
                    contentDescription = "Lyrics",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_circle),
                    contentDescription = "Close player",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // The hero album art is drawn by the sheet; this keeps its space.
        Spacer(Modifier.weight(1f))

        // ── Bottom: title + artist + seek bar + controls ──
        Text(
            text = currentSong?.title ?: "Nothing playing",
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = BillieEilishFamily,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = currentSong != null,
                    onClick = { nav.onAlbumClick() },
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = currentSong?.artist ?: "",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 16.sp,
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
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Spacer(Modifier.height(20.dp))

        val safeDuration = durationMs.coerceAtLeast(1L)
        val seekFraction = (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        var seeking by remember { mutableStateOf(false) }
        var seekValue by remember { mutableStateOf(0f) }

        Slider(
            value = if (seeking) seekValue else seekFraction,
            onValueChange = { value ->
                seeking = true
                seekValue = value
            },
            onValueChangeFinished = {
                if (seeking) {
                    PlaybackConnection.seekTo((seekValue * safeDuration).roundToLong())
                    seeking = false
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(if (seeking) (seekValue * safeDuration).roundToLong() else positionMs),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = CalSansFamily,
            )
            Text(
                text = formatTime(durationMs),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontFamily = CalSansFamily,
            )
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerButton(
                    iconRes = R.drawable.ic_prev,
                    contentDescription = "Previous",
                    size = 56.dp,
                    onClick = { PlaybackConnection.prev() },
                )
                Spacer(Modifier.width(28.dp))
                PlayerButton(
                    iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    size = 72.dp,
                    filled = true,
                    onClick = { PlaybackConnection.playPause() },
                )
                Spacer(Modifier.width(28.dp))
                PlayerButton(
                    iconRes = R.drawable.ic_next,
                    contentDescription = "Next",
                    size = 56.dp,
                    onClick = { PlaybackConnection.next() },
                )
            }

            val loopActive = loopMode != LoopMode.OFF
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (loopActive) 0.16f else 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { PlaybackConnection.cycleLoopMode() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (loopMode == LoopMode.ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
                    ),
                    contentDescription = when (loopMode) {
                        LoopMode.OFF -> "Loop off"
                        LoopMode.ALL -> "Loop queue"
                        LoopMode.ONE -> "Loop this song"
                    },
                    tint = Color.White.copy(alpha = if (loopActive) 1f else 0.45f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

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
