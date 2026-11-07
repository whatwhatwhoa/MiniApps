package io.github.whatwhatwhoa.stack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max

private val CardShape = RoundedCornerShape(12.dp)
private val ButtonShape = RoundedCornerShape(8.dp)

/** The page column: max 1240 dp, a 16 dp side gutter. Blocks inside are spaced 14 dp apart. */
fun Modifier.stackColumn(): Modifier = this.widthIn(max = 1240.dp).fillMaxWidth().padding(horizontal = 16.dp)

/** The gap between blocks on a page (guide §5). */
val BlockGap = 14.dp

/** Brand (title + a mono count) on the left, actions on the right; the Theme icon by default. */
@Composable
fun StackHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    actions: @Composable RowScope.() -> Unit = { ThemeIconButton() },
) {
    val c = Stack.colors
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The brand wraps as the web .brand does: at a large font the count goes under the title.
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            itemVerticalAlignment = Alignment.Bottom,
        ) {
            Text(title, style = StackType.h1, color = c.ink, maxLines = 1)
            if (count != null) {
                Text(count, Modifier.padding(bottom = 2.dp), style = StackType.mono, color = c.muted, maxLines = 1)
            }
        }
        actions()
    }
}

/**
 * The Theme button, as an icon in every app (owner: the icon in all of them): a half-filled circle
 * for Auto, a sun for Light, a moon for Dark. A tap cycles Auto → Light → Dark and remembers it.
 */
@Composable
fun ThemeIconButton(modifier: Modifier = Modifier) {
    val t = LocalThemeControl.current
    IconBox(
        modifier,
        Modifier.clickable(role = Role.Button) { t.set(t.mode.next()) }
            .semantics { contentDescription = "Theme: ${t.mode.label}; tap for ${t.mode.next().label}" },
    ) { ink, panel ->
        val stroke = 1.6.dp.toPx()
        val mid = Offset(size.width / 2, size.height / 2)
        when (t.mode) {
            ThemeMode.Auto -> {
                val r = size.minDimension * 0.42f
                drawCircle(ink, r, mid, style = Stroke(stroke))
                drawArc(ink, 90f, 180f, true, Offset(mid.x - r, mid.y - r), Size(2 * r, 2 * r))
            }
            ThemeMode.Light -> {
                val r = size.minDimension * 0.22f
                drawCircle(ink, r, mid, style = Stroke(stroke))
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    val dx = kotlin.math.cos(a).toFloat()
                    val dy = kotlin.math.sin(a).toFloat()
                    val r1 = size.minDimension * 0.34f
                    val r2 = size.minDimension * 0.48f
                    drawLine(ink, Offset(mid.x + dx * r1, mid.y + dy * r1), Offset(mid.x + dx * r2, mid.y + dy * r2),
                        stroke, StrokeCap.Round)
                }
            }
            ThemeMode.Dark -> {
                // A crescent: the disc, with a second disc in the button's color cut out of it.
                val r = size.minDimension * 0.42f
                drawCircle(ink, r, mid)
                drawCircle(panel, r * 0.86f, Offset(mid.x + r * 0.55f, mid.y - r * 0.35f))
            }
        }
    }
}

/**
 * Settings as an icon (a gear) beside the Theme icon, in a one-row header (owner: "make settings
 * to the settings icon"). [selected] while the Settings page is open: then it is filled with
 * the soft accent and edged with the accent, as a tab would be underlined.
 */
@Composable
fun SettingsIconButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconBox(
        modifier,
        Modifier.selectable(selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = "Settings" },
        selected = selected,
    ) { ink, panel ->
        val mid = Offset(size.width / 2, size.height / 2)
        val s = size.minDimension
        // Eight teeth, the body over them, and the hole in the button's color.
        for (k in 0 until 8) {
            val a = Math.toRadians(k * 45.0 + 22.5)
            val dx = kotlin.math.cos(a).toFloat()
            val dy = kotlin.math.sin(a).toFloat()
            drawLine(ink, Offset(mid.x + dx * s * 0.25f, mid.y + dy * s * 0.25f),
                Offset(mid.x + dx * s * 0.47f, mid.y + dy * s * 0.47f), s * 0.17f, StrokeCap.Butt)
        }
        drawCircle(ink, s * 0.33f, mid)
        drawCircle(panel, s * 0.14f, mid)
    }
}

