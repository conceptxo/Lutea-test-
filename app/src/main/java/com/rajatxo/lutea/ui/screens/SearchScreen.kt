package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
//  SEARCH PAGE — gradient, a search capsule, and the typewriter under it.
//
//  Top to bottom:  the search capsule (you type here) → the T hammer → the
//  cream carriage on its steel rail. The carriage follows the caret; on every
//  keystroke the hammer taps up at the capsule and the first LED flashes amber.
//  Everything is drawn with gradients — no images.
//
//  The ensemble hangs in the top half of the page, its rail sitting on the
//  horizontal centre line. The keyboard starts below the middle of the screen,
//  so it can never cover it — no IME padding, no movement, ever.
// ═══════════════════════════════════════════════════════════════════════════

private val TypeCream = Color(0xFFFCF2C7)   // butter cream — text + capsule

/** The playlist page's charcoal → black backdrop, reused here. */
private val PageTop = Color(0xFF3A3A3A)
private val PageBottom = Color.Black

private val CapsuleWidth = 80.dp
private val CapsuleHeight = 34.dp
private val RailAreaHeight = 60.dp          // rail at 36dp, hammer above it, air under the pipe
private val RailCenterY = 36.dp             // centre of the rail inside the rail area
private val TextStart = 66.dp               // margin + pill padding + glyph + gap: where typed text begins

// The search capsule that floats above the mechanism.
private val CapsuleBarHeight = 46.dp
private val CapsuleSideMargin = 20.dp
private val CapsuleInnerPadding = 18.dp

/** Lucide search glyph (MIT), inlined so the page owns its icon. */
private val SearchGlyph: ImageVector = ImageVector.Builder(
    name = "Search",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        strokeLineMiter = 4f,
    ) {
        moveTo(19f, 11f)
        arcTo(8f, 8f, 0f, true, true, 3f, 11f)
        arcTo(8f, 8f, 0f, true, true, 19f, 11f)
        moveTo(21f, 21f)
        lineTo(16.66f, 16.66f)
    }
}.build()

@Composable
fun SearchScreen() {
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageTop, PageBottom))),
    ) {
        // The machine is parked in the top half of the page with its rail sitting
        // on the horizontal centre line. The keyboard starts below the middle on
        // every phone, so it can never reach it — no insets, no movement.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
        ) {
            SearchMachine(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun SearchMachine(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    // Everything on the rail is designed for a 360dp-wide phone and scales with the screen width.
    val k = (LocalConfiguration.current.screenWidthDp / 360f).coerceIn(0.85f, 1.35f)

    var caretPx by remember { mutableStateOf(0f) }      // caret x inside the text box
    var viewportPx by remember { mutableStateOf(0f) }   // visible width of the text box
    var railPx by remember { mutableStateOf(0f) }       // rail width

    // Carriage target: centre the capsule over the caret, keep it between the end collars.
    val capsuleWPx = with(density) { CapsuleWidth.toPx() } * k
    val textStartPx = with(density) { TextStart.toPx() }
    val edgePx = with(density) { 26.dp.toPx() } * k
    val maxLeft = (railPx - capsuleWPx - edgePx).coerceAtLeast(edgePx)
    val targetLeft = (textStartPx + minOf(caretPx, viewportPx) - capsuleWPx / 2f)
        .coerceIn(edgePx, maxLeft)
    val carriageX by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "carriage",
    )

    // One strike per keystroke: hammer taps up onto the capsule, then springs back; LED flashes.
    val strike = remember { Animatable(0f) }
    LaunchedEffect(query) {
        if (query.isNotEmpty()) {
            strike.animateTo(1f, tween(40, easing = LinearOutSlowInEasing))
            strike.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 900f))
        }
    }

    Column(modifier = modifier) {
        // ── the search capsule: a lit pill floating above the mechanism ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CapsuleSideMargin)
                .height(CapsuleBarHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, TypeCream.copy(alpha = 0.30f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                .padding(horizontal = CapsuleInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SearchGlyph,
                contentDescription = null,
                tint = TypeCream.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .onSizeChanged { viewportPx = it.width.toFloat() },
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search songs, artists, albums",
                        color = TypeCream.copy(alpha = 0.35f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TypeCream,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.3.sp,
                    ),
                    cursorBrush = SolidColor(TypeCream),
                    onTextLayout = { layout ->
                        val end = layout.layoutInput.text.length
                        caretPx = layout.getCursorRect(query.length.coerceIn(0, end)).left
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✕",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── rail + carriage + hammer: edge to edge, scaled to the screen width ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RailAreaHeight * k)
                .onSizeChanged { railPx = it.width.toFloat() }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focus.requestFocus()
                    keyboard?.show()
                },
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * k, density.fontScale),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawRail(RailCenterY.toPx()) }
                Box(
                    modifier = Modifier
                        .size(CapsuleWidth, RailAreaHeight)
                        .graphicsLayer { translationX = carriageX }
                        .drawBehind { drawCarriage(strike.value) },
                )
            }
        }
    }
}

