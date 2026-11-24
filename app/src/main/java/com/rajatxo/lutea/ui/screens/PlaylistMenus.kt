package com.rajatxo.lutea.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.rajatxo.lutea.domain.model.Song
import com.rajatxo.lutea.ui.components.CalSansFamily

// ─────────────────────────────────────────────────────────────────────────────
// Playlist menus: custom-order (sort) + per-song three-dot.
//
// These are Mochy-/RiPlay-inspired option sets, implemented from scratch for
// Lutea's glass UI. The custom-order menu offers every RiPlay sort mode plus a
// leading "Pin" row. The song menu offers the Mochy-style action set.
// ─────────────────────────────────────────────────────────────────────────────

/** Sort modes for the custom-order menu. */
enum class PlaylistSortMode(val label: String) {
    PIN("Pin"),
    TITLE("Title"),
    DATE_PLAYED("Date played"),
    DATE_LIKED("Date liked"),
    LISTENING_TIME("Listening time"),
    RELATIVE_LISTENING_TIME("Relative listening time"),
    POSITION("Position"),
    ARTIST("Artist"),
    ALBUM("Album"),
    ARTIST_ALBUM("Artist, Album"),
    ALBUM_YEAR("Album year"),
    DATE_ADDED("Date added"),
    DURATION("Duration"),
}

// ── Glyphs for the menus. Drawn on Canvas to match the rest of the detail UI. ──

@Composable
fun MenuPinGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        // Head circle.
        drawCircle(
            color = color,
            radius = w * 0.16f,
            center = Offset(w * 0.5f, h * 0.24f),
            style = Stroke(width = stroke),
        )
        // Body line down to the point.
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.40f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun MenuHeartGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        // Two arcs + a point — a simple heart.
        val r = w * 0.18f
        drawCircle(
            color = color,
            radius = r,
            center = Offset(w * 0.34f, h * 0.38f),
            style = if (filled) Fill else Stroke(width = stroke),
        )
        drawCircle(
            color = color,
            radius = r,
            center = Offset(w * 0.66f, h * 0.38f),
            style = if (filled) Fill else Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.22f, h * 0.46f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.78f, h * 0.46f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun MenuPlaylistAddGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        // Three horizontal lines (a list) + a plus.
        for (i in 0..2) {
            val y = h * (0.28f + 0.16f * i)
            drawLine(
                color = color,
                start = Offset(w * 0.18f, y),
                end = Offset(if (i < 2) w * 0.82f else w * 0.55f, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        // Plus at bottom-right.
        drawLine(color, Offset(w * 0.66f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.74f, h * 0.64f), Offset(w * 0.74f, h * 0.80f), stroke, StrokeCap.Round)
    }
}

@Composable
fun MenuAlbumGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        drawCircle(color, w * 0.32f, Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
        drawCircle(color, w * 0.07f, Offset(w * 0.5f, h * 0.5f))
    }
}

@Composable
fun MenuArtistGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        drawCircle(color, w * 0.18f, Offset(w * 0.5f, h * 0.32f), style = Stroke(width = stroke))
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.50f),
            size = Size(w * 0.64f, h * 0.45f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun MenuEditGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        // Pencil: a diagonal line with a tip.
        drawLine(color, Offset(w * 0.24f, h * 0.76f), Offset(w * 0.68f, h * 0.32f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.32f), Offset(w * 0.76f, h * 0.24f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.32f, h * 0.72f), stroke, StrokeCap.Round)
    }
}

@Composable
fun MenuQueueGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        // List lines + a play triangle.
        for (i in 0..1) {
            val y = h * (0.30f + 0.18f * i)
            drawLine(color, Offset(w * 0.18f, y), Offset(w * 0.55f, y), stroke, StrokeCap.Round)
        }
        val p = Path().apply {
            moveTo(w * 0.62f, h * 0.26f)
            lineTo(w * 0.82f, h * 0.40f)
            lineTo(w * 0.62f, h * 0.54f)
            close()
        }
        drawPath(p, color)
        drawLine(color, Offset(w * 0.18f, h * 0.66f), Offset(w * 0.82f, h * 0.66f), stroke, StrokeCap.Round)
    }
}

@Composable
fun MenuHomeGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.20f)
            lineTo(w * 0.82f, h * 0.48f)
            lineTo(w * 0.82f, h * 0.80f)
            lineTo(w * 0.18f, h * 0.80f)
            lineTo(w * 0.18f, h * 0.48f)
            close()
        }
        drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun MenuDeleteGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        drawLine(color, Offset(w * 0.20f, h * 0.30f), Offset(w * 0.80f, h * 0.30f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.40f, h * 0.30f), Offset(w * 0.40f, h * 0.22f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.60f, h * 0.30f), Offset(w * 0.60f, h * 0.22f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.30f, h * 0.30f), Offset(w * 0.36f, h * 0.78f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.70f, h * 0.30f), Offset(w * 0.64f, h * 0.78f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.36f, h * 0.78f), Offset(w * 0.64f, h * 0.78f), stroke, StrokeCap.Round)
    }
}

@Composable
fun MenuInfoGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        drawCircle(color, w * 0.36f, Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke))
        drawCircle(color, w * 0.05f, Offset(w * 0.5f, h * 0.30f))
        drawLine(color, Offset(w * 0.5f, h * 0.42f), Offset(w * 0.5f, h * 0.70f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun MenuText(text: String, size: Int = 15, destructive: Boolean = false) {
    Text(
        text = text,
        color = if (destructive) Color(0xFFFF6B6B) else Color.White,
        fontSize = size.sp,
        fontFamily = CalSansFamily,
        fontWeight = FontWeight.Normal,
    )
}

/** A single row in a menu: glyph + label, pressable. */
@Composable
private fun MenuActionRow(
    label: String,
    destructive: Boolean = false,
    selected: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val iconColor = if (destructive) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.85f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Spacer(Modifier.width(14.dp))
        MenuText(label, destructive = destructive)
        if (selected) {
            Spacer(Modifier.weight(1f))
            // A small dot marks the active sort.
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
            )
        }
    }
}