/**
 * The 40 dp square both header icons sit in. [action] (the click) goes inside the clip, so its
 * ripple keeps the corners; [draw] gets the ink and the button's own color.
 */
@Composable
private fun IconBox(
    modifier: Modifier,
    action: Modifier,
    selected: Boolean = false,
    draw: DrawScope.(ink: Color, panel: Color) -> Unit,
) {
    val c = Stack.colors
    val fill = if (selected) c.accentSoft else c.panel
    Box(
        modifier
            .size(40.dp)
            .clip(ButtonShape)
            .background(fill)
            .border(1.dp, if (selected) c.accent else c.line, ButtonShape)
            .then(action),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) { draw(c.ink, fill) }
    }
}

/**
 * Tabs under the header: muted labels, the selected one in ink over a 2 dp accent line.
 * [compact] narrows each tab, for a row that also holds the header's buttons. [brand]: the first
 * tab is the app's name, the header of a one-row app, so it is set in the title's bold [StackType.h1],
 * larger than the other tabs (owner: "the bolded main tab font").
 */
@Composable
fun StackTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<String?> = emptyList(),
    compact: Boolean = false,
    brand: Boolean = false,
) {
    val c = Stack.colors
    Box(
        modifier.fillMaxWidth().drawBehind {
            drawRect(c.line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
        }
    ) {
        // Bottom-aligned, so the accent lines sit on the one line even when the name is taller.
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                Row(
                    Modifier
                        .selectable(on, role = Role.Tab) { onSelect(i) }
                        .drawBehind {
                            if (on) drawRect(c.accent, Offset(0f, size.height - 2.dp.toPx()),
                                Size(size.width, 2.dp.toPx()))
                        }
                        .heightIn(min = 44.dp)
                        // 7 dp in a compact row: with the larger name, Calculator's row still
                        // fits a 360 dp phone at the normal font size.
                        .padding(horizontal = if (compact) 7.dp else 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val style = if (brand && i == 0) StackType.h1 else StackType.tab
                    Text(label, style = style, color = if (on) c.ink else c.muted, maxLines = 1)
                    counts.getOrNull(i)?.let {
                        Text(it, Modifier.background(c.grid, CircleShape).padding(horizontal = 7.dp, vertical = 1.dp),
                            style = StackType.mono.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                            color = c.muted)
                    }
                }
            }
        }
    }
}

/**
 * A card: panel, 1 dp line, 12 dp radius. [title] with an optional [info] tooltip and an
 * [action] on the right; a 4 dp [stripe] on the left edge marks a category or series.
 */
@Composable
fun StackCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    info: String? = null,
    stripe: Color? = null,
    action: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Stack.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.panel)
            .border(1.dp, c.line, CardShape)
            .drawBehind { if (stripe != null) drawRect(stripe, size = Size(4.dp.toPx(), size.height)) }
            .padding(start = if (stripe != null) 18.dp else 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null || action != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) Text(title, style = StackType.h2, color = c.ink)
                    if (info != null) InfoDot(info)
                }
                action?.invoke(this)
            }
        }
        content()
    }
}

/** A KPI tile: uppercase label (+ tooltip), a big value with its unit, a context line. */
@Composable
fun Tile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    info: String? = null,
    sub: String? = null,
    valueColor: Color = Color.Unspecified,
    phone: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
) {
    val c = Stack.colors
    Column(
        modifier.clip(CardShape).background(c.panel).border(1.dp, c.line, CardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), Modifier.weight(1f, fill = false), style = StackType.label, color = c.muted)
            if (info != null) InfoDot(info)
        }
        Row {
            Text(value, Modifier.alignByBaseline(),
                style = if (phone) StackType.tileValuePhone else StackType.tileValue,
                color = if (valueColor == Color.Unspecified) c.ink else valueColor, maxLines = 1)
            if (unit != null) {
                Text(unit, Modifier.alignByBaseline().padding(start = 3.dp), style = StackType.tileUnit,
                    color = c.muted, maxLines = 1)
            }
        }
        if (sub != null) Text(sub, style = StackType.sub, color = c.muted)
        footer?.invoke()
    }
}

