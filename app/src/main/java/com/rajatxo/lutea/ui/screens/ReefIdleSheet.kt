package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.playback.PlaybackConnection
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
//  REEF — idle page
//
//  What fills the space above the typewriter while nothing is typed:
//    • a faint line-art reef that slowly draws itself on the dark page
//    • a paper sheet that feeds up out of the machine; the typewriter types
//      the library summary + "pick of the day" onto it, line by line
//    • ink stamps (two artists of the day + shuffle) that thud onto the sheet
//
//  The page background is untouched. When the user starts typing, all of this
//  fades away and the song list takes the space, exactly like before.
// ═══════════════════════════════════════════════════════════════════════════

private val ReefCream = Color(0xFFFCF2C7)     // same butter cream as the carriage
private val ReefAccent = Color(0xFFE8735A)    // the stamp ink: reef
private val SheetTop = Color(0xFF2E2A26)
private val SheetBottom = Color(0xFF211E1B)

private enum class IdleKind { HEAD, SONG }
private data class IdleLine(val text: String, val kind: IdleKind)
private data class IdleStamp(val label: String, val artist: String?)   // artist == null → shuffle

@Composable
fun ReefIdleSheet(
    searching: Boolean,
    library: List<Song>,
    loaded: Boolean,
    onPickQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val idleAlpha by animateFloatAsState(
        targetValue = if (searching) 0f else 1f,
        animationSpec = tween(350),
        label = "reefIdle",
    )
    // the sheet only types itself once; coming back from a search it is simply there
    var typedOnce by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.graphicsLayer {
            alpha = idleAlpha
            translationY = (1f - idleAlpha) * 24.dp.toPx()
        },
    ) {
        if (idleAlpha > 0.01f) {
            ReefArt(modifier = Modifier.fillMaxSize())
            IdleSheet(
                library = library,
                loaded = loaded,
                skipTyping = typedOnce,
                onDone = { typedOnce = true },
                onPickQuery = onPickQuery,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp),
            )
        }
    }
}

// ───────────────────────────── the sheet ─────────────────────────────

