package com.rajatxo.lutea.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.lutea.data.model.Playlist
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.playback.PlaybackConnection
import com.rajatxo.lutea.ui.components.CalSansFamily
import com.rajatxo.lutea.ui.foundation.LuteaTabCapsule
import com.rajatxo.lutea.ui.foundation.fluidNavMetrics
import com.rajatxo.lutea.ui.player.FullPlayer
import com.rajatxo.lutea.ui.player.MiniPlayer
import com.rajatxo.lutea.ui.util.LuteaTab
import com.rajatxo.lutea.ui.util.rememberCoverThemeColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/*
 * ===================================================================================
 *  WIRING (adjust to the real PlaylistStore / PlaybackConnection API)
 * ===================================================================================
 *  This screen is pure UI: it takes the playlist, its songs and callbacks. Put the store
 *  lookup and playback calls in a thin route function, for example:
 *
 *  @Composable
 *  fun PlaylistDetailRoute(playlistId: String, onBack: () -> Unit) {
 *      val playlists by PlaylistStore.playlists.collectAsState()
 *      val playlist = playlists.firstOrNull { it.id == playlistId } ?: return
 *      val songs = remember(playlist) { PlaylistStore.getSongs(playlistId) }
 *
 *      PlaylistDetailScreen(
 *          playlist = playlist,
 *          songs = songs,
 *          onBack = onBack,
 *          onPlayAll = { songs.firstOrNull()?.let { PlaybackConnection.playSong(it, songs) } },
 *          onShuffle = { songs.shuffled().let { s -> s.firstOrNull()?.let { PlaybackConnection.playSong(it, s) } } },
 *          onSongClick = { _, song -> PlaybackConnection.playSong(song, songs) },
 *          songArt = { it.albumArtUri },            // <- whatever the Song model calls its art
 *          insets = LocalLuteaWindowInsets.current, // Law 1
 *          // onRename / onChangeCover / onDelete -> PlaylistStore calls
 *      )
 *  }
 * ===================================================================================
 */

// ------------------------------------------------------------------ tunables

private const val COVER_HEIGHT_FRACTION = 0.60f   // square cover side, capped at 60% of height
private const val TITLE_OVERLAP_FRACTION = 0.14f  // big title starts this much inside the cover's fade zone
private val TopBarContentHeight = 56.dp
private val ActionButtonSize = 44.dp
private val PlaylistStatsHeight = 56.dp
private val CustomOrderHeight = 36.dp
private val PlayCapsulePadding = 44.dp

