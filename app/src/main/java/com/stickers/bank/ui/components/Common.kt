package com.stickers.bank.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.stickers.bank.data.model.PageBackground
import com.stickers.bank.data.model.Sticker
import com.stickers.bank.ui.theme.AppBackground
import com.stickers.bank.ui.theme.OnGold
import com.stickers.bank.ui.theme.Gold
import com.stickers.bank.ui.theme.AppOnSurface
import com.stickers.bank.ui.theme.AppOnSurfaceMuted
import com.stickers.bank.ui.theme.AppSurfaceVariant
import com.stickers.bank.ui.theme.neuClickable
import com.stickers.bank.ui.theme.parseHexColor

/** Header used on drill-down / detail screens: back arrow + centered gold title. */
@Composable
fun SbBackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).neuClickable(cornerRadius = 20.dp, elevation = 6.dp, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = AppOnSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title.uppercase(),
            color = Gold,
            fontSize = 18.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 40.dp),
        )
    }
}

/** The BANK | WHATSAPP bar seen at the bottom of catalog screens. */
@Composable
fun BottomActionBar(
    onBank: () -> Unit,
    onWhatsApp: () -> Unit,
    modifier: Modifier = Modifier,
    bankLabel: String = "BANK",
    actionLabel: String = "WHATSAPP",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .height(52.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .neuClickable(cornerRadius = 14.dp, elevation = 6.dp, onClick = onBank),
            contentAlignment = Alignment.Center,
        ) { Text(bankLabel, color = AppOnSurface, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp) }
        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxSize()
                .neuClickable(cornerRadius = 14.dp, elevation = 6.dp, background = Gold, onClick = onWhatsApp),
            contentAlignment = Alignment.Center,
        ) { Text(actionLabel, color = OnGold, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp) }
    }
}

/** A square sticker cell for grids. */
@Composable
fun StickerThumb(
    sticker: Sticker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    cornerRadius: Int = 12,
) {
    Box(
        // Floating (raised) at rest, like every other tappable surface — presses INTO the page
        // only while actually held, via neuClickable. It used to sit permanently inset (a fixed
        // "well" look); that read as pre-pressed even when nobody was touching it.
        modifier = modifier
            .aspectRatio(1f)
            .neuClickable(cornerRadius = cornerRadius.dp, elevation = 5.dp, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = sticker.coilModel(),
            contentDescription = sticker.caption.ifBlank { sticker.id },
            contentScale = ContentScale.Fit,
            // Clear of the raised surface (painted 5dp — its elevation — inside the cell) and of
            // the pressed state's inner shadows, so the float/press shows round every sticker
            // instead of being covered by stickers that fill their whole square.
            modifier = Modifier.fillMaxSize().padding(10.dp),
        )
        if (selected) {
            Box(Modifier.fillMaxSize().background(Gold.copy(alpha = 0.18f)))
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = Gold,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp),
            )
        }
    }
}

/**
 * Subtle transparency checkerboard so transparent stickers read clearly.
 * Uses a cached, GPU-tiled brush (a tiny bitmap repeated) instead of drawing hundreds of
 * rects per frame — this keeps grids scrolling smoothly.
 */
fun Modifier.checkerboard(cell: Float = 22f): Modifier =
    this.drawBehind { drawRect(checkerBrush) }

/**
 * A clearly-visible checkerboard for the EDITOR canvas only. The app-wide [checkerboard] is
 * deliberately near-black (so transparent stickers don't show a loud grid in lists); but inside
 * the editor a user needs erased/empty areas to obviously read as *transparent*, not black —
 * so this one uses medium greys.
 */
fun Modifier.editorCheckerboard(): Modifier =
    this.drawBehind { drawRect(editorCheckerBrush) }