@Composable
private fun IdleSheet(
    library: List<Song>,
    loaded: Boolean,
    skipTyping: Boolean,
    onDone: () -> Unit,
    onPickQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val day = System.currentTimeMillis() / 86_400_000L
    val artists = remember(library) {
        library.map { it.artist }.filter { it.isNotBlank() }.distinct()
    }
    val pick = remember(library) {
        if (library.isEmpty()) null else library[(day % library.size).toInt()]
    }
    val stampArtists = remember(library) {
        artists.filter { !it.contains("unknown", ignoreCase = true) }
            .shuffled(Random(day))
            .take(2)
    }

    val lines = buildList {
        if (loaded && library.isNotEmpty()) {
            add(IdleLine("> ${library.size} songs · ${artists.size} artists", IdleKind.HEAD))
            if (pick != null) {
                add(IdleLine("> pick of the day", IdleKind.HEAD))
                add(IdleLine("♪ ${pick.title} · ${pick.artist}", IdleKind.SONG))
            }
            add(IdleLine("> or try these", IdleKind.HEAD))
        } else if (loaded) {
            add(IdleLine("> no songs found on this device", IdleKind.HEAD))
        }
    }
    val total = lines.sumOf { it.text.length }
    val typed = remember(lines) { mutableIntStateOf(if (skipTyping) total else 0) }
    val typedNow = typed.intValue
    val done = lines.isNotEmpty() && typedNow >= total

    LaunchedEffect(lines) {
        if (lines.isEmpty() || skipTyping) return@LaunchedEffect
        delay(650)                                   // the paper has to feed in first
        while (typed.intValue < total) {
            typed.intValue = typed.intValue + 1
            delay(34)
        }
        onDone()
    }

    // paper feeds up out of the machine
    val feed = remember { Animatable(if (skipTyping) 1f else 0f) }
    LaunchedEffect(Unit) {
        feed.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    // stamps thud down one after another once the typing is finished
    val stamps = remember(library) {
        if (library.isEmpty()) emptyList()
        else stampArtists.map { IdleStamp(it, it) } + IdleStamp("shuffle", null)
    }
    var shownStamps by remember { mutableIntStateOf(if (skipTyping) 99 else 0) }
    LaunchedEffect(done) {
        if (done) {
            while (shownStamps < stamps.size) {
                delay(190)
                shownStamps += 1
            }
        }
    }

    val blink by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "caretBlink",
    )

    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .fillMaxWidth(0.88f)
            .graphicsLayer {
                rotationZ = -1.1f
                translationY = (1f - feed.value) * 90.dp.toPx()
                alpha = feed.value
            }
            .clip(shape)
            .background(Brush.verticalGradient(0f to SheetTop, 1f to SheetBottom))
            .border(0.8.dp, ReefCream.copy(alpha = 0.10f), shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        // letterhead
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "LUTEA",
                color = ReefCream,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = "your library",
                color = ReefCream.copy(alpha = 0.35f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ReefAccent.copy(alpha = 0.55f)),
        )
        Spacer(Modifier.height(4.dp))

        // which line is the typebar on right now
        var rem = typedNow
        var caretIdx = lines.lastIndex
        for ((i, l) in lines.withIndex()) {
            if (rem < l.text.length) {
                caretIdx = i
                break
            }
            rem -= l.text.length
        }

        var left = typedNow
        for ((i, line) in lines.withIndex()) {
            val shown = min(line.text.length, left)
            left -= shown
            val isSong = line.kind == IdleKind.SONG
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .drawBehind {
                        // ruled paper
                        drawLine(
                            color = ReefCream.copy(alpha = 0.06f),
                            start = Offset(0f, size.height - 0.5f),
                            end = Offset(size.width, size.height - 0.5f),
                            strokeWidth = 1f,
                        )
                    }
                    .then(
                        if (isSong && pick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { PlaybackConnection.playSong(pick, library) }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Text(
                    text = line.text.take(shown),
                    color = if (isSong) ReefCream else ReefCream.copy(alpha = 0.62f),
                    fontSize = if (isSong) 14.sp else 12.sp,
                    fontWeight = if (isSong) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (i == caretIdx) {
                    Text(
                        text = "▌",
                        color = ReefAccent,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.graphicsLayer { alpha = if (done) blink else 1f },
                    )
                }
            }
        }

        if (stamps.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val tilts = floatArrayOf(-3f, 2f, -1.5f)
                for ((i, stamp) in stamps.withIndex()) {
                    StampChip(
                        label = stamp.label,
                        tilt = tilts[i % tilts.size],
                        visible = i < shownStamps,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (stamp.artist != null) {
                                onPickQuery(stamp.artist)
                            } else {
                                val shuffled = library.shuffled()
                                shuffled.firstOrNull()?.let { PlaybackConnection.playSong(it, shuffled) }
                            }
                        },
                    )
                }
            }
        }
    }
}