/** Slate pipe: soft top edge, broad pale highlight band at ~40%, falling to near-black. */
private fun DrawScope.drawRail(centerY: Float) {
    val h = 13.5.dp.toPx()
    val top = centerY - h / 2f

    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF323639),
            0.077f to Color(0xFF3A3F42),
            0.154f to Color(0xFF4B5053),
            0.23f to Color(0xFF7A7F82),
            0.308f to Color(0xFFACB1B4),
            0.385f to Color(0xFFACB1B4),
            0.46f to Color(0xFF83888C),
            0.54f to Color(0xFF5F6468),
            0.615f to Color(0xFF575C60),
            0.69f to Color(0xFF4A4F52),
            0.77f to Color(0xFF373B3E),
            0.85f to Color(0xFF262A2B),
            0.92f to Color(0xFF1C2021),
            1f to Color(0xFF151515),
            startY = top,
            endY = top + h,
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, h),
    )

    // square charcoal collars at both ends
    val cw = 11.dp.toPx()
    val ch = 19.dp.toPx()
    val inset = 6.dp.toPx()
    for (left in booleanArrayOf(true, false)) {
        val x = if (left) inset else size.width - inset - cw
        val y = centerY - ch / 2f
        softShadow(
            topLeft = Offset(x, y + 2.dp.toPx()),
            size = Size(cw, ch),
            corner = 3.dp.toPx(),
            color = Color.Black,
            spread = 5.dp.toPx(),
            alpha = 0.7f,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF2B2D2C),
                0.12f to Color(0xFF323335),
                0.30f to Color(0xFF3D3E40),
                0.42f to Color(0xFF414244),
                0.55f to Color(0xFF3A3B3D),
                0.65f to Color(0xFF2E2F31),
                0.80f to Color(0xFF212224),
                0.90f to Color(0xFF181A19),
                1f to Color(0xFF0F0F0F),
                startY = y,
                endY = y + ch,
            ),
            topLeft = Offset(x, y),
            size = Size(cw, ch),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(x + 0.5.dp.toPx(), y + 0.5.dp.toPx()),
            size = Size(cw - 1.dp.toPx(), ch - 1.dp.toPx()),
            cornerRadius = CornerRadius(2.6.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** Smooth, blur-like shadow: stacked rounded rects with a quadratic falloff (no BlurMaskFilter needed). */
private fun DrawScope.softShadow(
    topLeft: Offset,
    size: Size,
    corner: Float,
    color: Color,
    spread: Float,
    alpha: Float,
    steps: Int = 12,
) {
    for (i in 0 until steps) {
        val t = i / (steps - 1f)
        val g = spread * t
        val a = alpha * (1f - t) * (1f - t) * 0.42f
        drawRoundRect(
            color = color.copy(alpha = a.coerceIn(0f, 1f)),
            topLeft = Offset(topLeft.x - g, topLeft.y - g),
            size = Size(size.width + g * 2f, size.height + g * 2f),
            cornerRadius = CornerRadius(corner + g),
        )
    }
}

/**
 * The carriage, to the reference's proportions: flat warm-white capsule with a soft bevel line near
 * the top, edge-shaded underside, a dark hammer slot on its underside, three small flat LEDs and a light-grey T hammer
 * with a dark cylinder bar.
 * Local origin = top-left of the capsule's box; the capsule sits low so the hammer has room above it.
 * @param strike 1 right after a keystroke, easing back to 0
 */
private fun DrawScope.drawCarriage(strike: Float) {
    val w = size.width
    val capH = CapsuleHeight.toPx()
    val s = strike.coerceIn(-0.3f, 1f)
    val capTop = RailCenterY.toPx() - capH / 2f
    val corner = CornerRadius(capH / 2f)
    val cx = w * 0.5f

    // ── smooth shadow cast by the capsule (visible on the pipe) ──
    softShadow(
        topLeft = Offset(4.dp.toPx(), capTop + 4.dp.toPx()),
        size = Size(w - 8.dp.toPx(), capH),
        corner = capH / 2f,
        color = Color.Black,
        spread = 8.dp.toPx(),
        alpha = 0.9f,
    )

    // ── T hammer: sits above the capsule and strikes UP at the search capsule ──
    val lift = 7.dp.toPx() * s
    val barW = 15.dp.toPx()
    val barH = 4.8.dp.toPx()
    val stemW = 3.4.dp.toPx()
    val stemLen = 11.dp.toPx()
    val barTop = capTop - stemLen - barH - lift
    softShadow(
        topLeft = Offset(cx - barW / 2f + 1.5.dp.toPx(), barTop + 2.dp.toPx()),
        size = Size(barW, barH),
        corner = 1.6.dp.toPx(),
        color = Color.Black,
        spread = 4.dp.toPx(),
        alpha = 0.5f,
        steps = 8,
    )
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFFD6D4CF),
            0.15f to Color(0xFFC7C5C2),
            0.80f to Color(0xFFC8C8C6),
            1f to Color(0xFFA9AAA4),
            startX = cx - stemW / 2f,
            endX = cx + stemW / 2f,
        ),
        topLeft = Offset(cx - stemW / 2f, barTop + barH - 0.5.dp.toPx()),
        size = Size(stemW, capTop - (barTop + barH) + 1.5.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF555A5D),
            0.25f to Color(0xFFA5AAAE),
            0.50f to Color(0xFF686D71),
            0.75f to Color(0xFF3F4346),
            1f to Color(0xFF202425),
            startY = barTop,
            endY = barTop + barH,
        ),
        topLeft = Offset(cx - barW / 2f, barTop),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(1.6.dp.toPx()),
    )
    for (ex in floatArrayOf(cx - barW / 2f + 0.4.dp.toPx(), cx + barW / 2f - 1.2.dp.toPx())) {
        drawRect(
            color = Color(0xFFB8BCC0).copy(alpha = 0.55f),
            topLeft = Offset(ex, barTop + 0.8.dp.toPx()),
            size = Size(0.8.dp.toPx(), barH - 1.6.dp.toPx()),
        )
    }

    // ── body: almost flat butter cream (#FCF2C7) ──
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFFFF8DC),
            0.06f to Color(0xFFFDF4CD),
            0.20f to Color(0xFFFCF3CB),
            0.30f to Color(0xFFFCF2C9),
            0.45f to Color(0xFFFCF2C7),
            0.80f to Color(0xFFFCF2C7),
            0.88f to Color(0xFFF9EEBE),
            0.93f to Color(0xFFF2E3AB),
            0.97f to Color(0xFFE3D08C),
            1f to Color(0xFFC9B374),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(0f, capTop),
        size = Size(w, capH),
        cornerRadius = corner,
    )
    // hairline outline
    drawRoundRect(
        color = Color(0xFFE8D9A0).copy(alpha = 0.75f),
        topLeft = Offset(0.4.dp.toPx(), capTop + 0.4.dp.toPx()),
        size = Size(w - 0.8.dp.toPx(), capH - 0.8.dp.toPx()),
        cornerRadius = corner,
        style = Stroke(width = 0.8.dp.toPx()),
    )
    // soft shading rolling the underside away from the light (wide + narrow stroke = smooth)
    val shade = Color(0xFFB59F5A)
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.50f to shade.copy(alpha = 0f),
            1f to shade.copy(alpha = 0.30f),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(2.dp.toPx(), capTop + 2.dp.toPx()),
        size = Size(w - 4.dp.toPx(), capH - 4.dp.toPx()),
        cornerRadius = CornerRadius(capH / 2f - 2.dp.toPx()),
        style = Stroke(width = 4.dp.toPx()),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.62f to shade.copy(alpha = 0f),
            1f to shade.copy(alpha = 0.55f),
            startY = capTop,
            endY = capTop + capH,
        ),
        topLeft = Offset(0.8.dp.toPx(), capTop + 0.8.dp.toPx()),
        size = Size(w - 1.6.dp.toPx(), capH - 1.6.dp.toPx()),
        cornerRadius = CornerRadius(capH / 2f - 0.8.dp.toPx()),
        style = Stroke(width = 1.6.dp.toPx()),
    )
    // faint darkening along the two rounded ends
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0f to Color(0xFFCDB878).copy(alpha = 0.32f),
            0.10f to Color.Transparent,
            0.90f to Color.Transparent,
            1f to Color(0xFFCDB878).copy(alpha = 0.32f),
            startX = 0f,
            endX = w,
        ),
        topLeft = Offset(1.dp.toPx(), capTop + 1.dp.toPx()),
        size = Size(w - 2.dp.toPx(), capH - 2.dp.toPx()),
        cornerRadius = CornerRadius(capH / 2f - 1.dp.toPx()),
        style = Stroke(width = 3.dp.toPx()),
    )
    // bevel line: a pale curved highlight ~20% down that bends round both ends and fades out
    val inset = capH * 0.19f
    val bevelBrush = Brush.verticalGradient(
        0f to Color(0xFFFFFDEB).copy(alpha = 0.95f),
        1f to Color(0xFFFFFDEB).copy(alpha = 0f),
        startY = capTop + inset,
        endY = capTop + capH * 0.52f,
    )
    drawRoundRect(
        brush = bevelBrush,
        topLeft = Offset(inset, capTop + inset),
        size = Size(w - inset * 2f, capH - inset * 2f),
        cornerRadius = CornerRadius(capH / 2f - inset),
        style = Stroke(width = 2.6.dp.toPx()),
        alpha = 0.35f,
    )
    drawRoundRect(
        brush = bevelBrush,
        topLeft = Offset(inset, capTop + inset),
        size = Size(w - inset * 2f, capH - inset * 2f),
        cornerRadius = CornerRadius(capH / 2f - inset),
        style = Stroke(width = 1.1.dp.toPx()),
    )

    // ── hammer slot: dark recess, lighter toward its lower lip ──
    val slotW = 16.5.dp.toPx()
    val slotH = 3.6.dp.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFF1D1C18),
            0.30f to Color(0xFF1D1C18),
            0.50f to Color(0xFF36332E),
            0.80f to Color(0xFF5B574E),
            1f to Color(0xFF6E6A5F),
            startY = capTop + capH - slotH,
            endY = capTop + capH,
        ),
        topLeft = Offset(cx - slotW / 2f, capTop + capH - slotH),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(1.2.dp.toPx()),
    )

    // ── LEDs: small and flat — green (amber while a key is struck), dark brown, dusty rose ──
    val ledR = 2.3.dp.toPx()
    val ledY = capTop + capH / 2f
    val ledX = w * 0.143f
    val gap = w * 0.113f
    val sc = s.coerceIn(0f, 1f)
    val c1 = Offset(ledX, ledY)
    drawCircle(lerp(Color(0xFFD0DAC6), Color(0xFFFFE3B8), sc), radius = ledR + 0.9.dp.toPx(), center = c1)
    drawCircle(
        brush = Brush.radialGradient(
            0f to lerp(Color(0xFF8CDA66), Color(0xFFFFB84D), sc),
            1f to lerp(Color(0xFF72C255), Color(0xFFF08F1F), sc),
            center = c1,
            radius = ledR,
        ),
        radius = ledR,
        center = c1,
    )
    val c2 = Offset(ledX + gap, ledY)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFF3A130B),
            1f to Color(0xFF240A04),
            center = c2,
            radius = ledR,
        ),
        radius = ledR,
        center = c2,
    )
    val c3 = Offset(ledX + gap * 2f, ledY)
    drawCircle(
        brush = Brush.radialGradient(
            0f to Color(0xFFD9A8A3),
            1f to Color(0xFFBF908B),
            center = c3,
            radius = ledR,
        ),
        radius = ledR,
        center = c3,
    )
}