/**
 * Playlist detail: cover art that rides up, zooms, dims, dissolves and blurs as the list
 * scrolls over it (Mochy's ScrollAwayCover, exactly), dissolving into a dark hue-tinted
 * background colour taken from the cover, with a song list riding over it. Once the header
 * is mostly gone, a progressive backdrop blur fades in below the status bar and eases back
 * to zero 96dp down — Mochy's top-bar glass, without a hard edge.
 *
 * Law 7: everything screen-relative is a fraction of BoxWithConstraints.maxWidth/maxHeight.
 * Law 1: pass LocalLuteaWindowInsets.current as [insets].
 * Law 9: forces light status/navigation bar icons while this screen is shown (the background
 *        is always dark), and restores the previous appearance on exit.
 *
 * @param onSongClick (index in [songs], song)
 * @param songArt     model for the row thumbnail (Uri / String / null). Null shows a note glyph.
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    songs: List<Song>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (index: Int, song: Song) -> Unit,
    modifier: Modifier = Modifier,
    onRename: () -> Unit = {},
    onChangeCover: () -> Unit = {},
    onAddSongs: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCustomOrder: () -> Unit = {},
    songArt: (Song) -> Any? = { null },
    insets: WindowInsets = WindowInsets.systemBars,
    activeTab: LuteaTab = LuteaTab.Library,
    onTabSelected: (LuteaTab) -> Unit = {},
) {
    DetailLightSystemBarIcons()
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var playerVisible by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    // Back closes the topmost layer first: lyrics, player, search, then the page.
    BackHandler(enabled = !playerVisible && !showLyrics && !searchOpen) { onBack() }
    BackHandler(enabled = searchOpen) { searchOpen = false }
    BackHandler(enabled = showLyrics) { showLyrics = false }
    BackHandler(enabled = playerVisible && !showLyrics) { playerVisible = false }
    LaunchedEffect(searchOpen) {
        if (!searchOpen) searchQuery = ""
    }

    val isPlaying by PlaybackConnection.isPlaying.collectAsState()
    val onPlayPause = {
        if (PlaybackConnection.currentSong.value == null) onPlayAll()
        else PlaybackConnection.playPause()
    }

    val themeState = rememberCoverThemeColor(playlist.coverUri, fallbackSeed = playlist.id.toString())
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        // Playlist covers are always shown 1:1. Use the full width on a phone,
        // but never taller than 60% of the screen (landscape / large displays).
        val coverHeight = minOf(screenW, screenH * COVER_HEIGHT_FRACTION)
        val coverHeightPx = with(density) { coverHeight.toPx() }.coerceAtLeast(1f)
        val titleOverlap = screenH * TITLE_OVERLAP_FRACTION
        val titleOverlapPx = with(density) { titleOverlap.toPx() }
        // contentPadding.top in pixels = coverHeight - titleOverlap (the space above the header).
        val contentTopPx = coverHeightPx - titleOverlapPx
        val sidePad = screenW * 0.06f
        val thumbSize = screenW * 0.13f
        val topInset = with(density) { insets.getTop(this).toDp() }
        val topInsetPx = with(density) { topInset.toPx() }
        val bottomInset = with(density) { insets.getBottom(this).toDp() }

        // 0f..1f as the FIRST SONG ROW reaches the top of the screen.
        //
        // We read the header item's actual offset from layoutInfo and derive progress from THAT.
        // LazyListItemInfo.offset is 0 at rest (the header at the content-area top) and goes
        // negative as the header scrolls up. So scrolledFromRest = -header.offset — NOT
        // contentTopPx - header.offset (that assumed viewport coords and gave progress ≈ 0.74 at
        // rest, which made the cover's alpha ~0.07 and the cover was invisible at rest).
        //
        // The denominator is contentTopPx + headerHeight — the total scroll needed to bring the
        // first song row to the top of the viewport. This makes the cover's full parallax unfold
        // over exactly the scroll that brings the songs up, so cover and list move together with
        // no gap. When the header leaves visibleItemsInfo, we return 1f if it scrolled off, or 0f
        // if it just hasn't been laid out yet (first frame) — so the cover is visible from frame
        // one instead of flashing invisible.
        val progress = remember(listState, contentTopPx) {
            derivedStateOf {
                val header = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == "header" }
                if (header == null) {
                    // header not in visibleItemsInfo: either not laid out yet (index 0, at rest)
                    // or scrolled fully off (index > 0). Assume rest if index 0, done if index > 0.
                    if (listState.firstVisibleItemIndex == 0) 0f else 1f
                } else {
                    val headerH = header.size.toFloat()
                    val scrolledFromRest = -header.offset.toFloat()
                    val denom = (contentTopPx + headerH).coerceAtLeast(1f)
                    (scrolledFromRest / denom).coerceIn(0f, 1f)
                }
            }
        }

        // Title and shuffle stay in the header and scroll with it. Only search detaches
        // and animates into the top bar, so no title docking/scale/fade state is needed.
        val topBarContentPx = with(density) { TopBarContentHeight.toPx() }
        val defaultTitleHeightPx = with(density) { 40.dp.toPx() }
        val titleToStatsGapPx = with(density) { 2.dp.toPx() }
        val statsHeightPx = with(density) { PlaylistStatsHeight.toPx() }
        val actionsGapPx = with(density) { 10.dp.toPx() }

        var titleSize by remember { mutableStateOf(IntSize.Zero) }
        val titleHeightPx = if (titleSize.height > 0) {
            titleSize.height.toFloat()
        } else {
            defaultTitleHeightPx
        }

        val screenHPx = with(density) { screenH.toPx() }
        val rawTitleTop = remember(listState, contentTopPx, screenHPx) {
            derivedStateOf {
                val header = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == "header" }
                // Once the header leaves the viewport, keep the overlay fully off-screen.
                // Using -contentTopPx pinned the title at y=0 under the top bar.
                val headerOffset = header?.offset?.toFloat()
                    ?: if (listState.firstVisibleItemIndex == 0) 0f else -screenHPx
                contentTopPx + headerOffset
            }
        }
        val headerTop: () -> Float = { rawTitleTop.value }
        val statsTop: () -> Float = { headerTop() + titleHeightPx + titleToStatsGapPx }
        val actionsTop: () -> Float = {
            statsTop() + statsHeightPx + actionsGapPx
        }

        // Where the search capsule lands: between the 40dp back disc and the menu.
        val searchDockX = with(density) { (sidePad + 48.dp).toPx() }
        val searchDockY = topInsetPx +
            (topBarContentPx - with(density) { ActionButtonSize.toPx() }) / 2f
        val searchCapsuleWidth =
            (screenW - sidePad * 2f - 104.dp).coerceAtLeast(120.dp)

        val totalMs = remember(songs) { songs.sumOf { detailDurationMs(it.duration) } }
        val statsSongCount = songs.size.toString()
        val statsDuration = remember(totalMs) { detailStatsDuration(totalMs) }
        val statsCreated = remember(playlist.createdAtMs) { detailCreatedDate(playlist.createdAtMs) }
        val visibleSongs = remember(songs, searchOpen, searchQuery) {
            val q = searchQuery.trim()
            if (searchOpen && q.isNotEmpty()) {
                songs.withIndex().filter { (_, song) ->
                    song.title.orEmpty().contains(q, ignoreCase = true) ||
                        song.artist.orEmpty().contains(q, ignoreCase = true)
                }
            } else {
                songs.withIndex().toList()
            }
        }

        // The cover + song list are captured together so the top glass and the
        // progressive blur sample what is actually scrolling underneath, not
        // just the cover art.
        val coverScroll = remember(progress) { { progress.value } }
        val coverColor = remember(themeState) { { themeState.value } }
        val pageGraphicsLayer = rememberGraphicsLayer()
        val pageBackdrop = rememberLayerBackdrop(graphicsLayer = pageGraphicsLayer) {
            drawContent()
        }

        // Mochy's playlist bar frosts once the header is more than halfway gone.
        // Read progress through a derived boolean so scrolling only recomposes
        // at the threshold, not on every frame.
        val blurVisible by remember(progress) {
            derivedStateOf { progress.value > 0.55f }
        }
        val blurMorph = animateFloatAsState(
            targetValue = if (blurVisible) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "playlistTopBlur",
        )
        // Only compose the blur while it is actually visible; the value itself is
        // read in the draw phase, so the scroll animation never recomposes the list.
        val blurActive by remember {
            derivedStateOf { blurMorph.value > 0.01f }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Capture layer: the real page behind the top bar. It stays outside
            // the LazyColumn, per Kyant's scrollable rule.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(pageBackdrop),
            ) {
                PlaylistPageBackground(
                    coverUri = playlist.coverUri,
                    backgroundColor = coverColor,
                    coverHeight = coverHeight,
                    scrollProgress = coverScroll,
                    placeholderSeed = playlist.id.toString(),
                    enableBlur = true,
                )

                // Top padding puts the header at the bottom of the cover's fade zone,
                // so the list "rides over" the blended background.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = coverHeight - titleOverlap,
                        bottom = bottomInset + screenH * 0.04f,
                    ),
                ) {
                    item(key = "header") {
                        PlaylistHeaderSpacer(
                            name = playlist.name,
                            sidePad = sidePad,
                        )
                    }
                    if (visibleSongs.isEmpty()) {
                        item(key = "empty") {
                            DetailEmptyHint(
                                sidePad = sidePad,
                                searching = searchOpen && searchQuery.isNotBlank(),
                            )
                        }
                    } else {
                        itemsIndexed(visibleSongs) { _, indexed ->
                            val song = indexed.value
                            PlaylistSongRow(
                                song = song,
                                art = songArt(song),
                                thumbSize = thumbSize,
                                sidePad = sidePad,
                                onClick = { onSongClick(indexed.index, song) },
                            )
                        }
                    }
                }

                // Title and stats are part of the captured page, so they soften
                // under the glass just like the rows do.
                PlaylistTitleOverlay(
                    name = playlist.name,
                    sidePad = sidePad,
                    headerTop = headerTop,
                    onSize = { titleSize = it },
                )
                PlaylistStatsOverlay(
                    songCount = statsSongCount,
                    duration = statsDuration,
                    dateDay = statsCreated.first,
                    dateMonth = statsCreated.second,
                    sidePad = sidePad,
                    statsTop = statsTop,
                )
            }

            // Real backdrop blur, full at the status bar and eased to zero
            // 96dp below it. Drawn above the captured page, below the controls.
            if (blurActive) {
                PlaylistTopBlur(
                    backdrop = pageBackdrop,
                    morph = { blurMorph.value },
                    height = topInset + 96.dp,
                    tint = coverColor,
                )
            }

            // Controls stay sharp above the blur. Title/stats/shuffle scroll
            // with the header; only the search control detaches into the top bar.
            PlaylistActionsOverlay(
                actionsTop = actionsTop,
                searchOpen = searchOpen,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onOpenSearch = { searchOpen = true },
                searchDockX = searchDockX,
                searchDockY = searchDockY,
                searchCapsuleWidth = searchCapsuleWidth,
                isPlaying = isPlaying,
                onPlayPause = onPlayPause,
                onShuffle = onShuffle,
                onCustomOrder = onCustomOrder,
                backdrop = pageBackdrop,
            )

            // Back + menu. Transparent bar, so songs pass behind the buttons.
            PlaylistTopBar(
                searchOpen = searchOpen,
                onCloseSearch = { searchOpen = false },
                topInset = topInset,
                sidePad = sidePad,
                onBack = onBack,
                onRename = onRename,
                onChangeCover = onChangeCover,
                onAddSongs = onAddSongs,
                onDelete = onDelete,
                backdrop = pageBackdrop,
                modifier = Modifier.align(Alignment.TopStart),
            )

            // Bottom chrome, same as the main app: nav capsule + mini player.
            // The caller adds matching bottom insets so songs can scroll clear.
            val navBottom = maxOf(bottomInset, 48.dp)
            val navMetrics = fluidNavMetrics(
                maxWidth = screenW,
                maxHeight = screenH,
                tabCount = LuteaTab.entries.size,
                fontScale = density.fontScale,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(navBottom + 112.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black,
                        )
                    ),
            )
            MiniPlayer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom + 56.dp),
                onExpand = { playerVisible = true },
                backdrop = pageBackdrop,
                navMatchedWidth = navMetrics.navBarWidthFor(LuteaTab.entries.size),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LuteaTabCapsule(
                    tabs = LuteaTab.entries.toList(),
                    activeTab = activeTab,
                    onTabSelected = onTabSelected,
                    backdrop = pageBackdrop,
                )
            }
            FullPlayer(
                visible = playerVisible,
                onClose = { playerVisible = false; showLyrics = false },
                onLyricsClick = { showLyrics = true },
                modifier = Modifier.fillMaxSize(),
            )
            if (showLyrics) {
                LyricsScreen(onClose = { showLyrics = false })
            }
        }
    }
}

@Composable
private fun PlaylistPageBackground(
    coverUri: String?,
    backgroundColor: () -> Color,
    coverHeight: Dp,
    scrollProgress: () -> Float,
    placeholderSeed: String,
    enableBlur: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind { drawRect(backgroundColor()) },
    ) {
        PlaylistCoverBackground(
            coverUri = coverUri,
            backgroundColor = backgroundColor,
            coverHeight = coverHeight,
            scrollProgress = scrollProgress,
            modifier = Modifier.align(Alignment.TopCenter),
            placeholderSeed = placeholderSeed,
            enableBlur = enableBlur,
        )
    }
}

/**
 * Mochy's top-bar blur, reduced to the one thing the playlist needs: a real
 * backdrop blur of the page, strongest at the status bar and faded to zero by
 * the bottom of the band. Three overlapping radii approximate the variable
 * radius ramp; the DstIn gradient removes the hard edge.
 *
 * shortcut: stacked bands instead of Mochy's single-pass variable-radius
 * shader. Upgrade to an AGSL effect only if band seams ever become visible.
 */