/** The glass card backing shared by both menus. */
@Composable
private fun MenuCard(
    modifier: Modifier,
    width: Dp,
    backdrop: LayerBackdrop?,
    content: @Composable () -> Unit,
) {
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .width(width)
            .clip(cardShape)
            .then(
                if (backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { cardShape },
                        effects = {
                            vibrancy()
                            colorControls(brightness = 0.05f, contrast = 1f, saturation = 1.2f)
                            blur(20.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.42f)) },
                    )
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.55f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), cardShape)
            .padding(vertical = 6.dp),
    ) { content() }
}

// ── Custom-order (sort) menu. ────────────────────────────────────────────────

/**
 * A dropdown listing every sort mode (RiPlay's set) plus a leading "Pin" row.
 * The active mode is marked. Selecting a mode dismisses and reports it.
 */
@Composable
fun PlaylistCustomOrderMenu(
    anchorY: Dp,
    endPad: Dp,
    current: PlaylistSortMode,
    backdrop: LayerBackdrop?,
    onDismiss: () -> Unit,
    onSelect: (PlaylistSortMode) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = interaction, indication = null, onClick = onDismiss),
    ) {
        MenuCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = anchorY, end = endPad),
            width = 232.dp,
            backdrop = backdrop,
        ) {
            PlaylistSortMode.entries.forEach { mode ->
                MenuActionRow(
                    label = mode.label,
                    selected = mode == current,
                    icon = { sortIcon(mode, Color.White.copy(alpha = 0.85f)) },
                    onClick = { onDismiss(); onSelect(mode) },
                )
            }
        }
    }
}

@Composable
private fun sortIcon(mode: PlaylistSortMode, color: Color) {
    val m = Modifier.size(16.dp)
    when (mode) {
        PlaylistSortMode.PIN -> MenuPinGlyph(color, m)
        PlaylistSortMode.TITLE,
        PlaylistSortMode.ARTIST,
        PlaylistSortMode.ALBUM,
        PlaylistSortMode.ARTIST_ALBUM,
        PlaylistSortMode.ALBUM_YEAR,
        PlaylistSortMode.POSITION -> MenuArtistGlyph(color, m)
        PlaylistSortMode.DATE_PLAYED,
        PlaylistSortMode.DATE_LIKED,
        PlaylistSortMode.DATE_ADDED -> MenuInfoGlyph(color, m)
        PlaylistSortMode.LISTENING_TIME,
        PlaylistSortMode.RELATIVE_LISTENING_TIME,
        PlaylistSortMode.DURATION -> MenuQueueGlyph(color, m)
    }
}

// ── Per-song three-dot menu. ─────────────────────────────────────────────────

/**
 * The Mochy-style song action sheet: Favourite toggle, Add to other playlist,
 * View album, View artist, Edit (placeholder), Add to queue, Pin to home
 * (placeholder), Remove from playlist, Details.
 *
 * Each action dismisses the menu first, then fires its callback. Callbacks the
 * app doesn't wire yet are no-ops (Edit, Pin to home) — they are present so the
 * full option list shows; behaviour comes in a later pass.
 */
@Composable
fun PlaylistSongMenu(
    song: Song,
    isFavourite: Boolean,
    anchorY: Dp,
    endPad: Dp,
    backdrop: LayerBackdrop?,
    onDismiss: () -> Unit,
    onFavourite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onViewAlbum: () -> Unit,
    onViewArtist: () -> Unit,
    onEdit: () -> Unit,
    onAddToQueue: () -> Unit,
    onPinToHome: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onDetails: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = interaction, indication = null, onClick = onDismiss),
    ) {
        MenuCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = anchorY, end = endPad),
            width = 248.dp,
            backdrop = backdrop,
        ) {
            MenuActionRow(
                label = if (isFavourite) "Unfavourite" else "Favourite",
                icon = { MenuHeartGlyph(Color.White.copy(alpha = 0.85f), filled = isFavourite, Modifier.size(16.dp)) },
                onClick = { onDismiss(); onFavourite() },
            )
            MenuActionRow(
                label = "Add to other playlist",
                icon = { MenuPlaylistAddGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onAddToPlaylist() },
            )
            MenuActionRow(
                label = "View album",
                icon = { MenuAlbumGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onViewAlbum() },
            )
            MenuActionRow(
                label = "View artist",
                icon = { MenuArtistGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onViewArtist() },
            )
            MenuActionRow(
                label = "Edit",
                icon = { MenuEditGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onEdit() },
            )
            MenuActionRow(
                label = "Add to queue",
                icon = { MenuQueueGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onAddToQueue() },
            )
            MenuActionRow(
                label = "Pin to home page",
                icon = { MenuHomeGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onPinToHome() },
            )
            MenuActionRow(
                label = "Remove from playlist",
                destructive = true,
                icon = { MenuDeleteGlyph(Color(0xFFFF6B6B), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onRemoveFromPlaylist() },
            )
            MenuActionRow(
                label = "Details",
                icon = { MenuInfoGlyph(Color.White.copy(alpha = 0.85f), Modifier.size(16.dp)) },
                onClick = { onDismiss(); onDetails() },
            )
        }
    }
}
