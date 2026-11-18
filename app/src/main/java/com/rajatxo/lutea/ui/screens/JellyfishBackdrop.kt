package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.rajatxo.lutea.ui.foundation.JellyLook

// ═══════════════════════════════════════════════════════════════════════════
//  JELLYFISH BACKDROP — scene wrapper
//
//  This file owns the things that must stay identical between jelly looks:
//  frame clock, search blur/dim, bottom fade and the shared particle field.
//
//  The jellyfish themselves live in two independent renderer files:
//      JellyfishDrift.kt    — DriftJellyfish(clock, modifier)
//      JellyfishClassic.kt  — ClassicJellyfish(clock, modifier)
//
//  Particles live in JellyfishParticles.kt. Renderers must not draw particles
//  or a full-screen background.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun JellyfishBackdrop(
    searching: Boolean,
    look: JellyLook = JellyLook.CLASSIC,
    modifier: Modifier = Modifier,
) {
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                clock.floatValue += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            }
            last = now
        }
    }

    // The classic look originally used a much lighter melt; keep that
    // personality while the current physics look gets the deeper blur.
    val blurRadius by animateFloatAsState(
        targetValue = when {
            !searching -> 0f
            look == JellyLook.CLASSIC -> 6f
            else -> 24f
        },
        animationSpec = tween(500),
        label = "jellyBlur",
    )
    val dim by animateFloatAsState(
        targetValue = if (searching) 0.42f else 1f,
        animationSpec = tween(500),
        label = "jellyDim",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = dim
                compositingStrategy = CompositingStrategy.Offscreen
            }
            // Soft fade at the bottom so nothing is cut off above the machine.
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        0.78f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            .blur(blurRadius.dp, BlurredEdgeTreatment.Unbounded),
    ) {
        JellyfishParticles(
            clock = clock,
            modifier = Modifier.fillMaxSize(),
        )
        when (look) {
            JellyLook.CURRENT -> DriftJellyfish(
                clock = clock,
                modifier = Modifier.fillMaxSize(),
            )

            JellyLook.CLASSIC -> ClassicJellyfish(
                clock = clock,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