@Composable
private fun PlaylistTopBlur(
    backdrop: LayerBackdrop,
    morph: () -> Float,
    height: Dp,
    tint: () -> Color,
) {
    val density = LocalDensity.current
    val maxBlurPx = with(density) { 14.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                alpha = morph()
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                // Tint the glass once, then fade blur + tint together.
                drawRect(tint().copy(alpha = 0.20f))
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        0.64f to Color.Black,
                        0.94f to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        // Three overlapping blur radii: 14dp at the status bar, 9dp lower,
        // 4dp near the fade. The later bands cross-fade over the earlier ones,
        // so the composite stays opaque and never ghosts against sharp rows.
        PlaylistBlurBand(
            backdrop = backdrop,
            blurPx = maxBlurPx,
            morph = morph,
            modifier = Modifier.fillMaxSize(),
        )
        PlaylistBlurBand(
            backdrop = backdrop,
            blurPx = maxBlurPx * 0.65f,
            morph = morph,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .offset(y = height * 0.25f),
            fadeIn = 0.35f,
        )
        PlaylistBlurBand(
            backdrop = backdrop,
            blurPx = maxBlurPx * 0.28f,
            morph = morph,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .offset(y = height * 0.55f),
            fadeIn = 0.35f,
        )
    }
}

@Composable
private fun PlaylistBlurBand(
    backdrop: LayerBackdrop,
    blurPx: Float,
    morph: () -> Float,
    modifier: Modifier = Modifier,
    fadeIn: Float = 0f,
) {
    Box(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (fadeIn > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            fadeIn to Color.Black,
                            1f to Color.Black,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RectangleShape },
                // A background fade must not inherit the button glass rim/shadow:
                // that is the dark line that used to show at the band's bottom.
                highlight = null,
                shadow = null,
                innerShadow = null,
                effects = {
                    blur(blurPx * morph())
                },
            ),
    )
}

