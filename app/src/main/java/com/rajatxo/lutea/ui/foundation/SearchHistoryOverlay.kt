package com.rajatxo.lutea.ui.foundation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
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
 * "Deleted" confirmation remains readable instead of disappearing with the
 * deck.
 */
@Composable
fun SearchClearAllOverlay(
    visible: Boolean,
    backdrop: LayerBackdrop?,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSuccess by remember { mutableStateOf(false) }

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
        visible = visible || showSuccess,
        enter = fadeIn(animationSpec = tween(180)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(220)) +
            slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(260),
            ),
        exit = fadeOut(animationSpec = tween(160)) +
            scaleOut(targetScale = 0.94f, animationSpec = tween(160)),
        modifier = modifier,
    ) {
        HoldButton(
            label = "Hold to Clear all",
            holdingLabel = "Keep holding…",
            successLabel = "Deleted",
            duration = 1500,
            resetAfter = 1400,
            onConfirm = {
                showSuccess = true
                onClearAll()
            },
            backdrop = backdrop,
        )
    }
}
