package com.rajatxo.lutea.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.lutea.R
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.CalSansFamily

/**
 * MiniPlayer — a compact glass capsule above the nav bar showing the current song.
 *
 * - Shows album art thumbnail (or placeholder) + title + artist
 * - Play/pause + next buttons on the right
 * - Tapping the capsule expands to the full player (onExpand callback)
 * - Only visible when a song is loaded (currentSong != null)
 * - Slide-up + fade animation when a song starts playing
 */
@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    hidden: Boolean = false,
    backdrop: LayerBackdrop? = null,
    // Total width to match the nav bar (4 tabs + gaps). When null, falls back
    // to fillMaxWidth with 16dp side padding (the old behaviour).
    navMatchedWidth: Dp? = null,
) {
    val currentSong by PlaybackConnection.currentSong.collectAsState()
    val isPlaying by PlaybackConnection.isPlaying.collectAsState()

    val capsuleShape = RoundedCornerShape(percent = 50)
    // Glass tuned to match the nav bar's frosted look but with a bit more
    // white so the blur reads (the nav bar is barely visible because it has
    // too much white; here we add a measured amount — a 0.16 white tint +
    // a soft top sheen — so the blur is still legible without going chalky).
    val glassSurface = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { capsuleShape },
            effects = {
                vibrancy()
                colorControls(
                    brightness = 0.05f,
                    contrast = 1f,
                    saturation = 1.35f,
                )
                blur(18.dp.toPx())
            },
            onDrawSurface = {
                // Slightly darker base than the nav so the album art pops,
                // plus a touch more white so the glass is visible.
                drawRect(Color.Black.copy(alpha = 0.36f))
                drawRect(Color.White.copy(alpha = 0.16f))
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.45f to Color.White.copy(alpha = 0.06f),
                        1f to Color.Transparent,
                    )
                )
            },
        )
    } else {
        Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color(0xFF303030),
                1f to Color(0xFF121212),
            ),
            shape = capsuleShape,
        )
    }

    AnimatedVisibility(
        visible = currentSong != null && !hidden,
        enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(),
        exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                // Match the nav bar's full outer width (4 tabs + gaps) when
                // provided; otherwise fall back to fillMaxWidth + 16dp pad.
                .then(
                    if (navMatchedWidth != null) {
                        Modifier
                            .width(navMatchedWidth)
                            .height(56.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp)
                    }
                )
                .shadow(elevation = 8.dp, shape = capsuleShape, clip = true)
                .clip(capsuleShape)
                .then(glassSurface)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.55f),
                        0.50f to Color.White.copy(alpha = 0.14f),
                        1f to Color.White.copy(alpha = 0.08f),
                    ),
                    shape = capsuleShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpand() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art thumbnail (or placeholder)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center,
            ) {
                currentSong?.albumArtUri?.let { artUri ->
                    AsyncImage(
                        model = artUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = CalSansFamily,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentSong?.artist ?: "",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontFamily = CalSansFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play/pause button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { PlaybackConnection.playPause() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Next button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(0.6.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { PlaybackConnection.next() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_next),
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
