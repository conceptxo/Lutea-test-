package com.rajatxo.lutea.ui.foundation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay

private val HoldCream = Color(0xFFFCF2C7)

/**
 * Wraps the Search tab with a hold-to-open gesture.
 *
 * A normal tap still reaches the tab underneath. Holding for five seconds
 * opens the search-style card: the first two seconds are just the hold, then
 * a glass capsule counts 3-2-1 above the tab. Moving the finger or letting go
 * cancels it, so swiping the nav never opens the card by accident.
 */
@Composable
internal fun SearchTabHold(
    onComplete: () -> Unit,
    backdrop: LayerBackdrop? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val latestOnComplete by rememberUpdatedState(onComplete)
    val vibrator = remember(context) {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun pulse(durationMs: Long, amplitude: Int) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                device.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // A missing vibrator must never interrupt the hold.
        }
    }

    var holding by remember { mutableStateOf(false) }
    var showCapsule by remember { mutableStateOf(false) }
    var remaining by remember { mutableIntStateOf(3) }

    LaunchedEffect(holding) {
        if (!holding) {
            showCapsule = false
            return@LaunchedEffect
        }

        // Hold for two seconds before the countdown starts, then 3-2-1.
        remaining = 3
        delay(2_000L)
        if (!holding) return@LaunchedEffect

        showCapsule = true
        for (number in 3 downTo 1) {
            remaining = number
            pulse(durationMs = 12L, amplitude = 36)
            delay(1_000L)
            if (!holding) return@LaunchedEffect
        }

        showCapsule = false
        pulse(durationMs = 40L, amplitude = 90)
        latestOnComplete()
        holding = false
    }

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (showCapsule) 1f else 0f,
        animationSpec = tween(150),
        label = "searchHoldBubbleAlpha",
    )
    val bubbleScale by animateFloatAsState(
        targetValue = if (showCapsule) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "searchHoldBubbleScale",
    )
    val bubbleVisible = showCapsule || bubbleAlpha > 0.01f

    Layout(
        content = {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        holding = true
                        val slop = with(density) { 22.dp.toPx() }

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: break
                            if (!change.pressed) break
                            if ((change.position - down.position).getDistance() > slop) {
                                break
                            }
                        }
                        holding = false
                    }
                },
            ) {
                content()
            }

            if (bubbleVisible) {
                HoldTimerCapsule(
                    number = remaining,
                    alpha = bubbleAlpha,
                    scale = bubbleScale,
                    backdrop = backdrop,
                )
            }
        },
    ) { measurables, constraints ->
        val contentPlaceable = measurables[0].measure(constraints)
        val bubblePlaceable = measurables.getOrNull(1)?.measure(Constraints())
        val gap = with(density) { 7.dp.roundToPx() }

        layout(contentPlaceable.width, contentPlaceable.height) {
            contentPlaceable.place(0, 0)
            bubblePlaceable?.let { bubble ->
                bubble.place(
                    x = (contentPlaceable.width - bubble.width) / 2,
                    y = -bubble.height - gap,
                )
            }
        }
    }
}

@Composable
private fun HoldTimerCapsule(
    number: Int,
    alpha: Float,
    scale: Float,
    backdrop: LayerBackdrop?,
) {
    val shape = RoundedCornerShape(50)
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
                blur(20.dp.toPx())
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

    Row(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .width(178.dp)
            .shadow(18.dp, shape, clip = false)
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
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "HOLD FOR STYLE",
            color = HoldCream.copy(alpha = 0.72f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.25.sp,
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(HoldCream),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = Color(0xFF1A1712),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
