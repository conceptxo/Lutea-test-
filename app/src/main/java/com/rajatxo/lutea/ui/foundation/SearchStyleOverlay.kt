package com.rajatxo.lutea.ui.foundation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.lutea.audio.TypewriterSound
import kotlin.math.min
import kotlin.math.sin

/** The two ways the top half of the search page can look. */
internal enum class SearchSurfaceStyle { JELLY, CARDS }

/** The two ways previous searches can be shown below the machine. */
internal enum class SearchHistoryStyle { PAPER, SIMPLE }

/** The two jellyfish renderers the search page can use. */
internal enum class JellyLook { CURRENT, CLASSIC }

private const val StylePrefs = "lutea_search"
private const val StyleKey = "search_surface_style"
private const val HistoryStyleKey = "search_history_style"
private const val JellyLookKey = "jelly_look_style"
private const val TypingSoundKey = "typing_sound_enabled"
private const val JellyDefaultMigrationKey = "jelly_default_v1"

private val StyleCream = Color(0xFFFCF2C7)
private val StyleJelly = Color(0xFF9188FF)
private val StyleJellyPink = Color(0xFFFFA0C6)

@Stable
internal class SearchStyleOverlayState(
    initialStyle: SearchSurfaceStyle,
    initialHistoryStyle: SearchHistoryStyle,
    initialTypingSoundEnabled: Boolean,
    initialJellyLook: JellyLook = JellyLook.CURRENT,
) {
    var style by mutableStateOf(initialStyle)
    var historyStyle by mutableStateOf(initialHistoryStyle)
    var jellyLook by mutableStateOf(initialJellyLook)
    var typingSoundEnabled by mutableStateOf(initialTypingSoundEnabled)
    var expanded by mutableStateOf(false)
}

internal val LocalSearchStyleOverlay = staticCompositionLocalOf {
    SearchStyleOverlayState(
        initialStyle = SearchSurfaceStyle.JELLY,
        initialHistoryStyle = SearchHistoryStyle.PAPER,
        initialTypingSoundEnabled = true,
        initialJellyLook = JellyLook.CURRENT,
    )
}

internal fun loadSearchSurfaceStyle(context: Context): SearchSurfaceStyle {
    val prefs = context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
    if (!prefs.getBoolean(JellyDefaultMigrationKey, false)) {
        // Jelly Drift is the product default. Apply it once on this update;
        // after that the user's own choice (including Card Stack) is kept.
        prefs.edit()
            .putString(StyleKey, SearchSurfaceStyle.JELLY.name)
            .putBoolean(JellyDefaultMigrationKey, true)
            .apply()
        return SearchSurfaceStyle.JELLY
    }
    val saved = prefs.getString(StyleKey, null)
    return SearchSurfaceStyle.entries.firstOrNull { it.name == saved }
        ?: SearchSurfaceStyle.JELLY
}

internal fun loadSearchHistoryStyle(context: Context): SearchHistoryStyle {
    val saved = context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
        .getString(HistoryStyleKey, null)
    return SearchHistoryStyle.entries.firstOrNull { it.name == saved }
        ?: SearchHistoryStyle.PAPER
}

internal fun loadJellyLook(context: Context): JellyLook {
    val saved = context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
        .getString(JellyLookKey, null)
    return JellyLook.entries.firstOrNull { it.name == saved }
        ?: JellyLook.CURRENT
}

internal fun loadTypingSoundEnabled(context: Context): Boolean {
    return context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
        .getBoolean(TypingSoundKey, true)
}

/**
 * The glass search-style card. It has no permanent button any more: the card
 * is opened by holding the Search tab on the nav, and the countdown capsule
 * itself lives beside the tab.
 *
 * This lives in LuteaScaffold as a sibling of the layerBackdrop producer, the
 * same way the Hold to delete button does. Calling drawBackdrop inside the
 * search content itself would sample its own capture layer and crash.
 */
