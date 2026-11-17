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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private val HoldButtonWidth = 176.dp
private val HoldButtonHeight = 38.dp

/**
 * Native Compose port of Evil Buttons' HoldButton.
 *
 * Hold to fill the capsule from left to right. Releasing early shakes and
 * aborts; reaching 100% fires [onConfirm] and shows the success label for
 * [resetAfter] milliseconds (0 keeps it up until the button is removed).
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
    val shape = RoundedCornerShape(percent = 50)

    var state by remember { mutableStateOf(HoldState.Idle) }
    val progress = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

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

    val contentColor = Color.White
    val fillBrush = Brush.verticalGradient(
        0f to Color(0xFFFF6B6B),
        0.48f to Color(0xFFE53935),
        1f to Color(0xFFB71C1C),
    )
    val currentText = when (state) {
        HoldState.Idle -> label
        HoldState.Holding -> holdingLabel
        HoldState.Success -> successLabel
    }
    val currentIcon = when (state) {
        HoldState.Idle -> IdleFaceGlyph
        HoldState.Holding -> HoldingFaceGlyph
        HoldState.Success -> SuccessFaceGlyph
    }

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
            .width(HoldButtonWidth)
            .height(HoldButtonHeight)
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
        // Base label: stays perfectly still underneath the reveal.
        HoldContent(text = currentText, icon = currentIcon, color = contentColor)

        // Red fill + white label in one full-size layer. Both are clipped to the
        // same left-to-right reveal, so the text itself never moves.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    val reveal = size.width * progress.value.coerceIn(0f, 1f)
                    if (reveal > 0f) {
                        clipRect(right = reveal) {
                            drawRect(brush = fillBrush)
                            this@drawWithContent.drawContent()
                        }
                    }
                },
        ) {
            HoldContent(text = currentText, icon = currentIcon, color = contentColor)
        }
    }
}

@Composable
private fun HoldContent(
    text: String,
    icon: ImageVector,
    color: Color,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Fixed footprint: the face changes expression without shuffling
            // sideways or resizing when the label gets longer or shorter.
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(HoldFaceSize),
            )
            Spacer(Modifier.width(HoldFaceGap))
            Box(
                modifier = Modifier.width(HoldLabelWidth),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text.uppercase(),
                    color = color,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val HoldFaceSize = 20.dp
private val HoldFaceGap = 6.dp
private val HoldLabelWidth = 122.dp

private val IdleFaceGlyph: ImageVector = faceGlyph(
    name = "IdleFace",
    pathData = "M10 2a8 8 0 1 0 0 16a8 8 0 0 0 0-16M8.5 8.5a1 1 0 1 1-2 0a1 1 0 0 1 2 0m4-1a1 1 0 1 1 0 2a1 1 0 0 1 0-2m.303 4.5h.797a.5.5 0 0 1 0 1h-.797c-1.077 0-2.13.319-3.026.916a.5.5 0 1 1-.554-.832A6.45 6.45 0 0 1 12.803 12",
)

private val HoldingFaceGlyph: ImageVector = faceGlyph(
    name = "HoldingFace",
    pathData = "M2 10a8 8 0 1 1 16 0a8 8 0 0 1-16 0m6.5-1.5a.5.5 0 0 0 .26-.929l-2.503-1.5a.5.5 0 0 0-.514.858l1.217.73A1 1 0 1 0 8.5 8.5m5 0a1 1 0 0 0-.459-.841l1.216-.73a.5.5 0 0 0-.514-.858l-2.499 1.5a.5.5 0 0 0 .256.929a1 1 0 1 0 2 0m.062 4.89c.34-.308.067-.716.055-.73l-.048-.054a3 3 0 0 0-.174-.171a4 4 0 0 0-.672-.49c-.6-.354-1.5-.695-2.721-.695s-2.121.34-2.722.694c-.3.176-.522.353-.674.49a3 3 0 0 0-.223.225c-.183.219-.178.545.054.73a.5.5 0 0 0 .7-.073l.003-.003l.023-.026a2 2 0 0 1 .115-.112a3 3 0 0 1 .508-.369c.463-.272 1.19-.556 2.216-.556s1.752.284 2.213.556c.233.137.4.272.507.369a2 2 0 0 1 .137.137l.003.003c.173.213.497.26.7.075",
)

private val SuccessFaceGlyph: ImageVector = faceGlyph(
    name = "SuccessFace",
    pathData = "M2 10a8 8 0 1 1 16 0a8 8 0 0 1-16 0m5.5-.5a1 1 0 1 0 0-2a1 1 0 0 0 0 2m6-1a1 1 0 1 0-2 0a1 1 0 0 0 2 0m.062 4.89c.376-.35.067-.716.055-.73l-.048-.054a3 3 0 0 0-.174-.171a4 4 0 0 0-.672-.49c-.6-.354-1.5-.695-2.721-.695s-2.121.34-2.722.694c-.3.175-.522.353-.674.49a3 3 0 0 0-.223.225l-.014.018l-.005.006l-.002.002l-.001.001l.389.314l-.39-.313a.5.5 0 0 0 .777.629l.003-.003l.023-.026a2 2 0 0 1 .115-.112a3 3 0 0 1 .508-.369c.463-.272 1.19-.556 2.216-.556s1.752.284 2.213.556c.233.137.4.272.507.369a2 2 0 0 1 .137.137l.003.003a.5.5 0 0 0 .7.075",
)

private fun faceGlyph(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 20.dp,
        defaultHeight = 20.dp,
        viewportWidth = 20f,
        viewportHeight = 20f,
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.White),
        )
    }.build()
