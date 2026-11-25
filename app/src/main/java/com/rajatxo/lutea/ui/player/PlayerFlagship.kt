package com.rajatxo.lutea.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rajatxo.lutea.R
import com.rajatxo.lutea.data.store.FavouriteStore
import com.rajatxo.lutea.playback.LoopMode
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.BillieEilishFamily
import com.rajatxo.lutea.ui.components.CalSansFamily

import kotlin.math.roundToLong

// ─────────────────────────────────────────────────────────────────────────────
// PlayerCoverBlend — Coral Spiral3 cover block, ported faithfully, with the
// edits the user asked for (corrected after seeing the screenshots).
//
// Spiral3 layout (Spiral3Player.kt lines 722–791):
//   (1) 96dp BLURRED full-screen album art = the immersive background canvas.
//   (2) Sharp SQUARE cover (TopCenter) with a bottom DstIn fade so the sharp
//       cover melts into the blur at its lower edge.
//
// The edits:
//   EDIT 1 — "remove the blur part from the top": the TOP FADE that hazed the
//     cover's top edge is REMOVED (was 120dp Transparent→Black DstIn). The
//     96dp blurred bg + the bottom 140dp fade stay. The cover's top is crisp.
//   EDIT 2 — universal fitting rule: the WHOLE cover is visible (square box,
//     aspectRatio 1f, ContentScale.Fit — nothing cropped, no logo clip).
//   EDIT 3 — "zoom a little + move the cover up + blend follows": the square
//     cover at fillMaxWidth only fills ~45% of the screen height, leaving dead
//     space below before the title. So:
//       • offset 80dp -> 24dp (cover moves up, just clearing the status bar;
//         the gap above the cover shrinks).
//       • graphicsLayer scaleX/Y 1.06f with TOP-CENTER pivot — gentle ~6% zoom
//         that grows the box DOWNWARD (no top crop) with only ~3% side bleed
//         (vs the 1.15f that clipped the right-edge logo). Full image still
//         visible, just bigger.
//       • The bottom 140dp fade is drawn INSIDE the box, so it moves up + grows
//         with the cover — the blend point and down-blur follow automatically.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerCoverBlend(
    modifier: Modifier = Modifier,
) {
    val currentSong by PlaybackConnection.currentSong.collectAsState()
    val context = LocalContext.current
    val coverUriStr = currentSong?.albumArtUri?.toString()

    val request = remember(coverUriStr) {
        if (coverUriStr.isNullOrBlank()) null
        else ImageRequest.Builder(context).data(coverUriStr).crossfade(true).build()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // (1) 96dp BLURRED full-screen album art — the immersive background
        //     canvas. Verbatim Spiral3 (lines 722–733). This is what makes the
        //     cover "melt" into the screen instead of sitting on a flat colour.
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(96.dp),
            )
        }

        // (2) Sharp SQUARE cover (Spiral3 block, lines 735–791) — SQUARE box
        //     (aspectRatio 1f), TopCenter. EDIT 3: offset 24dp (moved up from
        //     80dp) + gentle 1.06 zoom with top-center pivot (grows downward,
        //     no top crop, ~3% side bleed). Universal fitting: full image
        //     visible. The bottom 140dp fade (EDIT 1: top fade removed) is
        //     drawn inside the box, so it moves up + grows with the cover.
        if (request != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .align(Alignment.TopCenter)
                    .offset(y = 24.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        // EDIT 3: gentle ~6% zoom, top-center pivot so the box
                        // grows DOWNWARD (top stays put, no top crop). ~3% side
                        // bleed — full image still visible (1.15 clipped logo).
                        scaleX = 1.06f
                        scaleY = 1.06f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .drawWithContent {
                        drawContent()
                        // EDIT 1: top fade REMOVED (was 120dp Transparent→Black).
                        // Bottom fade kept (140dp Black→Transparent) — verbatim
                        // Spiral3, so the cover still melts into the blur below.
                        val bottomFadeHeightPx = 140.dp.toPx()
                        val imageHeight = size.height
                        val bottomFadeStartY =
                            (imageHeight - bottomFadeHeightPx).coerceAtLeast(0f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startY = bottomFadeStartY,
                                endY = imageHeight,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            ) {
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    // Universal fitting rule — the WHOLE cover shows, nothing
                    // cut off. Fit (not Crop). The box zoom (1.06, above) makes
                    // it bigger; the image scales with the box.
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PlayerControlBar — the persistent bottom control cluster. Drawn ONCE at the
// scaffold level so it sits over the player AND the lyrics page AND the queue
// page — Mochy keeps the seek bar + transport visible on all three.
//
// Layout (matches Mochy Flagship):
//   title/artist .............. heart + up-chevron(-> queue/lyrics)
//   ─────── seek bar ───────  0:06                    -3:06
//   -10s    prev    [play]    next    +10s      (loop)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlayerControlBar(
    onClose: () -> Unit,
    onQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentSong by PlaybackConnection.currentSong.collectAsState()
    val isPlaying by PlaybackConnection.isPlaying.collectAsState()
    val positionMs by PlaybackConnection.positionMs.collectAsState()
    val durationMs by PlaybackConnection.durationMs.collectAsState()
    val loopMode by PlaybackConnection.loopMode.collectAsState()
    val favourites by FavouriteStore.favourites.collectAsState()
    val nav = LocalPlayerNavigation.current

    // Poll position while the control bar exists (it persists across pages).
    LaunchedEffect(Unit) {
        while (true) {
            PlaybackConnection.pollPosition()
            kotlinx.coroutines.delay(500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        // ── Title row: title/artist left, heart + up-chevron right ──
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
            FlagshipIconButton(
                iconRes = if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline,
                contentDescription = if (isFav) "Unfavourite" else "Favourite",
                size = 22.dp,
                tint = if (isFav) Color(0xFFFF6B6B) else Color.White,
                onClick = { currentSong?.let { FavouriteStore.toggle(it.id) } },
            )
            Spacer(Modifier.width(6.dp))
            // Up-chevron opens the queue (scroll-up gesture equivalent).
            FlagshipIconButton(
                iconRes = R.drawable.ic_chevron_up,
                contentDescription = "Queue",
                size = 22.dp,
                onClick = onQueueClick,
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Seek bar — thin at rest, swells under the finger ──
        SeekBarRow(
            positionMs = positionMs,
            durationMs = durationMs,
            onSeek = { PlaybackConnection.seekTo(it) },
        )

        Spacer(Modifier.height(20.dp))

        // ── Transport: -10s / prev / play / next / +10s, loop at the edge ──
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlagshipIconButton(
                    iconRes = R.drawable.ic_skip_back_10,
                    contentDescription = "Back 10 seconds",
                    size = 26.dp,
                    onClick = {
                        val target = (positionMs - 10_000L).coerceAtLeast(0L)
                        PlaybackConnection.seekTo(target)
                    },
                )
                Spacer(Modifier.width(20.dp))
                FlagshipIconButton(
                    iconRes = R.drawable.ic_prev,
                    contentDescription = "Previous",
                    size = 30.dp,
                    onClick = { PlaybackConnection.prev() },
                )
                Spacer(Modifier.width(24.dp))
                // Play/pause — the large filled circle, dead-centre.
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { PlaybackConnection.playPause() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(Modifier.width(24.dp))
                FlagshipIconButton(
                    iconRes = R.drawable.ic_next,
                    contentDescription = "Next",
                    size = 30.dp,
                    onClick = { PlaybackConnection.next() },
                )
                Spacer(Modifier.width(20.dp))
                FlagshipIconButton(
                    iconRes = R.drawable.ic_skip_forward_10,
                    contentDescription = "Forward 10 seconds",
                    size = 26.dp,
                    onClick = {
                        val target = (positionMs + 10_000L)
                            .coerceAtMost(durationMs.coerceAtLeast(0L))
                        PlaybackConnection.seekTo(target)
                    },
                )
            }
            // Loop tucked at the right edge so the play button stays centred.
            val loopActive = loopMode != LoopMode.OFF
            FlagshipIconButton(
                iconRes = if (loopMode == LoopMode.ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                contentDescription = when (loopMode) {
                    LoopMode.OFF -> "Loop off"
                    LoopMode.ALL -> "Loop queue"
                    LoopMode.ONE -> "Loop this song"
                },
                size = 22.dp,
                tint = Color.White.copy(alpha = if (loopActive) 1f else 0.45f),
                bgAlpha = if (loopActive) 0.16f else 0f,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { PlaybackConnection.cycleLoopMode() },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Seek bar — thin at rest (3dp), swells under the finger (6dp), no thumb at
// rest, small thumb while dragging. Mochy Flagship seek bar.
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
    val trackHeight by animateDpAsState(
        targetValue = if (seeking) 6.dp else 3.dp,
        animationSpec = tween(180),
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
                    detectHorizontalDragGestures(
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

/** A small round icon button used in the title row and transport. */
@Composable
private fun FlagshipIconButton(
    iconRes: Int,
    contentDescription: String,
    size: Dp,
    tint: Color = Color.White,
    bgAlpha: Float = 0.08f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size + 16.dp)
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
            modifier = Modifier.size(size),
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = (totalSec % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