@Composable
internal fun SearchStyleOverlay(
    state: SearchStyleOverlayState,
    backdrop: LayerBackdrop?,
    visible: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(visible) {
        if (!visible) state.expanded = false
    }
    if (!visible) return
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        if (state.expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { state.expanded = false },
                    ),
            )
        }

        AnimatedVisibility(
            visible = state.expanded,
            enter = fadeIn(tween(180)) +
                scaleIn(
                    animationSpec = tween(230, easing = FastOutSlowInEasing),
                    initialScale = 0.90f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                ) +
                slideInVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 4 },
                ),
            exit = fadeOut(tween(150)) +
                scaleOut(
                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                    targetScale = 0.94f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                ) +
                slideOutVertically(
                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                    targetOffsetY = { it / 5 },
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding),
        ) {
            StyleCard(
                selected = state.style,
                historyStyle = state.historyStyle,
                jellyLook = state.jellyLook,
                typingSoundEnabled = state.typingSoundEnabled,
                backdrop = backdrop,
                onSelect = { style ->
                    state.style = style
                    state.expanded = false
                    context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
                        .edit()
                        .putString(StyleKey, style.name)
                        .apply()
                },
                onHistoryStyleSelect = { historyStyle ->
                    state.historyStyle = historyStyle
                    state.expanded = false
                    context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
                        .edit()
                        .putString(HistoryStyleKey, historyStyle.name)
                        .apply()
                },
                onJellyLookSelect = { jellyLook ->
                    state.jellyLook = jellyLook
                    // A jelly choice is only meaningful on the jelly page, so
                    // picking one also brings that page back.
                    state.style = SearchSurfaceStyle.JELLY
                    state.expanded = false
                    context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
                        .edit()
                        .putString(JellyLookKey, jellyLook.name)
                        .putString(StyleKey, SearchSurfaceStyle.JELLY.name)
                        .apply()
                },
                onTypingSoundToggle = { enabled ->
                    state.typingSoundEnabled = enabled
                    context.getSharedPreferences(StylePrefs, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(TypingSoundKey, enabled)
                        .apply()
                    if (enabled) {
                        TypewriterSound.init(context)
                    } else {
                        TypewriterSound.stopAll()
                    }
                },
            )
        }
    }
}