/** Tiles in rows of [columns], each row as tall as its tallest tile. */
@Composable
fun TileGrid(
    columns: Int,
    tiles: List<@Composable (Modifier) -> Unit>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { it(Modifier.weight(1f).fillMaxHeight()) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

enum class PillKind { Ok, Warn, Bad, Mute }

/** State in form (guide §2): a dot and a word in the status color, outlined. */
@Composable
fun Pill(text: String, kind: PillKind, modifier: Modifier = Modifier) {
    val c = Stack.colors
    val color = when (kind) {
        PillKind.Ok -> c.good
        PillKind.Warn -> c.handle
        PillKind.Bad -> c.danger
        PillKind.Mute -> c.muted
    }
    Row(
        modifier.border(1.dp, color, CircleShape).padding(horizontal = 8.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(text, style = StackType.pill, color = color, maxLines = 1)
    }
}

/** A small square of a series color, beside its label. */
@Composable
fun Swatch(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).background(color, RoundedCornerShape(3.dp)))
}

/** A legend or filter chip: swatch, label, an optional mono count. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    selected: Boolean = false,
    count: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Stack.colors
    val key = color ?: c.accent
    Row(
        modifier
            .clip(CircleShape)
            .background(if (selected) key.copy(alpha = 0.14f).compositeOver(c.panel) else c.panel)
            .border(1.dp, if (selected) key else c.line, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (color != null) Swatch(color)
        Text(text, style = StackType.seg.copy(fontSize = 12.5.sp), color = c.ink, maxLines = 1)
        if (count != null) Text(count, style = StackType.mono, color = c.muted)
    }
}

private fun Color.compositeOver(under: Color): Color {
    val a = alpha
    return Color(red * a + under.red * (1 - a), green * a + under.green * (1 - a), blue * a + under.blue * (1 - a), 1f)
}

/**
 * The segmented control: options in a bordered strip, the selected one filled with the accent.
 * Wraps onto a second line when it doesn't fit, as the web `.seg` does.
 */
@Composable
fun <T> Seg(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: (T) -> Boolean = { true },
    optionStyle: (T) -> TextStyle = { StackType.seg },
) {
    val c = Stack.colors
    FlowRow(
        modifier.clip(RoundedCornerShape(9.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(9.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            val ok = enabled(o)
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (on) c.accent else Color.Transparent)
                    .selectable(on, enabled = ok, role = Role.RadioButton) { onSelect(o) }
                    .heightIn(min = 34.dp)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = optionStyle(o),
                    color = when { on -> c.accentInk; ok -> c.ink; else -> c.muted.copy(alpha = 0.5f) },
                    maxLines = 1)
            }
        }
    }
}

/** A field label: 12 sp uppercase, muted. */
@Composable
fun FLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, style = StackType.label, color = Stack.colors.muted)
}

/** A button: panel with a line border, or [primary] in the accent. Says exactly what happens. */
@Composable
fun StackButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    small: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Stack.colors
    Row(
        modifier
            .clip(ButtonShape)
            .background(if (primary) c.accent else c.panel)
            .border(1.dp, if (primary) c.accent else c.line, ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = if (small) 34.dp else 40.dp)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke()
        Text(text,
            style = if (small) StackType.button.copy(fontSize = 13.sp) else StackType.button,
            color = (if (primary) c.accentInk else c.ink).copy(alpha = if (enabled) 1f else 0.45f),
            maxLines = 1)
    }
}

