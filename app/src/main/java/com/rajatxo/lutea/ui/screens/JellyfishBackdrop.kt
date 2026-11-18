package com.rajatxo.lutea.ui.screens

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
//  JELLYFISH BACKDROP  v2  —  "deep water"
//
//  • Real physics. Every jellyfish is a little simulation:
//      - the bell is driven by a pulse cycle and follows it through a spring,
//        so it overshoots and settles like real tissue
//      - each pulse is a thrust; water drag + a slow sink turn that into the
//        surge-and-glide you see in real jellyfish
//      - it leans (tilts) with its own motion and turns slowly in 3D
//      - every tentacle and oral arm is a Verlet rope pinned to the bell, so
//        they trail behind, bunch up when the bell squeezes, and sway in the
//        water instead of following a fixed sine
//  • Colour. Each animal has its own iridescent palette (rim → body → apex),
//    far ones fade toward deep blue for depth.
//  • Glow. Everything is added onto the page (BlendMode.Plus), so light
//    accumulates like real bioluminescence.
//  • Marine snow. Glowing particles are drawn by an AGSL shader (Android 13+),
//    layered for parallax, drifting and twinkling, with a soft bokeh layer in
//    front. Older phones get a lighter Canvas fallback.
//
//  Same call as before:  JellyfishBackdrop(searching = ..., modifier = ...)
// ═══════════════════════════════════════════════════════════════════════════

private const val JTau = 6.2831855f
private const val JPi = 3.1415927f
private const val JHalfPi = 1.5707964f

private val JellyDeepBlue = Color(0xFF2A5FA8)

private class JellySpec(
    val scale: Float,      // size relative to the hero
    val depth: Float,      // 1 = near / bright, small = far / faint
    val xFrac: Float,      // where it likes to float horizontally
    val y0Frac: Float,     // where it starts on the very first frame
    val rimHue: Float,     // palette: bell edge
    val bodyHue: Float,    //          bell body
    val apexHue: Float,    //          top of the dome
    val speed: Float,      // how fast it swims up
    val seed: Float,
)

// far → near, so the hero is drawn last
private val JellyCast = listOf(
    JellySpec(0.30f, 0.45f, 0.70f, 0.82f, 195f, 225f, 270f, 0.80f, 3.3f),
    JellySpec(0.38f, 0.55f, 0.13f, 0.34f, 12f, 335f, 290f, 0.90f, 1.4f),   // warm coral one
    JellySpec(0.52f, 0.70f, 0.20f, 0.64f, 330f, 290f, 245f, 1.00f, 5.1f),  // pink → violet
    JellySpec(0.48f, 0.65f, 0.87f, 0.30f, 150f, 190f, 235f, 1.10f, 2.2f),  // aqua → blue
    JellySpec(1.00f, 1.00f, 0.60f, 0.52f, 190f, 225f, 285f, 1.00f, 0.0f),  // hero: cyan → violet
)

// ───────────────────────────── composable ─────────────────────────────

@Composable
fun JellyfishBackdrop(
    searching: Boolean,
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

    // typing → everything melts into a soft blur, the song list takes over
    val blurRadius by animateFloatAsState(
        targetValue = if (searching) 24f else 0f,
        animationSpec = tween(500),
        label = "jellyBlur",
    )
    val dim by animateFloatAsState(
        targetValue = if (searching) 0.42f else 1f,
        animationSpec = tween(500),
        label = "jellyDim",
    )

    val sim = remember { JellySim() }
    val snowShader = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching { RuntimeShader(SnowAgsl) }.getOrNull()
        } else {
            null
        }
    }
    val snowBrush = remember(snowShader) { snowShader?.let { ShaderBrush(it) } }

    Canvas(
        modifier = modifier
            .graphicsLayer {
                alpha = dim
                compositingStrategy = CompositingStrategy.Offscreen
            }
            // soft fade at the bottom so nothing is cut off right above the machine
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
        sim.advance(t, size.width, size.height)

        drawSnow(snowShader, snowBrush, pass = 0f, t = t)          // far marine snow
        for (b in sim.bodies) drawBody(b, t)
        drawSnow(snowShader, snowBrush, pass = 1f, t = t)          // near, soft bokeh
    }
}