private val editorCheckerTile: androidx.compose.ui.graphics.ImageBitmap by lazy {
    val c = 11
    val bmp = androidx.compose.ui.graphics.ImageBitmap(c * 2, c * 2)
    val canvas = androidx.compose.ui.graphics.Canvas(bmp)
    val paint = androidx.compose.ui.graphics.Paint()
    val light = Color(0xFF6E6E78)
    val dark = Color(0xFF4A4A52)
    paint.color = dark
    canvas.drawRect(0f, 0f, (c * 2).toFloat(), (c * 2).toFloat(), paint)
    paint.color = light
    canvas.drawRect(0f, 0f, c.toFloat(), c.toFloat(), paint)
    canvas.drawRect(c.toFloat(), c.toFloat(), (c * 2).toFloat(), (c * 2).toFloat(), paint)
    bmp
}

private val editorCheckerBrush: androidx.compose.ui.graphics.Brush by lazy {
    androidx.compose.ui.graphics.ShaderBrush(
        androidx.compose.ui.graphics.ImageShader(
            editorCheckerTile,
            androidx.compose.ui.graphics.TileMode.Repeated,
            androidx.compose.ui.graphics.TileMode.Repeated,
        ),
    )
}

private val checkerTile: androidx.compose.ui.graphics.ImageBitmap by lazy {
    val c = 11
    val bmp = androidx.compose.ui.graphics.ImageBitmap(c * 2, c * 2)
    val canvas = androidx.compose.ui.graphics.Canvas(bmp)
    val paint = androidx.compose.ui.graphics.Paint()
    val light = Color(0xFF1D1D25)
    val dark = Color(0xFF15151B)
    paint.color = dark
    canvas.drawRect(0f, 0f, (c * 2).toFloat(), (c * 2).toFloat(), paint)
    paint.color = light
    canvas.drawRect(0f, 0f, c.toFloat(), c.toFloat(), paint)
    canvas.drawRect(c.toFloat(), c.toFloat(), (c * 2).toFloat(), (c * 2).toFloat(), paint)
    bmp
}

private val checkerBrush: androidx.compose.ui.graphics.Brush by lazy {
    androidx.compose.ui.graphics.ShaderBrush(
        androidx.compose.ui.graphics.ImageShader(
            checkerTile,
            androidx.compose.ui.graphics.TileMode.Repeated,
            androidx.compose.ui.graphics.TileMode.Repeated,
        ),
    )
}

/** Big rounded gold action button used on Create/empty screens. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .neuClickable(cornerRadius = 14.dp, elevation = 7.dp, background = if (filled) Gold else AppSurfaceVariant, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) AppBackground else Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = AppOnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        Text(
            subtitle,
            color = AppOnSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * Wraps a horizontally-scrollable [content] and overlays a right-edge fade + gold chevron while
 * there is more to scroll (so users can tell the row scrolls). [fadeColor] should match the
 * background the row sits on so the fade blends in. Reusable across every horizontal scroller.
 */
@Composable
fun HScrollHint(
    scrollState: ScrollState,
    fadeColor: Color = AppBackground,
    content: @Composable () -> Unit,
) {
    // height(IntrinsicSize.Min) bounds this Box to its content's actual height — without it, a
    // loose parent (e.g. a Column with no fixed height) lets fillMaxHeight() below expand to
    // the whole available region, so the arrow ends up centered on the *screen*, not the row.
    Box(Modifier.height(IntrinsicSize.Min)) {
        content()
        if (scrollState.canScrollForward) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(42.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, fadeColor))),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "More", tint = Gold, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * Admin-set backdrop for a whole page or a category screen: a solid color fill, then an image
 * cropped on top of it (the color shows through as a fallback edge/behind transparency). Paints
 * nothing when [bg] is null/empty, so the caller's own page underneath — normally the app's
 * plain flat background from MainActivity — shows through unchanged.
 */
@Composable
fun PageBackdrop(bg: PageBackground?, modifier: Modifier = Modifier) {
    if (bg == null || bg.isEmpty) return
    val color = remember(bg.color) { parseHexColor(bg.color) }
    Box(modifier.fillMaxSize().then(if (color != null) Modifier.background(color) else Modifier)) {
        bg.image?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