// ------------------------------------------------------------------ header + action buttons

@Composable
private fun PlaylistHeaderSpacer(
    name: String,
    sidePad: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sidePad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DetailText(
            text = name,
            size = 36.sp,
            lineHeight = 40.sp,
            family = CalSansFamily,
            maxLines = 3,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = 0f },
        )
        Spacer(Modifier.height(2.dp))
        Spacer(Modifier.height(PlaylistStatsHeight))
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(ActionButtonSize))
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(CustomOrderHeight))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlaylistTitleOverlay(
    name: String,
    sidePad: Dp,
    headerTop: () -> Float,
    onSize: (IntSize) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, headerTop().roundToInt()) }
            .padding(horizontal = sidePad),
        contentAlignment = Alignment.TopCenter,
    ) {
        DetailText(
            text = name,
            size = 36.sp,
            lineHeight = 40.sp,
            family = CalSansFamily,
            maxLines = 3,
            textAlign = TextAlign.Center,
            modifier = Modifier.onSizeChanged(onSize),
        )
    }
}

@Composable
private fun PlaylistStatsOverlay(
    songCount: String,
    duration: String,
    dateDay: String,
    dateMonth: String,
    sidePad: Dp,
    statsTop: () -> Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, statsTop().roundToInt()) }
            .padding(horizontal = sidePad)
            .height(PlaylistStatsHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailStat(value = songCount, label = "Songs", modifier = Modifier.weight(1f))
        DetailStatDivider()
        DetailStat(value = duration, label = "Duration", modifier = Modifier.weight(1f))
        DetailStatDivider()
        DetailStat(value = dateDay, label = dateMonth, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DetailStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DetailText(text = value, size = 22.sp, color = Color.White)
        Spacer(Modifier.height(4.dp))
        DetailText(
            text = label,
            size = 11.sp,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun DetailStatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(28.dp)
            .background(Color.White.copy(alpha = 0.16f)),
    )
}

@Composable
private fun PlaylistActionsOverlay(
    actionsTop: () -> Float,
    searchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    searchDockX: Float,
    searchDockY: Float,
    searchCapsuleWidth: Dp,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onShuffle: () -> Unit,
    onCustomOrder: () -> Unit,
    backdrop: LayerBackdrop?,
) {
    val searchProgress by animateFloatAsState(
        targetValue = if (searchOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "playlistSearchExpand",
    )
    val density = LocalDensity.current
    val gapPx = with(density) { 12.dp.roundToPx() }
    val customGapPx = with(density) { 8.dp.roundToPx() }
    val restSizePx = with(density) { ActionButtonSize.roundToPx() }

    Layout(
        content = {
            DetailGlassCircleButton(
                desc = "Shuffle playlist",
                onClick = onShuffle,
                white = true,
                backdrop = backdrop,
            ) {
                DetailShuffleGlyph(Color.Black, Modifier.size(18.dp))
            }
            DetailPill(
                label = if (isPlaying) "Pause" else "Play",
                filled = true,
                onClick = onPlayPause,
                height = ActionButtonSize,
                horizontalPadding = PlayCapsulePadding,
            ) {
                if (isPlaying) {
                    DetailPauseGlyph(Color.Black, Modifier.size(18.dp))
                } else {
                    DetailPlayGlyph(Color.Black, Modifier.size(18.dp))
                }
            }
            DetailSearchCapsule(
                expanded = searchOpen,
                progress = searchProgress,
                width = searchCapsuleWidth,
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onOpen = onOpenSearch,
                white = true,
                backdrop = backdrop,
            )
            DetailPill(
                label = "Custom order",
                filled = false,
                onClick = onCustomOrder,
                height = CustomOrderHeight,
                horizontalPadding = 18.dp,
                backdrop = backdrop,
            ) {
                DetailReorderGlyph(Color.White, Modifier.size(16.dp))
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        val shuffle = measurables[0].measure(Constraints())
        val play = measurables[1].measure(Constraints())
        val search = measurables[2].measure(Constraints())
        val customOrder = measurables[3].measure(Constraints())
        val total = shuffle.width + gapPx + play.width + gapPx + restSizePx
        val startX = ((constraints.maxWidth - total) / 2).coerceAtLeast(0)
        val shuffleRestX = startX
        val playRestX = startX + shuffle.width + gapPx
        val searchRestX = playRestX + play.width + gapPx

        layout(constraints.maxWidth, constraints.maxHeight) {
            val p = searchProgress.coerceIn(0f, 1f)
            val restY = actionsTop()
            val shuffleY = restY + (restSizePx - shuffle.height) / 2f
            val playY = restY + (restSizePx - play.height) / 2f
            shuffle.placeWithLayer(shuffleRestX, shuffleY.roundToInt())
            play.placeWithLayer(playRestX, playY.roundToInt())

            val searchX = searchRestX + (searchDockX - searchRestX) * p
            val searchY = restY + (searchDockY - restY) * p
            search.placeWithLayer(searchX.roundToInt(), searchY.roundToInt())

            // Directly under the play capsule, which is already screen-centred.
            customOrder.placeWithLayer(
                x = ((constraints.maxWidth - customOrder.width) / 2f).roundToInt(),
                y = (restY + restSizePx + customGapPx).roundToInt(),
            )
        }
    }
}

@Composable
private fun DetailGlassCircleButton(
    desc: String,
    onClick: () -> Unit,
    size: Dp = ActionButtonSize,
    white: Boolean = false,
    backdrop: LayerBackdrop? = null,
    icon: @Composable () -> Unit,
) {
    val surface = if (white) {
        Modifier.background(Color.White, CircleShape)
    } else {
        Modifier.detailGlass(CircleShape, backdrop = backdrop)
    }
    Box(
        modifier = Modifier
            .size(size)
            .detailPressable(onClick = onClick)
            .then(surface)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun DetailSearchCapsule(
    expanded: Boolean,
    progress: Float,
    width: Dp,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: () -> Unit,
    white: Boolean = false,
    backdrop: LayerBackdrop?,
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val currentWidth = ActionButtonSize + (width - ActionButtonSize) * progress
    // Keep the glyph at the left of the growing capsule instead of letting the
    // expanding centre drag it across the text field.
    val iconShiftDp = ActionButtonSize * 0.5f - currentWidth * 0.5f
    val pressModifier = if (expanded) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = { focusRequester.requestFocus() },
        )
    } else {
        Modifier.detailPressable(onClick = onOpen)
    }
    val ink = if (white) Color.Black else Color.White
    val surface = if (white) {
        Modifier.background(Color.White, CircleShape)
    } else {
        Modifier.detailGlass(CircleShape, backdrop = backdrop)
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            // Wait for the circle-to-capsule motion before the IME can resize anything.
            delay(420)
            runCatching { focusRequester.requestFocus() }
        } else {
            focusManager.clearFocus()
        }
    }

    Box(
        modifier = Modifier
            .width(currentWidth)
            .height(ActionButtonSize)
            .then(pressModifier)
            .then(surface)
            .semantics { contentDescription = "Search playlist" },
        contentAlignment = Alignment.Center,
    ) {
        DetailSearchGlyph(
            color = ink.copy(alpha = 0.9f),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    translationX = with(density) { iconShiftDp.toPx() }
                },
        )
        if (expanded || progress > 0.02f) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = ink,
                    fontSize = 14.sp,
                    fontFamily = CalSansFamily,
                ),
                cursorBrush = SolidColor(ink),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp, end = 16.dp)
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .graphicsLayer {
                        alpha = ((progress - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    },
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            DetailText(
                                text = "Search songs",
                                size = 14.sp,
                                color = ink.copy(alpha = 0.5f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailPill(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = ActionButtonSize,
    horizontalPadding: Dp = 26.dp,
    backdrop: LayerBackdrop? = null,
    icon: @Composable () -> Unit,
) {
    val fg = if (filled) Color.Black else Color.White
    val surface = if (filled) {
        Modifier.background(Color.White)
    } else {
        Modifier.detailGlass(CircleShape, backdrop = backdrop)
    }
    Row(
        modifier = Modifier
            .height(height)
            .detailPressable(onClick = onClick, enabled = enabled)
            .clip(CircleShape)
            .then(surface)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        DetailText(text = label, size = 15.sp, color = fg)
    }
}

@Composable
private fun Modifier.detailGlass(
    shape: Shape,
    backdrop: LayerBackdrop? = null,
    baseAlpha: Float = 0.35f,
): Modifier {
    val surface = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.4f)
                blur(20f.dp.toPx())
            },
            onDrawSurface = {
                drawRect(Color.Black.copy(alpha = baseAlpha))
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.10f),
                        0.45f to Color.White.copy(alpha = 0.02f),
                        1f to Color.Transparent,
                    ),
                )
            },
        )
    } else {
        Modifier
            .background(Color.Black.copy(alpha = baseAlpha), shape)
            .background(
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.14f),
                    0.45f to Color.White.copy(alpha = 0.03f),
                    1f to Color.Transparent,
                ),
                shape = shape,
            )
    }
    return this
        .clip(shape)
        .then(surface)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.52f),
                0.5f to Color.White.copy(alpha = 0.16f),
                1f to Color.White.copy(alpha = 0.08f),
            ),
            shape = shape,
        )
}

