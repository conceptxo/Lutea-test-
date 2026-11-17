package com.rajatxo.lutea.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajatxo.lutea.R
import kotlin.math.min
import kotlin.math.sin

/**
 * The two animated empty-page styles. The name is stored by [name], so changing
 * the enum order later will not change an existing user's choice.
 */
internal enum class SearchIdleStyle { JELLY, REEF }

private val StyleCream = Color(0xFFFCF2C7)
private val StyleJelly = Color(0xFF9188FF)
private val StyleJellyPink = Color(0xFFFFA0C6)
private val StyleReef = Color(0xFFE8735A)

/**
 * The little glass button in the search page's top-right corner and the card it
 * opens. The card sits just under the button, grows out of the top-right corner,
 * and closes by tapping anywhere else or pressing back (handled by the caller).
 */
@Composable
internal fun SearchStyleMenu(
    selected: SearchIdleStyle,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (SearchIdleStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        GlassMenuButton(
            expanded = expanded,
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.align(Alignment.TopEnd),
        )

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + scaleIn(
                animationSpec = tween(230, easing = FastOutSlowInEasing),
                initialScale = 0.88f,
                transformOrigin = TransformOrigin(1f, 0f),
            ),
            exit = fadeOut(tween(140)) + scaleOut(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(1f, 0f),
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp),
        ) {
            StyleCard(selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun GlassMenuButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press by animateFloatAsState(
        targetValue = if (expanded) 0.94f else 1f,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "styleButtonPress",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .shadow(15.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.17f),
                        Color.White.copy(alpha = 0.055f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_style),
            contentDescription = if (expanded) "Close style menu" else "Choose idle style",
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                },
        )
    }
}

@Composable
private fun StyleCard(
    selected: SearchIdleStyle,
    onSelect: (SearchIdleStyle) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(222.dp)
            .shadow(24.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF2B2724).copy(alpha = 0.90f),
                    0.48f to Color(0xFF1A1816).copy(alpha = 0.90f),
                    1f to Color(0xFF100F0E).copy(alpha = 0.90f),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.17f), shape)
            // Keep taps on the card's header from falling through to the scrim.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "IDLE STYLE",
                color = StyleCream.copy(alpha = 0.82f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${SearchIdleStyle.entries.size}",
                color = StyleCream.copy(alpha = 0.28f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.size(8.dp))

        StyleOption(
            title = "Jelly Drift",
            subtitle = "Neon jellies",
            selected = selected == SearchIdleStyle.JELLY,
            preview = { JellyPreview(Modifier.fillMaxSize()) },
            onClick = { onSelect(SearchIdleStyle.JELLY) },
        )
        Spacer(Modifier.size(6.dp))
        StyleOption(
            title = "Reef Current",
            subtitle = "Ink reef + paper",
            selected = selected == SearchIdleStyle.REEF,
            preview = { ReefPreview(Modifier.fillMaxSize()) },
            onClick = { onSelect(SearchIdleStyle.REEF) },
        )

        Spacer(Modifier.size(7.dp))
        Text(
            text = "TAP TO SWITCH",
            color = StyleCream.copy(alpha = 0.24f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun StyleOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.025f))
            .border(
                width = 1.dp,
                color = if (selected) StyleCream.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.065f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp)),
        ) {
            preview()
        }
        Spacer(Modifier.size(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = StyleCream.copy(alpha = 0.94f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = subtitle,
                color = StyleCream.copy(alpha = 0.42f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(6.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (selected) StyleCream.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.18f),
                    shape = CircleShape,
                ),
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StyleCream),
                )
            }
        }
    }
}

@Composable
private fun JellyPreview(modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition(label = "jellyPreview").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jellyPreviewBob",
    )
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF302742), Color(0xFF15121E)),
            ),
        )
        val cx = size.width * 0.5f
        val cy = size.height * (0.48f - 0.035f * bob)
        val r = min(size.width, size.height) * 0.245f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(StyleJelly.copy(alpha = 0.65f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 2.2f,
            ),
            radius = r * 2.2f,
            center = Offset(cx, cy),
        )
        val bell = Path().apply {
            moveTo(cx - r, cy)
            cubicTo(cx - r, cy - r * 1.18f, cx + r, cy - r * 1.18f, cx + r, cy)
            close()
        }
        drawPath(
            path = bell,
            brush = Brush.verticalGradient(
                colors = listOf(StyleJellyPink.copy(alpha = 0.95f), StyleJelly.copy(alpha = 0.72f)),
                startY = cy - r,
                endY = cy,
            ),
        )
        drawLine(
            color = StyleJellyPink.copy(alpha = 0.85f),
            start = Offset(cx - r, cy),
            end = Offset(cx + r, cy),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round,
        )
        for (i in -2..2) {
            val x = cx + i * r * 0.36f
            drawLine(
                color = StyleJellyPink.copy(alpha = 0.58f),
                start = Offset(x, cy + r * 0.08f),
                end = Offset(x + sin(bob * 3.14f + i) * r * 0.24f, cy + r * 1.55f),
                strokeWidth = 1.1f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ReefPreview(modifier: Modifier = Modifier) {
    val glow by rememberInfiniteTransition(label = "reefPreview").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "reefPreviewGlow",
    )
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2A211D), Color(0xFF151110)),
            ),
        )
        val paperLeft = size.width * 0.17f
        val paperTop = size.height * 0.16f
        drawRoundRect(
            color = Color(0xFF35302A),
            topLeft = Offset(paperLeft, paperTop),
            size = Size(size.width * 0.66f, size.height * 0.67f),
            cornerRadius = CornerRadius(3f, 3f),
        )
        drawRoundRect(
            color = StyleCream.copy(alpha = 0.16f),
            topLeft = Offset(paperLeft, paperTop),
            size = Size(size.width * 0.66f, size.height * 0.67f),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = 0.8f),
        )
        val rootX = size.width * 0.39f
        val path = Path().apply {
            moveTo(rootX, size.height * 0.82f)
            lineTo(rootX, size.height * 0.33f)
            moveTo(rootX, size.height * 0.57f)
            lineTo(size.width * 0.68f, size.height * 0.39f)
            moveTo(rootX, size.height * 0.65f)
            lineTo(size.width * 0.26f, size.height * 0.45f)
            moveTo(size.width * 0.68f, size.height * 0.39f)
            lineTo(size.width * 0.79f, size.height * 0.29f)
        }
        drawPath(
            path = path,
            color = StyleReef.copy(alpha = 0.9f * glow),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(
            color = StyleCream.copy(alpha = 0.82f * glow),
            radius = 1.6f,
            center = Offset(rootX, size.height * 0.33f),
        )
        drawCircle(
            color = StyleCream.copy(alpha = 0.7f * glow),
            radius = 1.4f,
            center = Offset(size.width * 0.79f, size.height * 0.29f),
        )
    }
}
