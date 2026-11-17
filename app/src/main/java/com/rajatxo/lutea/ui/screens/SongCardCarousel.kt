package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.rajatxo.lutea.domain.model.Song
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

// ═══════════════════════════════════════════════════════════════════════════
//  SONG CARD CAROUSEL
//
//  Search results as a swipeable stack of cards:
//    • the centred card is big and bright, with title, artist and a Play pill
//    • neighbours shrink, tuck BEHIND it, go dark and lose their text
//    • everything is driven continuously by the finger (no steps), then snaps
//    • a row of dots underneath, the active one stretches into a pill
// ═══════════════════════════════════════════════════════════════════════════

private const val CardAspect = 1.08f        // height / width of the centre card
private const val NeighbourScale = 0.78f    // how small the side cards get
private const val Tuck = 0.30f              // how far (of a card width) they slide under the centre card

/** 0 = this page is centred, ±1 = one card to the right / left. Live while swiping. */
private fun PagerState.offsetOf(page: Int): Float =
    (page - currentPage) - currentPageOffsetFraction

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SongCardCarousel(
    songs: List<Song>,
    nowPlayingId: Long?,
    onPlay: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { songs.size })
    val scope = rememberCoroutineScope()

    // new results → back to the first card. The page count can briefly be
    // zero while a list swap is settling, so never scroll then.
    LaunchedEffect(songs) {
        if (songs.isNotEmpty() && pagerState.pageCount > 0) {
            pagerState.scrollToPage(0)
        }
    }

    // the whole stack rises in when the results appear
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier.statusBarsPadding()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val cardW = min(maxWidth.value * 0.58f, (maxHeight.value - 6f) / CardAspect).dp
            val cardH = (cardW.value * CardAspect).dp

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = enter.value
                        val s = 0.90f + 0.10f * enter.value
                        scaleX = s
                        scaleY = s
                        translationY = (1f - enter.value) * 28.dp.toPx()
                    },
                contentPadding = PaddingValues(horizontal = (maxWidth - cardW) / 2),
                pageSize = PageSize.Fixed(cardW),
                pageSpacing = 0.dp,
                key = { index -> songs.getOrNull(index)?.id ?: "page-$index" },
            ) { page ->
                val song = songs.getOrNull(page)
                if (song != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // the centred card is always on top of its neighbours
                            .zIndex(-abs(page - pagerState.currentPage).toFloat()),
                        contentAlignment = Alignment.Center,
                    ) {
                        SongCard(
                            song = song,
                            playing = song.id == nowPlayingId,
                            page = page,
                            pager = pagerState,
                            modifier = Modifier
                                .width(cardW)
                                .height(cardH),
                            onClick = {
                                if (page == pagerState.currentPage) {
                                    onPlay(song)
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            },
                        )
                    }
                }
            }
        }

        if (songs.size > 1) {
            CarouselDots(
                count = songs.size,
                current = pagerState.currentPage.coerceIn(0, songs.lastIndex),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp, bottom = 8.dp)
                    .graphicsLayer { alpha = enter.value },
            )
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    playing: Boolean,
    page: Int,
    pager: PagerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = RoundedCornerShape(18.dp)
    val inner = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            // every value below is read inside the layer, so swiping never recomposes the card
            .graphicsLayer {
                val d = pager.offsetOf(page)
                val near = abs(d).coerceAtMost(1f)
                val s = 1f - (1f - NeighbourScale) * near
                scaleX = s
                scaleY = s
                translationX = -d.coerceIn(-2f, 2f) * size.width * Tuck
            }
            .clip(outer)
            .background(Color(0xFF14151A))
            .border(1.dp, Color.White.copy(alpha = 0.09f), outer)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(inner)
                .background(Color(0xFF2A2622)),
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // readable text over any artwork
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.30f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.84f),
                        ),
                    ),
            )

            // title · artist · Play pill — fades out on the side cards
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)
                    .graphicsLayer {
                        alpha = (1f - abs(pager.offsetOf(page)) * 1.8f).coerceIn(0f, 1f)
                    },
            ) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFF2F2F2))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = if (playing) "Playing" else "Play",
                        color = Color(0xFF111111),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // side cards go dark (the centre card is fully lit)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = abs(pager.offsetOf(page)).coerceAtMost(1f) * 0.62f
                    }
                    .background(Color.Black),
            )
        }
    }
}

/** Little dots under the cards; the active one stretches into a pill. */
@Composable
private fun CarouselDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier,
) {
    val shown = min(count, 7)
    val start = if (count <= 7) 0 else (current - 3).coerceIn(0, count - 7)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in start until start + shown) {
            key(i) {
                val active = i == current
                val w by animateDpAsState(
                    targetValue = if (active) 14.dp else 4.dp,
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    label = "dotWidth",
                )
                val a by animateFloatAsState(
                    targetValue = if (active) 0.95f else 0.28f,
                    animationSpec = tween(260),
                    label = "dotAlpha",
                )
                Box(
                    modifier = Modifier
                        .width(w)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = a)),
                )
            }
        }
    }
}