@Composable
private fun DetailMoreMenu(
    searchOpen: Boolean,
    onCloseSearch: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onAddSongs: () -> Unit,
    onDelete: () -> Unit,
    backdrop: LayerBackdrop?,
) {
    var open by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val dotsAlpha by animateFloatAsState(
        targetValue = if (searchOpen) 0f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "playlistMenuDots",
    )
    val crossAlpha by animateFloatAsState(
        targetValue = if (searchOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "playlistMenuCross",
    )
    LaunchedEffect(searchOpen) {
        if (searchOpen) open = false
    }

    Box {
        Box(
            modifier = Modifier
                .size(ActionButtonSize)
                .detailPressable {
                    if (searchOpen) onCloseSearch() else open = true
                }
                .detailGlass(CircleShape, backdrop = backdrop)
                .semantics {
                    contentDescription = if (searchOpen) "Close search" else "More"
                },
            contentAlignment = Alignment.Center,
        ) {
            DetailDotsGlyph(
                color = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        alpha = dotsAlpha
                        rotationZ = 90f * (1f - dotsAlpha)
                    },
            )
            DetailCloseGlyph(
                color = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        alpha = crossAlpha
                        rotationZ = -90f * (1f - crossAlpha)
                    },
            )
        }

        if (open && !searchOpen) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, with(density) { 48.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .width(208.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1F1F24))
                        .padding(vertical = 6.dp),
                ) {
                    DetailMenuRow("Rename") { open = false; onRename() }
                    DetailMenuRow("Change cover") { open = false; onChangeCover() }
                    DetailMenuRow("Add songs") { open = false; onAddSongs() }
                    DetailMenuRow("Delete", destructive = true) { open = false; onDelete() }
                }
            }
        }
    }
}