// ───────────────────────────── glowing particles (AGSL) ─────────────────────────────

/**
 * Procedural marine snow. Each layer is a jittered grid; one particle lives in each cell,
 * so there is no per-particle data to upload and no hard edges: a particle is a gaussian
 * core plus a wide soft halo, twinkling at its own rate and drifting down / sideways.
 *   pass 0 → three layers of fine snow (far → near)
 *   pass 1 → one layer of big soft bokeh discs, drawn over the jellyfish
 */
private const val SnowAgsl = """
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
    return half4(half3(col * uStrength), 1.0);
}
"""

private fun DrawScope.drawSnow(
    shader: RuntimeShader?,
    brush: ShaderBrush?,
    pass: Float,
    t: Float,
) {
    if (shader != null && brush != null) {
        shader.setFloatUniform("uSize", size.width, size.height)
        shader.setFloatUniform("uTime", t)
        shader.setFloatUniform("uPass", pass)
        shader.setFloatUniform("uStrength", 1f)
        drawRect(brush = brush, blendMode = BlendMode.Plus)
        return
    }
    if (pass > 0.5f) return

    // fallback for Android 12 and older: soft dots on a plain canvas
    val w = size.width
    val h = size.height
    for (i in 0 until 90) {
        val hx = jHash(i, 1)
        val hy = jHash(i, 2)
        val hs = jHash(i, 3)
        val x = (hx * w + sin(t * 0.3f + hs * 9f) * 12f + w) % w
        val y = (hy * h + t * (5f + hs * 9f)) % h
        val tw = 0.55f + 0.45f * sin(t * (0.8f + hs * 1.5f) + i)
        val rad = (0.9f + hs * 1.8f) * density
        drawCircle(Color(0xFFBFE6FF).copy(alpha = 0.07f * tw), rad * 4f, Offset(x, y), blendMode = BlendMode.Plus)
        drawCircle(Color(0xFFDDF3FF).copy(alpha = 0.60f * tw), rad, Offset(x, y), blendMode = BlendMode.Plus)
    }
}

// ───────────────────────────── physics ─────────────────────────────

private fun jSmooth(x: Float): Float {
    val v = x.coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}

private fun jHash(i: Int, salt: Int): Float {
    val x = sin(i * 127.1f + salt * 311.7f) * 43758.547f
    return x - floor(x)
}

/** Rotation (yaw, pitch, roll) of the animal; the result of apply() is left in ox/oy/oz. */
private class JRot {
    private var cYaw = 1f
    private var sYaw = 0f
    private var cPit = 1f
    private var sPit = 0f
    private var cRol = 1f
    private var sRol = 0f
    var ox = 0f
    var oy = 0f
    var oz = 0f

    fun set(yaw: Float, pitch: Float, roll: Float) {
        cYaw = cos(yaw); sYaw = sin(yaw)
        cPit = cos(pitch); sPit = sin(pitch)
        cRol = cos(roll); sRol = sin(roll)
    }

    // model space: x right, y DOWN, z away from the viewer
    fun apply(x: Float, y: Float, z: Float) {
        val x1 = x * cYaw + z * sYaw
        val z1 = -x * sYaw + z * cYaw
        val y2 = y * cPit - z1 * sPit
        val z2 = y * sPit + z1 * cPit
        ox = x1 * cRol - y2 * sRol
        oy = x1 * sRol + y2 * cRol
        oz = z2
    }
}

