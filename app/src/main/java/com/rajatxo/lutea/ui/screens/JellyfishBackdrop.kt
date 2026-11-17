package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════
//  JELLYFISH BACKDROP
//
//  Fills the empty area above the typewriter while nothing is typed.
//  Pure Canvas, no images, no extra libraries. Each jellyfish is a small 3D
//  model (dome rings, ribs, petals, rim spots, oral arms, tentacles) that is
//  rotated, pulsed and perspective-projected every frame.
//
//  searching = true  → everything blurs + dims, the result list shows on top.
// ═══════════════════════════════════════════════════════════════════════════

private const val JTau = 6.2831855f
private const val JHalfPi = 1.5707964f

private val JellyRim = Color(0xFF8F86FF)      // violet-blue at the bell edge
private val JellyApex = Color(0xFFE56FE3)     // hot pink toward the top
private val JellyRibCol = Color(0xFFEBDDFF)
private val JellyTentacle = Color(0xFFDDBBFF)
private val JellyArm = Color(0xFFF2B6FF)
private val JellySpot = Color(0xFF3B0E2B)     // the dark maroon blotches on the rim
private val JellyPetalBlue = Color(0xFF6F8CFF)
private val JellyPetalPink = Color(0xFFFF7ADB)

private class JellySpec(
    val xFrac: Float,    // horizontal position (0..1)
    val scale: Float,    // size relative to the main one
    val rise: Float,     // screens per second it floats up
    val offset: Float,   // starting point on its journey
    val alpha: Float,    // depth: far ones are fainter
    val seed: Float,
)

// Drawn back to front: the small, faint ones first, the big hero last.
private val JellyCast = listOf(
    JellySpec(0.84f, 0.42f, 1f / 26f, 0.15f, 0.45f, 4.7f),
    JellySpec(0.16f, 0.52f, 1f / 30f, 0.90f, 0.55f, 2.1f),
    JellySpec(0.50f, 1.00f, 1f / 38f, 0.38f, 1.00f, 0f),
)

@Composable
fun JellyfishBackdrop(
    searching: Boolean,
    modifier: Modifier = Modifier,
) {
    val clock = remember { mutableFloatStateOf(0f) }
    // Keep the swim running under the results list; the blur is only a layer.
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

    // typing → jellyfish melt into a soft blur, the songs list takes over
    val blurRadius by animateFloatAsState(
        targetValue = if (searching) 6f else 0f,
        animationSpec = tween(500),
        label = "jellyBlur",
    )
    val dim by animateFloatAsState(
        targetValue = if (searching) 0.42f else 1f,
        animationSpec = tween(500),
        label = "jellyDim",
    )

    Canvas(
        modifier = modifier
            .graphicsLayer {
                alpha = dim
                compositingStrategy = CompositingStrategy.Offscreen
            }
            // soft fade at the bottom so nothing is cut off right above the search line
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
        val t = clock.floatValue          // read in the draw phase: redraws only, no recomposition
        for (spec in JellyCast) drawJelly(spec, t, size.width, size.height)
    }
}

private fun jSmooth(x: Float): Float {
    val v = x.coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}

private fun jHash(i: Int, salt: Int): Float {
    val x = sin(i * 127.1f + salt * 311.7f) * 43758.547f
    return x - floor(x)
}

/** Rotation (yaw, pitch, roll) + perspective, so every point is truly 3D. */
private class JellyCam(
    private val cx: Float,
    private val cy: Float,
    yaw: Float,
    pitch: Float,
    roll: Float,
    private val focal: Float,
) {
    private val cYaw = cos(yaw)
    private val sYaw = sin(yaw)
    private val cPit = cos(pitch)
    private val sPit = sin(pitch)
    private val cRol = cos(roll)
    private val sRol = sin(roll)

    // model space: x right, y DOWN, z away from the viewer
    fun p(x: Float, y: Float, z: Float): Offset {
        val x1 = x * cYaw + z * sYaw
        val z1 = -x * sYaw + z * cYaw
        val y2 = y * cPit - z1 * sPit
        val z2 = y * sPit + z1 * cPit
        val x3 = x1 * cRol - y2 * sRol
        val y3 = x1 * sRol + y2 * cRol
        val s = focal / (focal + z2)
        return Offset(cx + x3 * s, cy + y3 * s)
    }
}