@Composable
private fun DetailMenuRow(
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    DetailText(
        text = label,
        size = 15.sp,
        color = if (destructive) Color(0xFFFF6B6B) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .detailPressable(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
    )
}

// ------------------------------------------------------------------ song rows

@Composable
private fun PlaylistSongRow(
    song: Song,
    art: Any?,
    thumbSize: Dp,
    sidePad: Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .detailPressable(pressedScale = 0.985f, onClick = onClick)
            .padding(horizontal = sidePad, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailSongThumb(art = art, size = thumbSize)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            DetailText(text = song.title.orEmpty(), size = 16.sp)
            Spacer(Modifier.height(2.dp))
            DetailText(
                text = song.artist.orEmpty(),
                size = 13.sp,
                color = Color.White.copy(alpha = 0.58f),
            )
        }
        Spacer(Modifier.width(12.dp))
        DetailText(
            text = detailClock(detailDurationMs(song.duration)),
            size = 12.sp,
            color = Color.White.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun DetailSongThumb(art: Any?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        DetailNoteGlyph(Color.White.copy(alpha = 0.35f), Modifier.size(size * 0.5f))
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun DetailEmptyHint(sidePad: Dp, searching: Boolean) {
    DetailText(
        text = if (searching) {
            "No songs found."
        } else {
            "No songs yet. Add some from your library."
        },
        size = 14.sp,
        color = Color.White.copy(alpha = 0.55f),
        maxLines = 3,
        modifier = Modifier.padding(horizontal = sidePad, vertical = 24.dp),
    )
}

// ------------------------------------------------------------------ top bar row

/**
 * Back + menu only. The bar stays transparent so the title/shuffle scroll behind it.
 * While the search capsule is open the menu disc becomes a cross that closes it.
 */
@Composable
private fun PlaylistTopBar(
    searchOpen: Boolean,
    onCloseSearch: () -> Unit,
    topInset: Dp,
    sidePad: Dp,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onAddSongs: () -> Unit,
    onDelete: () -> Unit,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(topInset + TopBarContentHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(TopBarContentHeight)
                .padding(horizontal = sidePad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailBackButton(onBack = onBack, backdrop = backdrop)
            Spacer(Modifier.weight(1f))
            DetailMoreMenu(
                searchOpen = searchOpen,
                onCloseSearch = onCloseSearch,
                onRename = onRename,
                onChangeCover = onChangeCover,
                onAddSongs = onAddSongs,
                onDelete = onDelete,
                backdrop = backdrop,
            )
        }
    }
}

@Composable
private fun DetailBackButton(
    onBack: () -> Unit,
    backdrop: LayerBackdrop?,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .detailPressable(onClick = onBack)
            .detailGlass(CircleShape, backdrop = backdrop)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        DetailBackGlyph(Color.White, Modifier.size(22.dp))
    }
}

// ------------------------------------------------------------------ small building blocks

/** BasicText wrapper so this file doesn't depend on Material. */
@Composable
private fun DetailText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    color: Color = Color.White,
    family: FontFamily = CalSansFamily,
    weight: FontWeight? = null,
    maxLines: Int = 1,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontFamily = family,
            fontWeight = weight,
            lineHeight = lineHeight,
            textAlign = textAlign,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Press-scale + click without a ripple (no Material dependency). */
@Composable
private fun Modifier.detailPressable(
    pressedScale: Float = 0.94f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}

/** Law 9: light status/nav bar icons while this (always dark) screen is up. */
@Composable
private fun DetailLightSystemBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.detailFindActivity()?.window
        if (window == null) {
            return@DisposableEffect onDispose { }
        }
        val controller = WindowCompat.getInsetsController(window, view)
        val previousStatus = controller.isAppearanceLightStatusBars
        val previousNav = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = previousStatus
            controller.isAppearanceLightNavigationBars = previousNav
        }
    }
}