@Composable
private fun StyleCard(
    selected: SearchSurfaceStyle,
    historyStyle: SearchHistoryStyle,
    jellyLook: JellyLook,
    typingSoundEnabled: Boolean,
    backdrop: LayerBackdrop?,
    onSelect: (SearchSurfaceStyle) -> Unit,
    onHistoryStyleSelect: (SearchHistoryStyle) -> Unit,
    onJellyLookSelect: (JellyLook) -> Unit,
    onTypingSoundToggle: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val glass = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                colorControls(
                    brightness = 0.05f,
                    contrast = 1f,
                    saturation = 1.35f,
                )
                blur(24.dp.toPx())
            },
            onDrawSurface = {
                drawRect(Color.Black.copy(alpha = 0.34f))
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.13f),
                        0.48f to Color.White.copy(alpha = 0.035f),
                        1f to Color.Transparent,
                    ),
                )
            },
        )
    } else {
        Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color(0xFF2B2724),
                1f to Color(0xFF100F0E),
            ),
            shape = shape,
        )
    }

    Column(
        modifier = Modifier
            .width(224.dp)
            .shadow(24.dp, shape, clip = false)
            .clip(shape)
            .then(glass)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.52f),
                    0.50f to Color.White.copy(alpha = 0.16f),
                    1f to Color.White.copy(alpha = 0.08f),
                ),
                shape = shape,
            )
            // Taps on the card's background should not fall through to the scrim.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SEARCH STYLE",
                color = StyleCream.copy(alpha = 0.88f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${SearchSurfaceStyle.entries.size}",
                color = StyleCream.copy(alpha = 0.32f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.size(8.dp))

        StyleOption(
            title = "Jelly Drift",
            subtitle = "Living backdrop + list",
            selected = selected == SearchSurfaceStyle.JELLY,
            preview = { JellyPreview(Modifier.fillMaxSize()) },
            onClick = { onSelect(SearchSurfaceStyle.JELLY) },
        )
        Spacer(Modifier.size(6.dp))
        StyleOption(
            title = "Card Stack",
            subtitle = "Swipe the songs",
            selected = selected == SearchSurfaceStyle.CARDS,
            preview = { CardsPreview(Modifier.fillMaxSize()) },
            onClick = { onSelect(SearchSurfaceStyle.CARDS) },
        )

        Spacer(Modifier.height(9.dp))
        JellyLookRow(
            selected = jellyLook,
            onSelect = onJellyLookSelect,
        )

        Spacer(Modifier.height(9.dp))
        TypingSoundRow(
            enabled = typingSoundEnabled,
            onToggle = { onTypingSoundToggle(!typingSoundEnabled) },
        )

        Spacer(Modifier.height(9.dp))
        HistoryStyleRow(
            selected = historyStyle,
            onSelect = onHistoryStyleSelect,
        )

        Spacer(Modifier.size(7.dp))
        Text(
            text = "TAP TO SWITCH",
            color = StyleCream.copy(alpha = 0.26f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun JellyLookRow(
    selected: JellyLook,
    onSelect: (JellyLook) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "JELLY UI",
            color = StyleCream.copy(alpha = 0.44f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
        )
        Spacer(Modifier.size(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StyleChip(
                title = "Current",
                subtitle = "Physics drift",
                selected = selected == JellyLook.CURRENT,
                onClick = { onSelect(JellyLook.CURRENT) },
                modifier = Modifier.weight(1f),
            )
            StyleChip(
                title = "Classic",
                subtitle = "Original 3D",
                selected = selected == JellyLook.CLASSIC,
                onClick = { onSelect(JellyLook.CLASSIC) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HistoryStyleRow(
    selected: SearchHistoryStyle,
    onSelect: (SearchHistoryStyle) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "HISTORY UI",
            color = StyleCream.copy(alpha = 0.44f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
        )
        Spacer(Modifier.size(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StyleChip(
                title = "Paper",
                subtitle = "Tear-off stack",
                selected = selected == SearchHistoryStyle.PAPER,
                onClick = { onSelect(SearchHistoryStyle.PAPER) },
                modifier = Modifier.weight(1f),
            )
            StyleChip(
                title = "Simple",
                subtitle = "Plain text list",
                selected = selected == SearchHistoryStyle.SIMPLE,
                onClick = { onSelect(SearchHistoryStyle.SIMPLE) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StyleChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) Color.White.copy(alpha = 0.10f)
                else Color.White.copy(alpha = 0.025f)
            )
            .border(
                width = 1.dp,
                color = if (selected) StyleCream.copy(alpha = 0.38f)
                else Color.White.copy(alpha = 0.07f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            color = StyleCream.copy(alpha = 0.92f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = subtitle,
            color = StyleCream.copy(alpha = 0.40f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TypingSoundRow(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.025f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.07f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Typing sound",
                color = StyleCream.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = if (enabled) "Typewriter keys · on" else "Typewriter keys · off",
                color = StyleCream.copy(alpha = 0.38f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(8.dp))
        TypingSoundSwitch(enabled = enabled)
    }
}

@Composable
private fun TypingSoundSwitch(enabled: Boolean) {
    val knobX by animateDpAsState(
        targetValue = if (enabled) 18.dp else 2.dp,
        animationSpec = tween(170, easing = FastOutSlowInEasing),
        label = "typingSoundKnob",
    )
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(20.dp)
            .clip(CircleShape)
            .background(
                if (enabled) StyleCream.copy(alpha = 0.92f)
                else Color.White.copy(alpha = 0.10f)
            )
            .border(
                width = 1.dp,
                color = if (enabled) StyleCream.copy(alpha = 0.40f)
                else Color.White.copy(alpha = 0.15f),
                shape = CircleShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = knobX)
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) Color(0xFF17130F)
                    else StyleCream.copy(alpha = 0.72f)
                ),
        )
    }
}

@Composable
private fun StyleOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.025f))
            .border(
                width = 1.dp,
                color = if (selected) StyleCream.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.07f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp)),
        ) {
            preview()
        }
        Spacer(Modifier.size(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = StyleCream.copy(alpha = 0.94f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = subtitle,
                color = StyleCream.copy(alpha = 0.44f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(6.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (selected) StyleCream.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StyleCream),
                )
            }
        }
    }
}

@Composable
private fun JellyPreview(modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition(label = "jellyPreview").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jellyPreviewBob",
    )
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF302742), Color(0xFF15121E)),
            ),
        )
        val cx = size.width * 0.5f
        val cy = size.height * (0.48f - 0.035f * bob)
        val r = min(size.width, size.height) * 0.245f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(StyleJelly.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 2.2f,
            ),
            radius = r * 2.2f,
            center = Offset(cx, cy),
        )
        val bell = Path().apply {
            moveTo(cx - r, cy)
            cubicTo(cx - r, cy - r * 1.18f, cx + r, cy - r * 1.18f, cx + r, cy)
            close()
        }
        drawPath(
            path = bell,
            brush = Brush.verticalGradient(
                colors = listOf(StyleJellyPink.copy(alpha = 0.95f), StyleJelly.copy(alpha = 0.72f)),
                startY = cy - r,
                endY = cy,
            ),
        )
        drawLine(
            color = StyleJellyPink.copy(alpha = 0.85f),
            start = Offset(cx - r, cy),
            end = Offset(cx + r, cy),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
        for (i in -2..2) {
            val x = cx + i * r * 0.36f
            drawLine(
                color = StyleJellyPink.copy(alpha = 0.58f),
                start = Offset(x, cy + r * 0.08f),
                end = Offset(x + sin(bob * 3.14f + i) * r * 0.24f, cy + r * 1.55f),
                strokeWidth = 1.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun CardsPreview(modifier: Modifier = Modifier) {
    val drift by rememberInfiniteTransition(label = "cardsPreview").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cardsPreviewDrift",
    )
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A211D), Color(0xFF151110)),
            ),
        )
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFF33313A),
            topLeft = Offset(w * 0.22f + drift * w * 0.03f, h * 0.18f),
            size = Size(w * 0.48f, h * 0.68f),
            cornerRadius = CornerRadius(4f, 4f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF59516A), Color(0xFF24212B)),
                startY = h * 0.16f,
                endY = h * 0.86f,
            ),
            topLeft = Offset(w * 0.36f - drift * w * 0.03f, h * 0.14f),
            size = Size(w * 0.48f, h * 0.72f),
            cornerRadius = CornerRadius(4f, 4f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = min(w, h) * 0.075f,
            center = Offset(w * 0.60f - drift * w * 0.03f, h * 0.66f),
        )
        drawLine(
            color = Color(0xFF111111).copy(alpha = 0.7f),
            start = Offset(w * 0.575f - drift * w * 0.03f, h * 0.66f),
            end = Offset(w * 0.62f - drift * w * 0.03f, h * 0.66f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF111111).copy(alpha = 0.7f),
            start = Offset(w * 0.62f - drift * w * 0.03f, h * 0.66f),
            end = Offset(w * 0.60f - drift * w * 0.03f, h * 0.625f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF111111).copy(alpha = 0.7f),
            start = Offset(w * 0.62f - drift * w * 0.03f, h * 0.66f),
            end = Offset(w * 0.60f - drift * w * 0.03f, h * 0.695f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.28f),
            start = Offset(w * 0.44f - drift * w * 0.03f, h * 0.24f),
            end = Offset(w * 0.74f - drift * w * 0.03f, h * 0.24f),
            strokeWidth = 1f,
            cap = StrokeCap.Round,
        )
    }
}
