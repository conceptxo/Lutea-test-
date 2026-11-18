package com.rajatxo.lutea.ui.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * Lutea backdrop — Kyant liquid glass blur layer (Lutea's 3-layer approach).
 *
 * Same approach Lutea's SettingsScreen uses:
 *   Layer 1: Capture layer — NOT clickable, NOT scrollable.
 *            layerBackdrop() captures the background behind it.
 *   Layer 2: Visible background (image, gradient, etc.) drawn over the capture.
 *   Layer 3: Foreground content — uses drawBackdrop() to blur the captured
 *            background, producing frosted-glass effect.
 *
 * Why this works:
 *   The crash happens when BOTH layerBackdrop AND drawBackdrop are inside a
 *   scrollable container. By keeping layerBackdrop OUTSIDE the scroll (in Layer 1),
 *   the kyant criteria is met — no crash.
 *
 * Critical ordering: Layer 1 (the capture layer with layerBackdrop) must come
 * BEFORE Layer 3 (content with drawBackdrop) in the z-order. Kyant content lives
 * on TOP of the capture — that's why drawBackdrop can blur it.
 *
 * Usage:
 *   LuteaBackdrop(background = { Background() }) { backdrop ->
 *     // Your content here — use Modifier.drawBackdrop(backdrop, ...) on individual
 *     // elements to give them the frosted-glass blur effect.
 *     PillTextField(..., backdrop = backdrop)
 *     ContinueButton(..., backdrop = backdrop)
 *   }
 *
 * @param background        composable that renders the visible background.
 *                          It is also rendered inside the layerBackdrop capture Box
 *                          (twice per frame) unless captureBackground is supplied.
 * @param captureBackground optional cheap/static background for the capture layer.
 *                          Use this when the real background is animated, so the
 *                          glass blur does not run the animation twice.
 * @param content           foreground content — elements here can use
 *                          Modifier.drawBackdrop(...) to blur the captured background.
 */
@Composable
fun LuteaBackdrop(
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit,
    captureBackground: (@Composable () -> Unit)? = null,
    content: @Composable (backdrop: com.kyant.backdrop.backdrops.LayerBackdrop) -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()
    val backdrop = rememberLayerBackdrop(graphicsLayer = graphicsLayer) {
        drawContent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Layer 1: Capture layer (NOT clickable, NOT scrollable) ────────
        // layerBackdrop records its content for drawBackdrop. It draws its
        // content too, so the visible background sits on top of it.
        // CRITICAL: layerBackdrop() must be OUTSIDE any scrollable container to
        // avoid the kyant crash. This Box is not scrollable → safe.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) {
            if (captureBackground != null) captureBackground() else background()
        }

        // ── Layer 2: Visible background ───────────────────────────────────
        // Opaque background drawn over the capture layer. When the real
        // background is animated, captureBackground can stay static and cheap.
        background()

        // ── Layer 3: Foreground content (ON TOP of everything) ──────────
        // Elements here can call Modifier.drawBackdrop(backdrop, ...) to blur
        // whatever is behind them (the captured background).
        // This is the TOPMOST layer — kyant content sits above the background.
        content(backdrop)
    }
}