/** A one-line text field in the Stack look: line border, 2 dp accent when focused, a muted suffix. */
@Composable
fun StackField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    mono: Boolean = true,
    description: String? = null,
) {
    val c = Stack.colors
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val style = (if (mono) StackType.monoBody else StackType.body).copy(color = c.ink)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(ButtonShape)
                    .background(c.panel)
                    .border(if (focused) 2.dp else 1.dp, if (focused) c.accent else c.line, ButtonShape)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) Text(placeholder, style = style, color = c.muted.copy(alpha = 0.7f))
                    inner()
                }
                if (suffix != null) Text(suffix, Modifier.padding(start = 6.dp), style = StackType.body, color = c.muted)
            }
        },
    )
}

/** A callout: an amber bar and a muted note, for a caveat the reader must not miss. */
@Composable
fun Callout(text: String, modifier: Modifier = Modifier) {
    val c = Stack.colors
    Row(modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(c.handle))
        Text(text, Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp), style = StackType.note, color = c.muted)
    }
}

/** Notes at the bottom of a card: definitions and caveats, one line per point. */
@Composable
fun Notes(lines: List<String>, modifier: Modifier = Modifier) {
    val c = Stack.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach {
            Row {
                Text("•", Modifier.width(14.dp), style = StackType.note, color = c.muted)
                Text(it, style = StackType.note, color = c.muted)
            }
        }
    }
}

/** A label and its value on one line, the value right-aligned in tabular figures. */
@Composable
fun KeyValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    val c = Stack.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = StackType.body.copy(fontSize = 14.sp), color = c.muted, maxLines = 1)
        Text(value, style = StackType.body.copy(fontSize = 14.sp, fontFeatureSettings = TNUM),
            color = if (valueColor == Color.Unspecified) c.ink else valueColor, maxLines = 1)
    }
}

/**
 * The (i) after a label: tap it for the definition, in one floating tip (guide: every metric
 * explains itself). The touch area is larger than the 15 dp ring.
 */
@Composable
fun InfoDot(tip: String, modifier: Modifier = Modifier) {
    val c = Stack.colors
    var open by remember { mutableStateOf(false) }
    Box(
        modifier
            .padding(start = 2.dp)
            .size(26.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { open = !open }
            .semantics { contentDescription = tip },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(15.dp).border(1.dp, c.muted.copy(alpha = 0.8f), CircleShape), contentAlignment = Alignment.Center) {
            Text("i", style = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                lineHeight = 10.sp), color = c.muted)
        }
        if (open) {
            Popup(popupPositionProvider = TipPosition, onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true)) {
                Text(
                    tip,
                    Modifier.widthIn(max = 320.dp).padding(8.dp)
                        .shadow(8.dp, ButtonShape).background(c.ink, ButtonShape)
                        .padding(horizontal = 11.dp, vertical = 8.dp),
                    style = StackType.note.copy(fontSize = 12.5.sp),
                    color = c.panel,
                )
            }
        }
    }
}

/** Below the (i) if it fits, else above; never off the screen's sides. */
private object TipPosition : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, max(0, windowSize.width - popupContentSize.width))
        val below = anchorBounds.bottom
        val y = if (below + popupContentSize.height <= windowSize.height) below
        else (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/**
 * The cutting mat: the dark board with its grid where a tool shows what it is working on (the
 * guide's empty state). [grid] null leaves the grid to the content, e.g. a grid in real units.
 */
@Composable
fun Mat(modifier: Modifier = Modifier, grid: Dp? = 32.dp, content: @Composable BoxScope.() -> Unit) {
    val c = Stack.colors
    Box(
        modifier
            .clip(CardShape)
            .background(c.mat)
            .drawBehind {
                if (grid != null) {
                    val step = grid.toPx()
                    var x = step
                    while (x < size.width) { drawRect(c.matLine, Offset(x, 0f), Size(1f, size.height)); x += step }
                    var y = step
                    while (y < size.height) { drawRect(c.matLine, Offset(0f, y), Size(size.width, 1f)); y += step }
                }
            },
        content = content,
    )
}

/** Text on the mat: the light greys the guide uses on the dark board. */
object MatInk {
    val text = Color(0xFFC9D1D8)
    val strong = Color(0xFFFFFFFF)
}
