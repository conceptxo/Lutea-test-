package com.rajatxo.lutea.ui.foundation

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.rajatxo.lutea.ui.components.HoldButton
import kotlinx.coroutines.delay

/**
 * Shared bridge between SearchScreen (which owns the history) and
 * LuteaScaffold (which owns the glass backdrop).
 *
 * The clear button MUST live in the scaffold as a sibling of the layerBackdrop
 * producer. A drawBackdrop inside the search content would recursively sample
 * its own layer and crash.
 */
@Stable
class SearchHistoryOverlayState {
    var expanded by mutableStateOf(false)
    internal var onClearAll: (() -> Unit)? = null

    fun clearAll() {
        onClearAll?.invoke()
    }
}

val LocalSearchHistoryOverlay = staticCompositionLocalOf { SearchHistoryOverlayState() }

/**
 * The expanded-history clear action, rendered above the nav / mini player.
 *
 * It holds its own success state briefly after the history collapses, so the
 * "Ghee Khatam" confirmation remains readable before the capsule blurs away.
 */
@Composable
fun SearchClearAllOverlay(
    visible: Boolean,
    backdrop: LayerBackdrop?,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSuccess by remember { mutableStateOf(false) }
    val shown = visible || showSuccess
    val blurOutDuration = 650

    LaunchedEffect(visible) {
        if (visible) showSuccess = false
    }
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1400)
            showSuccess = false
        }
    }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(animationSpec = tween(180)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(220)) +
            slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(260),
            ),
        // The success capsule leaves like the intro's smoke: soft expansion,
        // fade, and a real render blur instead of a hard disappear.
        exit = fadeOut(
            animationSpec = tween(blurOutDuration, easing = LinearOutSlowInEasing),
        ) + scaleOut(
            targetScale = 1.08f,
            animationSpec = tween(blurOutDuration, easing = LinearOutSlowInEasing),
        ),
        modifier = modifier,
    ) {
        val blurProgress by animateFloatAsState(
            targetValue = if (shown) 0f else 1f,
            animationSpec = tween(blurOutDuration, easing = LinearOutSlowInEasing),
            label = "clearCapsuleBlur",
        )
        val blurPx = with(LocalDensity.current) { (18.dp * blurProgress).toPx() }

        Box(
            modifier = Modifier.graphicsLayer {
                translationY = -5.dp.toPx() * blurProgress
                if (blurPx > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(
                        blurPx,
                        blurPx,
                        Shader.TileMode.DECAL,
                    ).asComposeRenderEffect()
                } else {
                    renderEffect = null
                }
            },
        ) {
            HoldButton(
                label = "Hold to delete",
                holdingLabel = "BSDK mat kr",
                successLabel = "Ghee Khatam",
                duration = 1500,
                // Keep the success face up until the blur-out removes it.
                resetAfter = 0,
                onConfirm = {
                    showSuccess = true
                    onClearAll()
                },
                backdrop = backdrop,
            )
        }
    }
}