/** One tentacle / oral arm: a Verlet rope. kind 0 = fine, 1 = thick ribbon, 2 = oral arm. */
private class JChain(
    val n: Int,
    val angle: Float,
    val len: Float,       // in units of the bell radius
    val kind: Int,
    val phase: Float,
) {
    val px = FloatArray(n)
    val py = FloatArray(n)
    val pz = FloatArray(n)
    val qx = FloatArray(n)   // previous positions
    val qy = FloatArray(n)
    val qz = FloatArray(n)
}

private class JellyBody(val spec: JellySpec) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var tilt = 0f
    var tiltV = 0f
    var yaw = 0f
    var yawV = 0f
    var pitch = -0.6f
    var phase = 0f
    var bell = 0f      // 0 relaxed … 1 squeezed (spring-driven, so it overshoots)
    var bellV = 0f
    var rim = 0f       // the bell edge lags a little behind the dome
    var rimV = 0f
    var age = 0f
    var r = 100f
    private var prevDt = 0.016f

    val rot = JRot()
    private val rnd = Random((spec.seed * 1000f).toInt() + 17)
    private val period = 2.5f + spec.scale * 1.0f + rnd.nextFloat() * 0.4f
    val chains: List<JChain>

    // scratch (anchor + rest direction of the chain being processed)
    private var ax = 0f
    private var ay = 0f
    private var az = 0f
    private var dx = 0f
    private var dy = 0f
    private var dz = 0f

    init {
        val list = ArrayList<JChain>()
        val count = (10 + 8 * spec.scale).toInt()
        val nodes = 13 + (spec.scale * 4f).toInt()
        for (k in 0 until count) {
            val thick = k % 4 == 0
            list.add(
                JChain(
                    n = if (thick) nodes + 3 else nodes,
                    angle = k * JTau / count + (rnd.nextFloat() - 0.5f) * 0.20f,
                    len = (1.5f + 1.3f * rnd.nextFloat()) * (if (thick) 1.20f else 1f),
                    kind = if (thick) 1 else 0,
                    phase = rnd.nextFloat() * JTau,
                ),
            )
        }
        for (k in 0 until 4) {
            list.add(JChain(11, k * JTau / 4f + 0.4f, 1.35f, 2, rnd.nextFloat() * JTau))
        }
        chains = list
    }

    fun spawn(w: Float, h: Float, initial: Boolean) {
        r = min(w * 0.16f, h * 0.17f) * spec.scale
        x = w * (spec.xFrac + if (initial) 0f else (rnd.nextFloat() - 0.5f) * 0.10f)
        y = if (initial) h * spec.y0Frac else h + r * 1.1f
        vx = 0f
        vy = if (initial) -h * 0.04f * spec.speed else 0f
        phase = rnd.nextFloat()
        bell = 0f; bellV = 0f; rim = 0f; rimV = 0f
        tilt = 0f; tiltV = 0f
        yaw = (rnd.nextFloat() - 0.5f) * 1.0f; yawV = 0f
        age = if (initial) 3f else 0f
        rot.set(yaw, pitch, tilt)
        for (ch in chains) {
            place(ch)
            val seg = ch.len * r / (ch.n - 1)
            for (i in 0 until ch.n) {
                ch.px[i] = ax + dx * seg * i
                ch.py[i] = ay + dy * seg * i
                ch.pz[i] = az + dz * seg * i
                ch.qx[i] = ch.px[i]; ch.qy[i] = ch.py[i]; ch.qz[i] = ch.pz[i]
            }
        }
    }

    /** Anchor point (on the bell) and rest direction of a chain, in world space. */
    private fun place(ch: JChain) {
        val bellR = r * (1f - 0.16f * bell)
        val bellH = r * 0.5f * (1f + 0.14f * bell)
        val ca = cos(ch.angle)
        val sa = sin(ch.angle)
        if (ch.kind == 2) {
            rot.apply(r * 0.12f * ca, -bellH * 0.30f, r * 0.12f * sa)
        } else {
            rot.apply(bellR * ca, r * 0.02f, bellR * sa)
        }
        ax = x + rot.ox; ay = y + rot.oy; az = rot.oz
        val flare = if (ch.kind == 2) 0.25f else 0.20f
        rot.apply(ca * flare, 1f, sa * flare)
        val l = sqrt(rot.ox * rot.ox + rot.oy * rot.oy + rot.oz * rot.oz).coerceAtLeast(0.0001f)
        dx = rot.ox / l; dy = rot.oy / l; dz = rot.oz / l
    }

    fun step(dt: Float, time: Float, w: Float, h: Float) {
        r = min(w * 0.16f, h * 0.17f) * spec.scale
        age += dt
        val ratio = dt / prevDt
        prevDt = dt

        // ── pulse: quick squeeze, slow relax ──
        phase += dt / period
        if (phase >= 1f) {
            phase -= 1f
            tiltV += (rnd.nextFloat() - 0.5f) * 0.5f          // every stroke is a little uneven
            vx += (rnd.nextFloat() - 0.5f) * r * 0.25f
        }
        val win = 0.22f
        val target = if (phase < win) jSmooth(phase / win) else 1f - jSmooth((phase - win) / (1f - win))
        bellV += (150f * (target - bell) - 11f * bellV) * dt   // under-damped spring → overshoot
        bell += bellV * dt
        rimV += (70f * (bell - rim) - 8f * rimV) * dt           // the edge trails the dome
        rim += rimV * dt

        // ── thrust, drag and sink: surge and glide ──
        val kd = 1.9f
        val vr = h * 0.042f * spec.speed          // average climb speed we want (px/s)
        val vSink = r * 0.05f
        val amp = (vr + vSink) * kd / 0.14f       // 0.14 = ∫ sin-pulse over one cycle
        if (phase < win) {
            val a = amp * sin(phase / win * JPi)
            vx += sin(tilt) * a * dt                // thrust follows the way the bell leans
            vy -= cos(tilt) * a * dt
        }
        val current = sin(time * 0.17f + spec.seed * 3f) * r * 0.10f +
            sin(time * 0.071f + spec.seed) * r * 0.06f
        vx += (current - vx) * kd * dt
        vy += (vSink - vy) * kd * dt
        x += vx * dt
        y += vy * dt

        // ── attitude: leans with its own motion, turns slowly in 3D ──
        val tTarget = 0.30f * sin(time * 0.19f + spec.seed * 2f) +
            (vx / (r * 1.2f)).coerceIn(-0.3f, 0.3f) * 0.5f
        tiltV += (-4f * (tilt - tTarget) - 2.4f * tiltV) * dt
        tilt += tiltV * dt
        val yTarget = 0.9f * sin(time * 0.26f + spec.seed * 1.3f)
        yawV += (-2.2f * (yaw - yTarget) - 2.6f * yawV) * dt
        yaw += yawV * dt
        pitch = -(0.55f + 0.30f * sin(time * 0.20f + spec.seed * 0.7f + 1f))
        rot.set(yaw, pitch, tilt)

        if (y < -r * 3.4f) {
            spawn(w, h, false)
            return
        }

        // ── tentacles and arms: Verlet ropes pinned to the bell ──
        val damp = exp(-2.6f * dt)
        val k60 = dt * 60f
        for (ch in chains) {
            place(ch)
            val n = ch.n
            val seg = ch.len * r / (n - 1)
            ch.px[0] = ax; ch.py[0] = ay; ch.pz[0] = az
            ch.qx[0] = ax; ch.qy[0] = ay; ch.qz[0] = az
            val waveAmp = r * 1.5f * (if (ch.kind == 2) 0.7f else 1f)
            for (i in 1 until n) {
                val f = i / (n - 1f)
                val vxn = (ch.px[i] - ch.qx[i]) * ratio * damp
                val vyn = (ch.py[i] - ch.qy[i]) * ratio * damp
                val vzn = (ch.pz[i] - ch.qz[i]) * ratio * damp
                ch.qx[i] = ch.px[i]; ch.qy[i] = ch.py[i]; ch.qz[i] = ch.pz[i]

                val wa = waveAmp * f.pow(1.2f)
                val accX = sin(i * 0.7f - time * 2.3f + ch.phase) * wa
                val accZ = cos(i * 0.7f - time * 2.0f + ch.phase * 1.3f) * wa
                val g = r * 0.30f                      // slightly heavier than water

                ch.px[i] += vxn + accX * dt * dt
                ch.py[i] += vyn + g * dt * dt
                ch.pz[i] += vzn + accZ * dt * dt

                // gentle pull toward the rest line: strong at the root, free at the tip,
                // stronger while the bell is squeezed (the tentacles stream out behind it)
                val ks = 0.16f * (1f - f).pow(2.2f) * (1f + 1.3f * bell) * k60
                ch.px[i] += (ax + dx * seg * i - ch.px[i]) * ks
                ch.py[i] += (ay + dy * seg * i - ch.py[i]) * ks
                ch.pz[i] += (az + dz * seg * i - ch.pz[i]) * ks
            }
            repeat(3) {
                for (i in 1 until n) {
                    val ex = ch.px[i] - ch.px[i - 1]
                    val ey = ch.py[i] - ch.py[i - 1]
                    val ez = ch.pz[i] - ch.pz[i - 1]
                    val dist = sqrt(ex * ex + ey * ey + ez * ez).coerceAtLeast(0.0001f)
                    val diff = (dist - seg) / dist
                    if (i == 1) {
                        ch.px[i] -= ex * diff; ch.py[i] -= ey * diff; ch.pz[i] -= ez * diff
                    } else {
                        val hx = ex * diff * 0.5f
                        val hy = ey * diff * 0.5f
                        val hz = ez * diff * 0.5f
                        ch.px[i] -= hx; ch.py[i] -= hy; ch.pz[i] -= hz
                        ch.px[i - 1] += hx; ch.py[i - 1] += hy; ch.pz[i - 1] += hz
                    }
                }
            }
        }
    }
}

