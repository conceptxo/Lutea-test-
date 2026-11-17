package com.rajatxo.lutea.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class HoldState { Idle, Holding, Success }

/**
 * Native Compose port of Evil Buttons' HoldButton.
 *
 * Hold to fill the capsule from left to right. Releasing early shakes and
 * aborts; reaching 100% fires [onConfirm] and shows the success label for
 * [resetAfter] milliseconds.
 *
 * The button is deliberately small and self-contained. When [backdrop] is
 * available it is a sibling of the layerBackdrop producer, so it can safely
 * sample the page behind it with Coral's glass recipe.
 */
@Composable
fun HoldButton(
    label: String,
    holdingLabel: String,
    successLabel: String,
    duration: Int,
    resetAfter: Int,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    onAbort: ((Float) -> Unit)? = null,
    backdrop: LayerBackdrop? = null,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val shape = RoundedCornerShape(percent = 50)

    var state by remember { mutableStateOf(HoldState.Idle) }
    val progress = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var buttonWidthPx by remember { mutableIntStateOf(0) }

    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnAbort by rememberUpdatedState(onAbort)

    val pressScale by animateFloatAsState(
        targetValue = if (state == HoldState.Holding) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "holdButtonPress",
    )

    fun startHold() {
        if (state != HoldState.Idle) return
        holdJob?.cancel()
        state = HoldState.Holding
        holdJob = scope.launch {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing),
            )
            state = HoldState.Success
            currentOnConfirm()
            if (resetAfter > 0) {
                delay(resetAfter.toLong())
                state = HoldState.Idle
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    fun cancelHold() {
        if (state != HoldState.Holding) return
        holdJob?.cancel()
        val reached = progress.value
        state = HoldState.Idle
        currentOnAbort?.invoke(reached)
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
            )
        }
        scope.launch {
            shake.snapTo(0f)
            shake.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = LinearEasing),
            )
        }
    }

    val glassSurface = if (backdrop != null) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                colorControls(
                    brightness = 0.05f,
                    contrast = 1f,
                    saturation = 1.4f,
                )
                blur(20f.dp.toPx())
            },
            onDrawSurface = {
                drawRect(Color.Black.copy(alpha = 0.35f))
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
        Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color(0xFF353535),
                1f to Color(0xFF272727),
            ),
            shape = shape,
        )
    }

    val contentColor = Color(0xFFF5F5F5)
    val fillContentColor = Color(0xFF171717)
    val fillBrush = Brush.verticalGradient(
        0f to Color(0xFFFDFDFD),
        1f to Color(0xFFE5E5E5),
    )
    val currentText = when (state) {
        HoldState.Idle -> label
        HoldState.Holding -> holdingLabel
        HoldState.Success -> successLabel
    }
    val isSuccess = state == HoldState.Success
    val showBaseLabel = progress.value < 0.999f
    val fillWidth = progress.value.coerceIn(0f, 1f)
    val buttonWidthDp = with(density) { buttonWidthPx.toDp() }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale

                // React's keyframes: 0 → -4 → 4 → -2 → 2 → 0 over 300ms.
                val segment = (shake.value * 5f).toInt().coerceIn(0, 4)
                val fraction = (shake.value * 5f) - segment
                val d = 4.dp.toPx()
                translationX = when (segment) {
                    0 -> -d * fraction
                    1 -> -d + 2f * d * fraction
                    2 -> d - 3f * d * fraction
                    3 -> -0.5f * d + d * fraction
                    else -> 0.5f * d * (1f - fraction)
                }
            }
            .width(190.dp)
            .height(38.dp)
            .onSizeChanged { buttonWidthPx = it.width }
            .clip(shape)
            .then(glassSurface)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.52f),
                    0.50f to Color.White.copy(alpha = 0.16f),
                    1f to Color.White.copy(alpha = 0.08f),
                ),
                shape = shape,
            )
            .pointerInput(duration) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (state != HoldState.Idle) return@awaitEachGesture
                    down.consume()
                    startHold()

                    // The pointer owns the gesture until it lifts. Moving off the
                    // capsule does not fire it early; it keeps filling until release.
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                    }
                    cancelHold()
                }
            },
    ) {
        // Base label: visible wherever the light fill has not reached yet.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (showBaseLabel) 1f else 0f },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HoldContent(text = currentText, success = isSuccess, color = contentColor)
        }

        // Light fill, clipped to its animated width. The dark label is laid out at
        // the full capsule width so it is revealed in place instead of sliding.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(fillWidth)
                .clipToBounds()
                .background(fillBrush),
        ) {
            Row(
                modifier = Modifier
                    // requiredWidth ignores the narrow fill's constraints; the
                    // parent's clipToBounds then reveals the label in place.
                    .requiredWidth(buttonWidthDp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HoldContent(
                    text = currentText,
                    success = isSuccess,
                    color = fillContentColor,
                )
            }
        }
    }
}

@Composable
private fun HoldContent(
    text: String,
    success: Boolean,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (success) {
            Icon(
                imageVector = CheckGlyph,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            maxLines = 1,
        )
    }
}

private val CheckGlyph: ImageVector = ImageVector.Builder(
    name = "Check",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.25f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(20f, 6f)
        lineTo(9f, 17f)
        lineTo(4f, 12f)
    }
}.build()
