package com.rajatxo.lutea.ui.player

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
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
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.CalSansFamily
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
 * PlayerSheet — ONE surface that IS the mini player collapsed and the full
 * player expanded. There is exactly one cover-art element: a single
 * [AsyncImage] that is the 40dp thumbnail in the capsule and grows — corner
 * radius and all — into the full-width square hero cover at the top of the
 * player. No second [PlayerCoverBlend], no full-screen blur, no offscreen
 * compositing on the transition's critical path. That is what makes the
 * expand/shrink read as "the mini player itself is growing", and what removes
 * the lag: the GPU only touches one image, and it is layout-driven (offset +
 * size), not a per-frame filter.
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
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
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

            // ── The ONE shared cover ──
            // Mini state: 40dp thumbnail, 12dp in from the capsule's left,
            // vertically centred in the 56dp capsule, 8dp corner radius.
            // Full state: full-width square, top edge flush with the sheet's
            // top (which is the screen top once expanded), 0dp corner radius.
            // Size, position and corner radius all lerp together, so the
            // thumbnail literally grows into the hero cover — one element.
            val miniArtSize = 40.dp
            val miniArtInset = 12.dp
            val miniArtCy = MiniHeight / 2f
            val miniArtRadius = 8.dp

            val heroArtSize = maxW
            val heroArtCy = maxW / 2f
            val heroArtRadius = 0.dp

            val artSize = lerp(miniArtSize, heroArtSize, eased)
            val artCx = lerp(miniArtInset + miniArtSize / 2f, maxW / 2f, eased)
            val artCy = lerp(miniArtCy, heroArtCy, eased)
            val artRadius = lerp(miniArtRadius, heroArtRadius, eased)

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
                        tween(durationMillis = 380, easing = FastOutSlowInEasing),
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
                // ── The single shared cover art ──
                // One AsyncImage. It is the mini thumbnail (40dp, 8dp radius,
                // capsule-left) and the full hero cover (full-width square,
                // flush to the sheet's top, square corners) — size, position
                // and corner radius lerp together so it grows, not crossfades.
                SharedCoverArt(
                    artUri = currentSong?.albumArtUri,
                    artSize = artSize,
                    artCx = artCx,
                    artCy = artCy,
                    artRadius = artRadius,
                    density = density,
                    context = context,
                    eased = eased,
                )                // ── Mini row: title/artist + play/pause + next ──
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
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
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

/**
 * The single shared cover. One [AsyncImage], fixed 1080 decode size (so the
 * growing drag never triggers a re-decode per frame), positioned by absolute
 * offset within the sheet and clipped to a lerped corner radius. The border
 * tints from the mini ink to white as it grows.
 */
@Composable
private fun SharedCoverArt(
    artUri: Uri?,
    artSize: Dp,
    artCx: Dp,
    artCy: Dp,
    artRadius: Dp,
    density: Density,
    context: Context,
    eased: Float,
) {
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
            .clip(RoundedCornerShape(artRadius))
            .background(Color(0xFF2A2A2A))
            .border(
                width = 0.8.dp,
                color = lerpColor(
                    MiniInk.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.10f),
                    eased,
                ),
                shape = RoundedCornerShape(artRadius),
            ),
        contentAlignment = Alignment.Center,
    ) {
        artUri?.let { uri ->
            // Fixed decode size: the art grows during the expand drag, so a
            // layout-sized request would restart on every frame and settle on
            // a small decode. 1080 keeps the real metadata art crisp without
            // a huge bitmap.
            val request = remember(uri) {
                ImageRequest.Builder(context)
                    .data(uri)
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
    // The expanded player is now a LIGHTWEIGHT surface: the dark PlayerBg
    // (already painted by the morphing sheet's background), the title/artist
    // + control cluster are drawn by the host's persistent [PlayerControlBar]
    // overlay (so they stay on the lyrics/queue pages too). Here we only need
    // the top bar (lyrics + close). No second PlayerCoverBlend — the cover is
    // the single shared element above.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
    ) {
        // A subtle scrim behind the top bar so the round buttons stay legible
        // over any cover.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Transparent,
                    )
                )
        )

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