private fun Context.detailFindActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ---- formatting. Assumes Song.duration is milliseconds (Long/Int/String all accepted). ----

private fun detailDurationMs(raw: Any?): Long = when (raw) {
    is Number -> raw.toLong()
    is String -> raw.toLongOrNull() ?: 0L
    else -> 0L
}

private fun detailClock(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSec / 60L
    val seconds = (totalSec % 60L).toString().padStart(2, '0')
    return if (minutes >= 60L) {
        "${minutes / 60L}:${(minutes % 60L).toString().padStart(2, '0')}:$seconds"
    } else {
        "$minutes:$seconds"
    }
}

private fun detailStatsDuration(totalMs: Long): String {
    val totalMin = (totalMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMin / 60L
    val mins = totalMin % 60L
    return if (hours > 0L) "${hours}h ${mins}m" else "${mins}m"
}

private fun detailCreatedDate(ms: Long): Pair<String, String> {
    if (ms <= 0L) return "\u2014" to "Created"
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    val month = date.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
    return date.dayOfMonth.toString() to month
}

// ---- glyphs, drawn with Canvas so no icon library is needed ----

@Composable
private fun DetailPlayGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.24f, h * 0.14f)
            lineTo(w * 0.86f, h * 0.50f)
            lineTo(w * 0.24f, h * 0.86f)
            close()
        }
        drawPath(path, color)
        // round the corners a little
        drawPath(path, color, style = Stroke(width = w * 0.12f, join = StrokeJoin.Round))
    }
}