private fun DrawScope.drawJelly(spec: JellySpec, t: Float, w: Float, h: Float) {
    // ── journey: floats up the page, fades in at the bottom, out at the top ──
    val prog = (t * spec.rise + spec.offset) % 1f
    val a = spec.alpha * jSmooth(prog / 0.10f) * jSmooth((1f - prog) / 0.12f)
    if (a < 0.01f) return

    val r = min(w * 0.30f, h * 0.27f) * spec.scale

    // ── pulse: quick contraction, slow relaxation ──
    val period = 2.6f + spec.scale * 0.9f
    val ph = ((t + spec.seed) % period) / period
    val c = if (ph < 0.28f) {
        val x = ph / 0.28f
        1f - (1f - x) * (1f - x)
    } else {
        1f - jSmooth((ph - 0.28f) / 0.72f)
    }

    val sway = sin(t * 0.23f + spec.seed) * w * 0.07f + sin(t * 0.11f + spec.seed * 1.7f) * w * 0.03f
    val cx = w * spec.xFrac + sway * spec.scale.coerceAtLeast(0.6f)
    val cy = h * (1.25f - prog * 1.65f) - r * 0.12f * c   // little thrust on every pulse

    // ── slow 3D tumble: sometimes side-on, sometimes seen from underneath ──
    val yaw = 0.90f * sin(t * 0.30f + spec.seed)
    val pitch = -(0.55f + 0.40f * sin(t * 0.24f + spec.seed * 0.7f + 1f))
    val roll = 0.16f * sin(t * 0.33f + spec.seed + 2f)
    val cam = JellyCam(cx, cy, yaw, pitch, roll, r * 7f)

    val bellR = r * (1f - 0.13f * c)
    val bellH = r * 0.60f * (1f + 0.12f * c)

    // point on the bell surface: u 0 = apex, 1 = rim; the rim is gently scalloped
    fun surf(u: Float, th: Float): Offset {
        val phi = u * JHalfPi
        val lobe = u.pow(10)
        val rr = bellR * sin(phi) * (1f + 0.035f * cos(th * 16f) * lobe)
        val yy = -bellH * cos(phi) + r * 0.03f * cos(th * 16f) * lobe
        return cam.p(rr * cos(th), yy, rr * sin(th))
    }

    val path = Path()
    val unit = r / 260f

    // ── luminous halo behind the whole animal ──
    val bellMid = cam.p(0f, -bellH * 0.4f, 0f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to JellyApex.copy(alpha = 0.16f * a),
            1f to Color.Transparent,
            center = bellMid,
            radius = r * 1.7f,
        ),
        radius = r * 1.7f,
        center = bellMid,
        blendMode = BlendMode.Plus,
    )

    // ── dome: nested translucent rings, so the middle is denser like real jelly ──
    val rings = 14
    val m = 36
    for (i in 0 until rings) {
        val u = 1f - i * (0.88f / (rings - 1))
        path.reset()
        for (j in 0 until m) {
            val o = surf(u, j * JTau / m)
            if (j == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        path.close()
        drawPath(path, lerp(JellyRim, JellyApex, i / (rings - 1f)).copy(alpha = 0.115f * a))
    }

    // ── petals: the blue / pink segments between the ribs ──
    val k = 16
    for (i in 0 until k) {
        val th0 = (i + 0.14f) * JTau / k
        val th1 = (i + 0.86f) * JTau / k
        path.reset()
        val steps = 6
        for (s in 0..steps) {
            val o = surf(0.50f + 0.47f * s / steps, th0)
            if (s == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        for (s in steps downTo 0) {
            val o = surf(0.50f + 0.47f * s / steps, th1)
            path.lineTo(o.x, o.y)
        }
        path.close()
        drawPath(path, (if (i % 2 == 0) JellyPetalBlue else JellyPetalPink).copy(alpha = 0.10f * a))
    }

    // ── ribs (radial canals) ──
    val ribStroke = Stroke(width = (1.3f * unit).coerceAtLeast(0.8f), cap = StrokeCap.Round)
    for (i in 0 until k) {
        val th = i * JTau / k
        path.reset()
        for (s in 0..8) {
            val o = surf(0.16f + 0.84f * s / 8f, th)
            if (s == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, JellyRibCol.copy(alpha = 0.30f * a), style = ribStroke)
    }

    // ── bell edge: bright thin ring + a fainter inner ring ──
    for (pass in 0..1) {
        val u = if (pass == 0) 1f else 0.93f
        path.reset()
        for (j in 0..48) {
            val o = surf(u, j * JTau / 48f)
            if (j == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        path.close()
        drawPath(
            path,
            Color(0xFFD7B5FF).copy(alpha = (if (pass == 0) 0.75f else 0.35f) * a),
            style = Stroke(width = (if (pass == 0) 4f else 2f) * unit.coerceAtLeast(0.3f), join = StrokeJoin.Round),
        )
    }

    // ── dark rim spots ──
    val spots = 44
    for (i in 0 until spots) {
        val th = i * JTau / spots + jHash(i, 1) * 0.08f
        val o = surf(0.985f - jHash(i, 2) * 0.05f, th)
        val sr = r * (0.018f + 0.026f * jHash(i, 3))
        drawCircle(JellySpot.copy(alpha = 0.92f * a), sr, o)
        drawCircle(
            Color(0xFFFF9BE8).copy(alpha = 0.35f * a),
            sr * 0.45f,
            Offset(o.x - sr * 0.25f, o.y - sr * 0.35f),
        )
    }

    // ── glowing centre under the bell ──
    val core = cam.p(0f, -bellH * 0.28f, 0f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFFFB6F2).copy(alpha = 0.55f * a),
            1f to Color.Transparent,
            center = core,
            radius = r * 0.42f,
        ),
        radius = r * 0.42f,
        center = core,
        blendMode = BlendMode.Plus,
    )

    // ── thin trailing tentacles (every 4th is a thick ribbon) ──
    val count = (10 + 6 * spec.scale).toInt()
    val segs = 26
    for (n in 0 until count) {
        val th = n * JTau / count + (jHash(n, 5) - 0.5f) * 0.25f
        val thick = n % 4 == 0
        val len = r * (1.35f + 1.5f * jHash(n, 6)) * (if (thick) 1.25f else 1f)
        val phase = jHash(n, 7) * JTau
        path.reset()
        val midSeg = (segs * 0.45f).toInt()
        val head = Path()
        for (s in 0..segs) {
            val f = s / segs.toFloat()
            val wave = r * 0.11f * f.pow(1.1f) * (1.2f - 0.5f * c)
            val ang = f * 8.5f - t * 1.9f + phase
            val bx = bellR * 0.95f * (1f - 0.55f * f)
            val x = bx * cos(th) + wave * sin(ang) + r * 0.10f * f * f * sin(t * 0.7f + phase)
            val z = bx * sin(th) + wave * cos(ang)
            val y = f * len * (1f + 0.08f * c)
            val o = cam.p(x, y, z)
            if (s == 0) {
                path.moveTo(o.x, o.y)
                head.moveTo(o.x, o.y)
            } else {
                path.lineTo(o.x, o.y)
                if (s <= midSeg) head.lineTo(o.x, o.y)
            }
        }
        val wBase = (if (thick) 2.6f else 1.3f) * unit.coerceAtLeast(0.45f)
        val col = JellyTentacle.copy(alpha = (if (thick) 0.55f else 0.42f) * a)
        drawPath(path, col, style = Stroke(width = wBase, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(head, col, style = Stroke(width = wBase * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // ── four frilly oral arms from the centre ──
    for (n in 0 until 4) {
        val th = n * JTau / 4f + 0.4f
        val armLen = r * 1.5f * (1f + 0.15f * c)
        var prev = Offset.Zero
        for (s in 0..22) {
            val f = s / 22f
            val rad = r * (0.10f + 0.20f * f)
            val x = rad * cos(th) + r * 0.10f * sin(f * 7f - t * 2.2f + th) * f
            val z = rad * sin(th) + r * 0.10f * cos(f * 6f - t * 2.0f + th) * f
            val y = -bellH * 0.28f + f * armLen
            val o = cam.p(x, y, z)
            if (s > 0) {
                drawLine(
                    color = JellyArm.copy(alpha = 0.50f * a * (1f - f * 0.4f)),
                    start = prev,
                    end = o,
                    strokeWidth = (r * 0.045f * (1f - f * 0.8f)).coerceAtLeast(1f),
                    cap = StrokeCap.Round,
                )
            }
            prev = o
        }
    }

    // ── specular glint on the dome (light stays fixed on screen, the jelly turns under it) ──
    val rx = bellR * 0.80f
    val ry = bellH * 0.90f
    drawArc(
        color = Color.White.copy(alpha = 0.28f * a),
        startAngle = 205f,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = Offset(bellMid.x - rx, bellMid.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = r * 0.035f, cap = StrokeCap.Round),
    )
}
