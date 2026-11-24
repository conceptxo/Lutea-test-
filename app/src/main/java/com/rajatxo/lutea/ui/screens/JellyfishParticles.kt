package com.rajatxo.lutea.ui.screens

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════
//  JELLYFISH PARTICLES — shared background layer
//
//  Marine snow + a very soft blue ambient glow. Both jelly looks use this same
//  field, so the water feels identical when the renderer changes. This file is
//  deliberately separate from the jellyfish: Claude should only replace the
//  renderer files, never this one.
// ═══════════════════════════════════════════════════════════════════════════

private val ParticleGlow = Color(0xFF2E8BFF)
private const val MainParticleStrength = 0.56f
private const val BokehParticleStrength = 0.26f

@Composable
internal fun JellyfishParticles(
    clock: State<Float>,
    modifier: Modifier = Modifier,
) {
    val shader = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching { RuntimeShader(ParticleAgsl) }.getOrNull()
        } else {
            null
        }
    }
    val brush = remember(shader) { shader?.let { ShaderBrush(it) } }

    Canvas(modifier = modifier) {
        val t = clock.value
        drawParticleGlow(t)
        drawParticlePass(shader, brush, pass = 0f, strength = MainParticleStrength, t = t)
        drawParticlePass(shader, brush, pass = 1f, strength = BokehParticleStrength, t = t)
    }
}

/** Low, breathing blue pool. Kept under 5% alpha so it reads as water, not a lamp. */
private fun DrawScope.drawParticleGlow(t: Float) {
    val breathe = 0.88f + 0.12f * sin(t * 0.22f)
    val center = Offset(size.width * 0.5f, size.height * 0.42f)
    val radius = size.width * 0.92f
    drawCircle(
        brush = Brush.radialGradient(
            0f to ParticleGlow.copy(alpha = 0.040f * breathe),
            0.55f to ParticleGlow.copy(alpha = 0.018f * breathe),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Plus,
    )
}

private fun DrawScope.drawParticlePass(
    shader: RuntimeShader?,
    brush: ShaderBrush?,
    pass: Float,
    strength: Float,
    t: Float,
) {
    if (shader != null && brush != null) {
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uTime", t)
        shader.setFloatUniform("uPass", pass)
        shader.setFloatUniform("uStrength", strength)
        drawRect(brush = brush, blendMode = BlendMode.Plus)
        return
    }
    if (pass > 0.5f) return

    // Android 12 and older: a light Canvas fallback with the same cool-white
    // palette. No bokeh pass, so the cost stays low.
    val w = size.width
    val h = size.height
    for (i in 0 until 76) {
        val hx = particleHash(i, 1)
        val hy = particleHash(i, 2)
        val hs = particleHash(i, 3)
        val x = (hx * w + sin(t * 0.3f + hs * 9f) * 12f + w) % w
        val y = (hy * h + t * (5f + hs * 9f)) % h
        val tw = 0.55f + 0.45f * sin(t * (0.8f + hs * 1.5f) + i)
        val rad = (0.9f + hs * 1.8f) * density
        drawCircle(
            Color(0xFFBFE6FF).copy(alpha = 0.055f * tw),
            rad * 4f,
            Offset(x, y),
            blendMode = BlendMode.Plus,
        )
        drawCircle(
            Color(0xFFDDF3FF).copy(alpha = 0.45f * tw),
            rad,
            Offset(x, y),
            blendMode = BlendMode.Plus,
        )
    }
}

private fun particleHash(i: Int, salt: Int): Float {
    val x = sin(i * 127.1f + salt * 311.7f) * 43758.547f
    return x - floor(x)
}

/**
 * Procedural marine snow. One particle lives in each jittered grid cell, so
 * there is no per-particle data to upload and no hard edges. The shader is a
 * transparent additive layer: black pixels have alpha 0, so the page, jelly
 * renderer and history stay visible behind it.
 *
 *   pass 0 → three layers of fine snow
 *   pass 1 → one layer of big, soft bokeh discs
 */
private const val ParticleAgsl = """
uniform float2 uSize;
uniform float uTime;
uniform float uPass;
uniform float uStrength;

float h11(float n) { return fract(sin(n * 12.9898) * 43758.5453); }

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uSize.y;
    float vy = fragCoord.y / uSize.y;
    float3 col = float3(0.0);

    for (int i = 0; i < 4; i++) {
        if (uPass < 0.5 && i == 3) { continue; }
        if (uPass > 0.5 && i != 3) { continue; }

        float fl = float(i);
        float scale = 8.0;
        float thr = 0.55;
        float bright = 0.95;
        float fall = 0.07;
        float jit = 0.40;
        if (i == 1) { scale = 13.0; thr = 0.60; bright = 0.80; fall = 0.045; }
        if (i == 2) { scale = 21.0; thr = 0.66; bright = 0.65; fall = 0.030; }
        if (i == 3) { scale = 3.6;  thr = 0.78; bright = 0.32; fall = 0.100; jit = 0.18; }

        float2 p = uv * scale + float2(uTime * 0.012 * (fl + 1.0), -uTime * fall);
        float2 id = floor(p);
        float2 f = fract(p) - 0.5;

        float n = id.x * 7.13 + id.y * 131.7 + fl * 53.0;
        float r1 = h11(n);
        float r2 = h11(n + 3.1);
        float r3 = h11(n + 7.7);
        if (r3 < thr) { continue; }

        float2 pos = (float2(r1, r2) - 0.5) * jit
            + 0.05 * float2(sin(uTime * 0.5 + r1 * 6.28), cos(uTime * 0.43 + r2 * 6.28));
        float d = length(f - pos);
        float tw = 0.55 + 0.45 * sin(uTime * (0.7 + r2 * 1.6) + r3 * 50.0);
        float edge = 1.0 - smoothstep(0.28, 0.5, max(abs(f.x), abs(f.y)));

        float v;
        if (i == 3) {
            float size = 0.16 + 0.12 * r1;
            float body = exp(-d * d / (size * size));
            float q = (d - size * 0.85) / (size * 0.22);
            float ring = exp(-q * q);
            v = (body * 0.35 + ring * 0.50) * tw * edge;
        } else {
            float size = mix(0.022, 0.07, r1 * r1);
            float core = exp(-d * d / (size * size));
            float halo = exp(-d * d / (size * size * 6.0));
            v = (core + 0.22 * halo) * tw * edge;
        }

        // mostly cool white-cyan, now and then a violet-pink spark
        float3 c = mix(float3(0.62, 0.88, 1.0), float3(0.95, 0.72, 1.0), step(0.9, r2));
        col += c * v * bright;
    }

    // a little brighter toward the surface light
    col *= mix(1.25, 0.65, clamp(vy, 0.0, 1.0));
    float glow = clamp(max(col.r, max(col.g, col.b)) * uStrength, 0.0, 1.0);
    return half4(half3(col * uStrength), half(glow));
}
"""