@Composable
private fun DetailPauseGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val barW = size.width * 0.22f
        val barH = size.height * 0.72f
        val y = (size.height - barH) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.22f, y),
            size = Size(barW, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.35f, barW * 0.35f),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.56f, y),
            size = Size(barW, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.35f, barW * 0.35f),
        )
    }
}

@Composable
private fun DetailShuffleGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            // line A: top-left -> bottom-right, arrow head at the end
            moveTo(w * 0.08f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.28f)
            lineTo(w * 0.62f, h * 0.72f)
            lineTo(w * 0.86f, h * 0.72f)
            moveTo(w * 0.76f, h * 0.62f)
            lineTo(w * 0.86f, h * 0.72f)
            lineTo(w * 0.76f, h * 0.82f)
            // line B: bottom-left -> top-right, with a gap where it passes under line A
            moveTo(w * 0.08f, h * 0.72f)
            lineTo(w * 0.30f, h * 0.72f)
            lineTo(w * 0.40f, h * 0.58f)
            moveTo(w * 0.52f, h * 0.42f)
            lineTo(w * 0.62f, h * 0.28f)
            lineTo(w * 0.86f, h * 0.28f)
            moveTo(w * 0.76f, h * 0.18f)
            lineTo(w * 0.86f, h * 0.28f)
            lineTo(w * 0.76f, h * 0.38f)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun DetailReorderGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.07f
        val xs = floatArrayOf(size.width * 0.34f, size.width * 0.66f)
        val ys = floatArrayOf(size.height * 0.24f, size.height * 0.50f, size.height * 0.76f)
        for (x in xs) {
            for (y in ys) {
                drawCircle(color, radius = r, center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun DetailBackGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.64f, h * 0.18f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.82f)
        }
        drawPath(
            path,
            color,
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun DetailDotsGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.width * 0.09f
        val cy = size.height / 2f
        for (i in 0..2) {
            drawCircle(color, radius = r, center = Offset(size.width * (0.2f + 0.3f * i), cy))
        }
    }
}

@Composable
private fun DetailCloseGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.22f),
            end = Offset(w * 0.78f, h * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.78f, h * 0.22f),
            end = Offset(w * 0.22f, h * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun DetailSearchGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = color,
            radius = w * 0.27f,
            center = Offset(w * 0.42f, h * 0.42f),
            style = Stroke(width = w * 0.09f),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.86f, h * 0.86f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun DetailNoteGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color, radius = w * 0.13f, center = Offset(w * 0.36f, h * 0.70f))
        val stem = Path().apply {
            moveTo(w * 0.48f, h * 0.70f)
            lineTo(w * 0.48f, h * 0.24f)
            lineTo(w * 0.74f, h * 0.38f)
        }
        drawPath(
            stem,
            color,
            style = Stroke(width = w * 0.07f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