/** An ink stamp: drops in slightly too big, thuds down with a little bounce. */
@Composable
private fun StampChip(
    label: String,
    tilt: Float,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
        label = "stamp",
    )
    val shape = RoundedCornerShape(6.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                val s = 1.55f - 0.55f * t
                scaleX = s
                scaleY = s
                alpha = t.coerceIn(0f, 1f)
                rotationZ = tilt
            }
            .clip(shape)
            .background(ReefAccent.copy(alpha = 0.08f))
            .border(1.4.dp, ReefAccent.copy(alpha = 0.85f), shape)
            .clickable(enabled = visible, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
    ) {
        Text(
            text = if (label == "shuffle") "▶ SHUFFLE" else label.uppercase(),
            color = ReefAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ───────────────────────────── the reef ─────────────────────────────

private class ReefSeg(
    val xs: FloatArray,      // offsets from the reef's root, in units of the canvas height
    val ys: FloatArray,
    val start: Float,        // when this branch starts growing (0..1 of the whole drawing)
    val end: Float,
    val depth: Int,
)

private val ReefBranches: List<ReefSeg> by lazy {
    val rnd = Random(7)
    val out = ArrayList<ReefSeg>()
    fun grow(x: Float, y: Float, angle: Float, len: Float, depth: Int, t0: Float) {
        val pts = 7
        val xs = FloatArray(pts)
        val ys = FloatArray(pts)
        var cx = x
        var cy = y
        var ang = angle
        val bend = (rnd.nextFloat() - 0.5f) * 0.12f
        for (i in 0 until pts) {
            xs[i] = cx
            ys[i] = cy
            ang += bend
            cx += sin(ang) * len / (pts - 1)
            cy -= cos(ang) * len / (pts - 1)
        }
        val dur = 0.20f
        out.add(ReefSeg(xs, ys, t0, t0 + dur, depth))
        if (depth < 4) {
            val kids = if (depth == 0) 3 else if (rnd.nextFloat() < 0.35f) 3 else 2
            for (k in 0 until kids) {
                val a = ang + (k - (kids - 1) / 2f) * 0.58f + (rnd.nextFloat() - 0.5f) * 0.22f
                grow(xs[pts - 1], ys[pts - 1], a, len * 0.68f, depth + 1, t0 + dur * 0.75f)
            }
        }
    }
    grow(0f, 0f, 0f, 0.30f, 0, 0f)
    out
}

/** Faint line-art reef that draws itself once, three of them growing from the sheet's edge. */
@Composable
private fun ReefArt(modifier: Modifier = Modifier) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(300)
        grow.animateTo(1f, tween(7000, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = modifier) {
        val p = grow.value * 0.78f      // the last branches finish at ~0.78 of the timeline
        // root x (fraction of width), size, mirrored?, lean
        drawReef(p, size.width * 0.50f, 1.00f, false)
        drawReef(p, size.width * 0.14f, 0.72f, true)
        drawReef(p, size.width * 0.88f, 0.80f, false)
    }
}

private fun DrawScope.drawReef(progress: Float, rootX: Float, scale: Float, mirror: Boolean) {
    val unit = size.height * scale
    val dir = if (mirror) -1f else 1f
    val path = Path()
    for (seg in ReefBranches) {
        val local = ((progress - seg.start) / (seg.end - seg.start)).coerceIn(0f, 1f)
        if (local <= 0f) continue
        val n = seg.xs.size
        val f = local * (n - 1)
        val whole = f.toInt().coerceAtMost(n - 1)
        val frac = f - whole
        path.reset()
        path.moveTo(rootX + seg.xs[0] * unit * dir, size.height + seg.ys[0] * unit)
        for (i in 1..whole) {
            path.lineTo(rootX + seg.xs[i] * unit * dir, size.height + seg.ys[i] * unit)
        }
        if (whole < n - 1) {
            val nx = seg.xs[whole] + (seg.xs[whole + 1] - seg.xs[whole]) * frac
            val ny = seg.ys[whole] + (seg.ys[whole + 1] - seg.ys[whole]) * frac
            path.lineTo(rootX + nx * unit * dir, size.height + ny * unit)
        }
        val width = (2.4f - seg.depth * 0.45f).coerceAtLeast(0.8f) * density
        drawPath(
            path = path,
            color = ReefCream.copy(alpha = 0.085f + seg.depth * 0.012f),
            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // a little polyp on each finished tip
        if (local >= 1f && seg.depth == 4) {
            drawCircle(
                color = ReefAccent.copy(alpha = 0.22f),
                radius = 1.8f * density,
                center = Offset(
                    rootX + seg.xs[n - 1] * unit * dir,
                    size.height + seg.ys[n - 1] * unit,
                ),
            )
        }
    }
}
