package com.rajatxo.lutea.ui.foundation

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlin.math.roundToInt

private const val SoundPrefs = "lutea_search"
private const val TypingSoundKey = "typing_sound_enabled"
private const val TypingVolumeKey = "typing_sound_volume"

private val SoundCream = Color(0xFFFCF2C7)

/** Only the machine's typing sound is configurable now. */
@Stable
internal class SearchSoundOverlayState(
    initialTypingSoundEnabled: Boolean,
    initialTypingSoundVolume: Float = TypewriterSound.DEFAULT_LEVEL,
) {
    var typingSoundEnabled by mutableStateOf(initialTypingSoundEnabled)
    var typingSoundVolume by mutableStateOf(initialTypingSoundVolume.coerceIn(0f, 1f))
    var expanded by mutableStateOf(false)
}

internal val LocalSearchSoundOverlay = staticCompositionLocalOf {
    SearchSoundOverlayState(initialTypingSoundEnabled = true)
}

internal fun loadTypingSoundEnabled(context: Context): Boolean {
    return context.getSharedPreferences(SoundPrefs, Context.MODE_PRIVATE)
        .getBoolean(TypingSoundKey, true)
}

internal fun loadTypingSoundVolume(context: Context): Float {
    return context.getSharedPreferences(SoundPrefs, Context.MODE_PRIVATE)
        .getFloat(TypingVolumeKey, TypewriterSound.DEFAULT_LEVEL)
        .coerceIn(0f, 1f)
}

/**
 * Long-press Search tab opens this glass card. It is a sibling of the
 * layerBackdrop producer so the glass can sample the page behind it.
 */
@Composable
internal fun SearchSoundOverlay(
    state: SearchSoundOverlayState,
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
            SoundCard(
                typingSoundEnabled = state.typingSoundEnabled,
                typingSoundVolume = state.typingSoundVolume,
                backdrop = backdrop,
                onTypingSoundToggle = { enabled ->
                    state.typingSoundEnabled = enabled
                    context.getSharedPreferences(SoundPrefs, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(TypingSoundKey, enabled)
                        .apply()
                    if (enabled) {
                        TypewriterSound.init(context)
                        TypewriterSound.setLevel(state.typingSoundVolume)
                    } else {
                        TypewriterSound.stopAll()
                    }
                },
                onTypingVolumeChange = { volume ->
                    state.typingSoundVolume = volume.coerceIn(0f, 1f)
                    TypewriterSound.setLevel(state.typingSoundVolume)
                },
                onTypingVolumeChangeFinished = {
                    context.getSharedPreferences(SoundPrefs, Context.MODE_PRIVATE)
                        .edit()
                        .putFloat(TypingVolumeKey, state.typingSoundVolume)
                        .apply()
                },
            )
        }
    }
}

@Composable
private fun SoundCard(
    typingSoundEnabled: Boolean,
    typingSoundVolume: Float,
    backdrop: LayerBackdrop?,
    onTypingSoundToggle: (Boolean) -> Unit,
    onTypingVolumeChange: (Float) -> Unit,
    onTypingVolumeChangeFinished: () -> Unit,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(12.dp),
    ) {
        Text(
            text = "SEARCH SOUND",
            color = SoundCream.copy(alpha = 0.88f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(9.dp))
        TypingSoundSection(
            enabled = typingSoundEnabled,
            volume = typingSoundVolume,
            onToggle = { onTypingSoundToggle(!typingSoundEnabled) },
            onVolumeChange = onTypingVolumeChange,
            onVolumeChangeFinished = onTypingVolumeChangeFinished,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "TAP TO CLOSE",
            color = SoundCream.copy(alpha = 0.26f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun TypingSoundSection(
    enabled: Boolean,
    volume: Float,
    onToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TypingSoundRow(
            enabled = enabled,
            onToggle = onToggle,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Volume",
                color = SoundCream.copy(alpha = 0.38f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "${(volume * 100f).roundToInt()}%",
                color = SoundCream.copy(alpha = 0.38f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            onValueChangeFinished = onVolumeChangeFinished,
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = SoundCream.copy(alpha = 0.92f),
                activeTrackColor = SoundCream.copy(alpha = 0.80f),
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                disabledThumbColor = SoundCream.copy(alpha = 0.34f),
                disabledActiveTrackColor = SoundCream.copy(alpha = 0.22f),
                disabledInactiveTrackColor = Color.White.copy(alpha = 0.07f),
            ),
            modifier = Modifier.fillMaxWidth(),
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
                color = SoundCream.copy(alpha = 0.90f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = if (enabled) "Typewriter keys · on" else "Typewriter keys · off",
                color = SoundCream.copy(alpha = 0.38f),
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
                if (enabled) SoundCream.copy(alpha = 0.92f)
                else Color.White.copy(alpha = 0.10f),
            )
            .border(
                width = 1.dp,
                color = if (enabled) SoundCream.copy(alpha = 0.40f)
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
                    else SoundCream.copy(alpha = 0.72f),
                ),
        )
    }
}
