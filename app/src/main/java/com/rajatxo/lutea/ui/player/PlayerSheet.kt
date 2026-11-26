package com.rajatxo.lutea.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // ── spmp-style unified anchored drag ────────────────────────────────
    // ONE AnchoredDraggableState drives the whole mini↔player morph. The
    // surface moves 1:1 with the finger and flings/snaps to a page anchor
    // (0 = mini capsule, 1 = full player) — the same continuous gesture
    // spmp's NowPlayingContainer uses, so the open/close feels weighty and
    // physical instead of the old hand-rolled velocity-threshold settle.
    //
    // Anchors are pixel positions: anchor 0 sits at the mini offset (capsule
    // fully visible at the bottom), anchor 1 sits at the full-screen offset
    // (0 travel remaining). requireOffset() returns the live px offset; we
    // invert it into a 0..1 expansion the existing geometry already consumes.
    val swipeState = remember {
        AnchoredDraggableState(
            initialValue = if (expanded) 1 else 0,
            anchors = DraggableAnchors {},
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    // travelPx isn't known until BoxWithConstraints; this fallback mirrors the
    // initial anchor (collapsed = 0 travel done = full travel remaining).
    var travelPxFallback by remember { mutableStateOf(0f) }

    // ── expansion: the 0..1 progress the geometry lerps on ──
    // Computed from the live swipe offset relative to the two anchors, so it
    // tracks the finger 1:1 during a drag and animates during a fling/snap.
    // Before anchors are laid out the offset is NaN — fall back to the
    // initial expanded/collapsed state so the first frame is correct.
    val expansion = remember(expanded) {
        derivedStateOf {
            val raw = swipeState.offset
            if (raw.isNaN()) {
                if (expanded) travelPxFallback else 0f
            } else {
                raw
            }
        }
    }

    // Keep the host's `expanded` boolean in sync with where the sheet
    // actually settled (so back-button/`playerVisible` logic stays correct).
    LaunchedEffect(swipeState) {
        snapshotFlow { swipeState.settledValue }
            .distinctUntilChanged()
            .collect { page ->
                val isExpanded = page == 1
                if (isExpanded != expanded) onExpandedChange(isExpanded)
            }
    }

    // Programmatic open/close (tap the capsule, the close button, the host
    // flipping `expanded`). Uses the PUBLIC suspend anchoredDrag{dragTo}
    // block (NOT spmp's internal animateTo, which is @Suppress'd
    // INVISIBLE_MEMBER and fragile across Compose versions). Animate over a
    // short spring so the non-gesture open feels as smooth as a fling.
    LaunchedEffect(expanded) {
        val targetPage = if (expanded) 1 else 0
        if (swipeState.settledValue == targetPage && !swipeState.isAnimationRunning) {
            return@LaunchedEffect
        }
        scope.launch {
            swipeState.anchoredDrag(targetPage) { anchors, target ->
                val targetOffset = anchors.positionOf(target)
                if (targetOffset.isNaN()) return@anchoredDrag
                // Per-frame dragTo so the surface animates visually (not a
                // single jump). androidx.compose.animation.core.animate drives
                // the value toward the target and calls back each frame.
                val start = swipeState.requireOffset()
                androidx.compose.animation.core.animate(
                    initialValue = start,
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { value, _ ->
                    dragTo(value)
                }
            }
        }
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

            // ── update anchors when the layout settles ──
            // Anchor 0 = the mini capsule's rest position (travelPx below full
            // screen), anchor 1 = full screen (0 offset). Computed in px so the
            // anchored drag is device-density accurate.
            val travelPx = with(density) {
                (maxH - MiniHeight - miniBottomPadding).toPx()
            }.coerceAtLeast(1f)
            travelPxFallback = travelPx
            LaunchedEffect(travelPx) {
                val anchors = DraggableAnchors {
                    0 at travelPx
                    1 at 0f
                }
                swipeState.updateAnchors(anchors)
            }

            // e = 1 at full screen, 0 at capsule. Invert the live offset
            // (which is travelPx at capsule, 0 at full screen).
            val e = (1f - (expansion.value / travelPx)).coerceIn(0f, 1f)
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
                    .anchoredDraggable(
                        state = swipeState,
                        orientation = Orientation.Vertical,
                        // Swipe up = expand. reverseDirection flips the drag
                        // axis so finger-up grows expansion, matching spmp.
                        reverseDirection = true,
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
    // The cover + seamless blend (Mochy Flagship): full-bleed cover that
    // dissolves into the cover-derived colour via Lutea's blendStops. The
    // control cluster is NOT here — it lives in [PlayerControlBar], drawn by
    // the host as a persistent overlay so it stays on the lyrics and queue
    // pages too (Mochy keeps the seek bar + transport on all three pages).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        PlayerCoverBlend()

        // ── Top bar: lyrics button (left) + close (right) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TopBarCircleButton(
                iconRes = R.drawable.ic_lyrics,
                contentDescription = "Lyrics",
                onClick = onLyricsClick,
            )
            TopBarCircleButton(
                iconRes = R.drawable.ic_close_circle,
                contentDescription = "Close player",
                iconSize = 20.dp,
                onClick = onClose,
            )
        }
    }
}

/** A small round icon button for the expanded player's top bar. */
@Composable
private fun TopBarCircleButton(
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