private class JellySim {
    val bodies = JellyCast.map { JellyBody(it) }
    private var lastT = -1f
    private var simTime = 0f
    private var ready = false

    /** Safe to call any number of times per frame: it only moves forward when the clock did. */
    fun advance(t: Float, w: Float, h: Float) {
        if (w < 8f || h < 8f) return
        if (!ready) {
            for (b in bodies) b.spawn(w, h, true)
            ready = true
            lastT = t
            return
        }
        var dt = t - lastT
        if (dt <= 0f) return
        lastT = t
        dt = dt.coerceAtMost(0.05f)
        val n = ceil(dt / 0.0125f).toInt().coerceAtLeast(1)    // small sub-steps = stable and smooth
        val s = dt / n
        repeat(n) {
            for (b in bodies) b.step(s, simTime, w, h)
            simTime += s
        }
    }
}

// ───────────────────────────── drawing ─────────────────────────────

private fun DrawScope.drawBody(b: JellyBody, t: Float) {
    val spec = b.spec
    val r = b.r
    if (r < 4f) return

    val a = (0.30f + 0.70f * spec.depth) *
        jSmooth(b.age / 2.5f) *
        jSmooth((b.y + r * 0.8f) / (size.height * 0.22f))
    if (a < 0.01f) return

    // palette, pushed toward deep blue with distance
    val fog = (1f - spec.depth) * 0.55f
    val rimC = lerp(Color.hsv(spec.rimHue, 0.45f, 1f), JellyDeepBlue, fog)
    val bodyC = lerp(Color.hsv(spec.bodyHue, 0.70f, 0.95f), JellyDeepBlue, fog)
    val apexC = lerp(Color.hsv(spec.apexHue, 0.80f, 0.95f), JellyDeepBlue, fog)
    val tentC = lerp(rimC, bodyC, 0.45f)
    val armC = lerp(bodyC, apexC, 0.5f)

    val focal = r * 7f
    val bellR = r * (1f - 0.16f * b.bell)
    val bellH = r * 0.5f * (1f + 0.14f * b.bell)
    val rot = b.rot

    fun proj(lx: Float, ly: Float, lz: Float): Offset {
        rot.apply(lx, ly, lz)
        val s = focal / (focal + rot.oz)
        return Offset(b.x + rot.ox * s, b.y + rot.oy * s)
    }

    // u: 0 = apex … 1 = rim.  The rim is scalloped and flares a little when the bell lags.
    fun surf(u: Float, th: Float): Offset {
        val phi = u * JHalfPi
        val lobe = u.pow(10)
        val flare = 1f + 0.10f * (b.bell - b.rim) * u.pow(3)
        val rr = bellR * sin(phi) * flare * (1f + 0.03f * cos(th * 16f) * lobe)
        val yy = -bellH * cos(phi) + r * 0.03f * cos(th * 16f) * lobe
        return proj(rr * cos(th), yy, rr * sin(th))
    }

    val path = Path()
    val plus = BlendMode.Plus

    // ── halo ──
    val mid = proj(0f, -bellH * 0.4f, 0f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to bodyC.copy(alpha = 0.16f * a),
            1f to Color.Transparent,
            center = mid,
            radius = r * 1.9f,
        ),
        radius = r * 1.9f,
        center = mid,
        blendMode = plus,
    )

    // ── bell body: nested translucent rings, denser toward the middle, light adds up ──
    val rings = 20
    val m = 40
    for (i in 0 until rings) {
        val u = 1f - i * (0.90f / (rings - 1))
        path.reset()
        for (j in 0 until m) {
            val o = surf(u, j * JTau / m)
            if (j == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        path.close()
        val k = i / (rings - 1f)
        val col = if (k < 0.5f) lerp(rimC, bodyC, k * 2f) else lerp(bodyC, apexC, (k - 0.5f) * 2f)
        drawPath(path, col.copy(alpha = 0.052f * a), blendMode = plus)
    }

    // ── radial canals, each with its own shimmer of colour ──
    val ribs = 24
    val ribW = (r * 0.008f).coerceAtLeast(0.8f)
    for (i in 0 until ribs) {
        val th = i * JTau / ribs
        path.reset()
        for (s in 0..8) {
            val o = surf(0.12f + 0.88f * s / 8f, th)
            if (s == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        val shimmer = 0.5f + 0.5f * sin(th * 2f + t * 0.6f + spec.seed)
        drawPath(
            path,
            lerp(rimC, apexC, shimmer).copy(alpha = 0.20f * a),
            style = Stroke(width = ribW, cap = StrokeCap.Round),
            blendMode = plus,
        )
    }

    // ── bell edge: soft glow + thin bright line ──
    path.reset()
    for (j in 0..48) {
        val o = surf(1f, j * JTau / 48f)
        if (j == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
    }
    path.close()
    drawPath(path, rimC.copy(alpha = 0.10f * a), style = Stroke(width = r * 0.09f, join = StrokeJoin.Round), blendMode = plus)
    drawPath(path, rimC.copy(alpha = 0.30f * a), style = Stroke(width = r * 0.03f, join = StrokeJoin.Round), blendMode = plus)
    drawPath(
        path,
        lerp(rimC, Color.White, 0.55f).copy(alpha = 0.80f * a),
        style = Stroke(width = (r * 0.011f).coerceAtLeast(1f), join = StrokeJoin.Round),
        blendMode = plus,
    )

    // ── the four horseshoe rings you can see under the dome ──
    val gonadW = (r * 0.02f).coerceAtLeast(1.2f)
    for (g in 0 until 4) {
        val ang = g * JTau / 4f + 0.785f
        val gx = bellR * 0.40f * cos(ang)
        val gz = bellR * 0.40f * sin(ang)
        val rho = bellR * 0.19f
        path.reset()
        for (j in 0..20) {
            val tt = ang + 0.35f + (JTau - 0.7f) * j / 20f
            val o = proj(gx + rho * cos(tt), -bellH * 0.34f, gz + rho * sin(tt))
            if (j == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(
            path,
            lerp(rimC, Color.White, 0.5f).copy(alpha = 0.50f * a),
            style = Stroke(width = gonadW, cap = StrokeCap.Round, join = StrokeJoin.Round),
            blendMode = plus,
        )
    }

    // ── glowing centre ──
    val core = proj(0f, -bellH * 0.28f, 0f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to lerp(bodyC, Color.White, 0.35f).copy(alpha = 0.45f * a),
            1f to Color.Transparent,
            center = core,
            radius = r * 0.5f,
        ),
        radius = r * 0.5f,
        center = core,
        blendMode = plus,
    )

    // ── tentacles and oral arms (the simulated ropes) ──
    for (ch in b.chains) {
        val n = ch.n
        val pts = Array(n) { i ->
            val s = focal / (focal + ch.pz[i])
            Offset(b.x + (ch.px[i] - b.x) * s, b.y + (ch.py[i] - b.y) * s)
        }
        val base = when (ch.kind) {
            1 -> r * 0.022f
            2 -> r * 0.050f
            else -> r * 0.008f
        }.coerceAtLeast(0.8f)
        val col = if (ch.kind == 2) armC else tentC
        val coreA = when (ch.kind) {
            1 -> 0.55f
            2 -> 0.42f
            else -> 0.42f
        }

        fun smooth(upTo: Int, into: Path) {
            into.reset()
            into.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until upTo) {
                val mx = (pts[i].x + pts[i + 1].x) * 0.5f
                val my = (pts[i].y + pts[i + 1].y) * 0.5f
                into.quadraticBezierTo(pts[i].x, pts[i].y, mx, my)
            }
            into.lineTo(pts[upTo].x, pts[upTo].y)
        }

        smooth(n - 1, path)
        drawPath(path, col.copy(alpha = 0.05f * a), style = Stroke(width = base * 5f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = plus)
        drawPath(path, col.copy(alpha = coreA * a), style = Stroke(width = base, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = plus)
        smooth((n * 0.45f).toInt().coerceIn(2, n - 1), path)   // thicker near the root = tapering
        drawPath(path, col.copy(alpha = 0.22f * a), style = Stroke(width = base * 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = plus)

        if (ch.kind == 2) {
            // frilly arms: a string of glowing beads along the rope
            for (i in 1 until n) {
                val f = i / (n - 1f)
                drawCircle(
                    col.copy(alpha = 0.28f * a * (1f - f * 0.6f)),
                    base * 0.55f * (1f - f * 0.5f) + 0.6f,
                    pts[i],
                    blendMode = plus,
                )
            }
        } else {
            // bioluminescent bead where the tentacle meets the rim
            drawCircle(Color.White.copy(alpha = 0.45f * a), (r * 0.012f).coerceAtLeast(1f), pts[0], blendMode = plus)
        }
    }

    // ── specular glint: the light stays fixed on screen while the animal turns under it ──
    val rx = bellR * 0.80f
    val ry = bellH * 0.90f
    drawArc(
        color = Color.White.copy(alpha = 0.26f * a),
        startAngle = 205f,
        sweepAngle = 42f,
        useCenter = false,
        topLeft = Offset(mid.x - rx, mid.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = r * 0.03f, cap = StrokeCap.Round),
        blendMode = plus,
    )
    val sheen = Offset(mid.x - bellR * 0.30f, mid.y - bellH * 0.45f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.16f * a),
            1f to Color.Transparent,
            center = sheen,
            radius = r * 0.45f,
        ),
        radius = r * 0.45f,
        center = sheen,
        blendMode = plus,
    )
}
