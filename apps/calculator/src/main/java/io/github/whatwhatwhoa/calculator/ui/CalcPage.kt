package io.github.whatwhatwhoa.calculator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.calculator.Calc
import io.github.whatwhatwhoa.calculator.CalcModel
import io.github.whatwhatwhoa.calculator.Entry
import io.github.whatwhatwhoa.calculator.Num
import io.github.whatwhatwhoa.stack.InstrumentSans
import io.github.whatwhatwhoa.stack.JetBrainsMono
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.stack.TNUM
import kotlinx.coroutines.delay

private val KeyShape = RoundedCornerShape(10.dp)
private val KeyGap = 8.dp
private val StripH = 42.dp

/**
 * The calculator: history above (newest at the bottom), the formula as typed with its live
 * answer, a strip of functions, the keypad. Phones in one column; from 600 dp history and the
 * formula on the left, the keys on the right. A keyboard types into it too.
 */
@Composable
fun CalcPage(model: CalcModel, wide: Boolean, modifier: Modifier, onFunctions: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BoxWithConstraints(modifier.fillMaxWidth().focusRequester(focus).focusable().onKeyEvent { typed(model, it) }) {
        val h = maxHeight
        if (wide) {
            val keyH = ((h - StripH - 10.dp - KeyGap * 5) / 6).coerceIn(36.dp, 72.dp)
            val right = minOf(460.dp, maxWidth * 0.5f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HistoryPane(model, Modifier.weight(1f))
                    Display(model)
                }
                Column(
                    Modifier.width(right).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom),
                ) {
                    Strip(model, onFunctions)
                    Keypad(model, keyH)
                }
            }
        } else {
            val keyH = ((h * 0.5f - KeyGap * 5) / 6).coerceIn(44.dp, 66.dp)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HistoryPane(model, Modifier.weight(1f))
                Display(model)
                Strip(model, onFunctions)
                Keypad(model, keyH)
            }
        }
    }
}

/** A hardware keyboard: * / - for × ÷ −, Enter or = to work it out, Esc to clear. */
private fun typed(m: CalcModel, e: KeyEvent): Boolean {
    if (e.type != KeyEventType.KeyDown || e.isCtrlPressed) return false
    when (e.key) {
        Key.Backspace -> m.backspace()
        Key.Delete -> m.delete()
        Key.DirectionLeft -> m.left()
        Key.DirectionRight -> m.right()
        Key.Enter, Key.NumPadEnter -> m.equals()
        Key.Escape -> m.clearAll()
        else -> {
            val ch = e.utf16CodePoint.toChar()
            val s = when (ch) {
                '*' -> "×"
                '/' -> "÷"
                '-' -> "−"
                '=' -> { m.equals(); return true }
                in '0'..'9', '.', '+', '^', '(', ')', ',', '!', '%', in 'a'..'z', in 'A'..'Z', '_' -> ch.toString()
                else -> return false
            }
            m.key(s)
        }
    }
    return true
}

// --- History

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryPane(m: CalcModel, modifier: Modifier) {
    val c = Stack.colors
    var ask by remember { mutableStateOf(false) }
    if (m.history.isEmpty()) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
            Notes(
                listOf(
                    "Answers collect here, the newest at the bottom.",
                    "Tap one to use its answer; hold one to change its formula.",
                    "Hold the formula to copy its answer.",
                )
            )
        }
    } else {
        LazyColumn(modifier.fillMaxWidth(), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(m.history) { e -> HistoryRow(e, { m.value(e.answer) }, { m.edit(e) }) }
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.End) {
                    StackButton("Clear history", { ask = true }, small = true)
                }
            }
        }
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text("Clear history?") },
            text = { Text("Removes every answer in the list. Your functions stay.") },
            confirmButton = { TextButton({ m.clearHistory(); ask = false }) { Text("Clear") } },
            dismissButton = { TextButton({ ask = false }) { Text("Cancel") } },
            containerColor = c.panel,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(e: Entry, onUse: () -> Unit, onEdit: () -> Unit) {
    val c = Stack.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onUse, onLongClick = onEdit)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(e.formula, style = Mono.copy(fontSize = 14.sp), color = c.muted, textAlign = TextAlign.End)
        Text("= " + Num.group(e.answer), style = Mono.copy(fontSize = 19.sp), color = c.ink, textAlign = TextAlign.End)
    }
}

// --- The formula

@Composable
private fun Display(m: CalcModel) {
    val c = Stack.colors
    val context = LocalContext.current
    val preview by remember(m) { derivedStateOf { if (m.fresh) null else Calc.preview(m.text, m.env()) } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.panel)
            .border(1.dp, c.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Formula(m.text, m.cursor, Calc.openBrackets(m.text), m::moveTo) {
            val v = preview
            copy(context, if (v != null) Num.plain(v) else m.text)
        }
        val v = preview
        val (line, color) = when {
            m.error != null -> m.error!! to c.danger
            v != null && Num.plain(v) != m.text.trim() -> "= ${Num.shown(v)}" to c.muted
            else -> " " to c.muted
        }
        Text(line, Modifier.fillMaxWidth(), style = Mono.copy(fontSize = 20.sp), color = color,
            textAlign = TextAlign.End, maxLines = 2)
    }
}

/**
 * The formula with a blinking cursor; brackets still open show as faint ) at the end (they close
 * themselves). A tap puts the cursor there, a hold copies the answer.
 */
@Composable
private fun Formula(text: String, cursor: Int, open: Int, onTap: (Int) -> Unit, onHold: () -> Unit) {
    val c = Stack.colors
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val blink = rememberInfiniteTransition(label = "cursor")
    val phase = blink.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "blink")
    val fontSize = when {
        text.length <= 12 -> 34.sp
        text.length <= 24 -> 28.sp
        else -> 23.sp
    }
    val shown = buildAnnotatedString {
        append(text)
        if (open > 0) withStyle(SpanStyle(color = c.muted.copy(alpha = 0.5f))) { append(")".repeat(open)) }
    }
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    Text(
        shown,
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { p -> layout?.let { tap(it.getOffsetForPosition(p)) } },
                    onLongPress = { hold() },
                )
            }
            .drawWithContent {
                drawContent()
                val l = layout ?: return@drawWithContent
                if (phase.value < 0.55f) {
                    val r = l.getCursorRect(cursor.coerceIn(0, text.length))
                    val w = 2.dp.toPx()
                    val x = (r.left - w / 2).coerceIn(0f, size.width - w)
                    drawRect(c.accent, Offset(x, r.top), Size(w, r.height))
                }
            }
            .semantics { contentDescription = if (text.isEmpty()) "Formula, empty" else "Formula $text" },
        style = Mono.copy(fontSize = fontSize, lineHeight = fontSize * 1.2f),
        color = c.ink,
        textAlign = TextAlign.End,
        onTextLayout = { layout = it },
    )
}

private fun copy(context: Context, text: String) {
    if (text.isBlank()) return
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Answer", text.replace('−', '-').replace("×10^", "E")))
    // Android 13 and later show their own note when something is copied.
    if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

// --- Function strip and keypad

/** Built-in keys of the strip: label and what each types. */
private val stripKeys = listOf(
    "√" to "√", "x²" to "²", "xʸ" to "^", "!" to "!", "%" to "%", "π" to "π", "e" to "e", "ans" to "ans",
    "sin" to "sin(", "cos" to "cos(", "tan" to "tan(", "asin" to "asin(", "acos" to "acos(", "atan" to "atan(",
    "ln" to "ln(", "log" to "log(", "abs" to "abs(", "min" to "min(", "max" to "max(",
)

@Composable
private fun Strip(m: CalcModel, onFunctions: () -> Unit) {
    LazyRow(Modifier.fillMaxWidth().height(StripH), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Key(if (m.degrees) "Deg" else "Rad", Kind.Edit, Modifier.widthIn(min = 56.dp), StripH,
                description = if (m.degrees) "Angles in degrees; tap for radians" else "Angles in radians; tap for degrees",
                sizeSp = 15) { m.chooseDegrees(!m.degrees) }
        }
        items(m.functions.toList()) { f ->
            Key(f.name, Kind.Mine, Modifier.widthIn(min = 52.dp), StripH, description = "Your function ${f.signature}",
                sizeSp = 16) { m.key(if (f.params.isEmpty()) f.name else f.name + "(") }
        }
        items(stripKeys) { (label, typed) ->
            Key(label, Kind.Fn, Modifier.widthIn(min = 48.dp), StripH, sizeSp = 17) { m.key(typed) }
        }
        item {
            Key("+ Function", Kind.Edit, Modifier.widthIn(min = 96.dp), StripH,
                description = "Make your own function", sizeSp = 14) { onFunctions() }
        }
    }
}

@Composable
private fun Keypad(m: CalcModel, keyH: Dp) {
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        KeyRow {
            Key(null, Kind.Edit, Modifier.weight(1f), keyH, "Move left", Glyph.Left, repeat = true) { m.left() }
            Key(null, Kind.Edit, Modifier.weight(1f), keyH, "Move right", Glyph.Right, repeat = true) { m.right() }
            Key("Del", Kind.Edit, Modifier.weight(1f), keyH, "Delete after the cursor", repeat = true, sizeSp = 16) { m.delete() }
            Key(null, Kind.Edit, Modifier.weight(1f), keyH, "Backspace", Glyph.Back, repeat = true) { m.backspace() }
        }
        KeyRow {
            Key("Clear all", Kind.Edit, Modifier.weight(1f), keyH, "Clear the formula", sizeSp = 15) { m.clearAll() }
            Key("(", Kind.Op, Modifier.weight(1f), keyH) { m.key("(") }
            Key(")", Kind.Op, Modifier.weight(1f), keyH) { m.key(")") }
            Key("÷", Kind.Op, Modifier.weight(1f), keyH, "Divide") { m.key("÷") }
        }
        DigitRow(m, keyH, "7", "8", "9", "×", "Times")
        DigitRow(m, keyH, "4", "5", "6", "−", "Minus")
        DigitRow(m, keyH, "1", "2", "3", "+", "Plus")
        KeyRow {
            Key("0", Kind.Digit, Modifier.weight(1f), keyH) { m.key("0") }
            Key(".", Kind.Digit, Modifier.weight(1f), keyH, "Point") { m.key(".") }
            Key(",", Kind.Op, Modifier.weight(1f), keyH, "Comma, between a function's values") { m.key(",") }
            Key("=", Kind.Equals, Modifier.weight(1f), keyH, "Equals") { m.equals() }
        }
    }
}

@Composable
private fun DigitRow(m: CalcModel, keyH: Dp, a: String, b: String, d: String, op: String, opName: String) {
    KeyRow {
        for (k in listOf(a, b, d)) Key(k, Kind.Digit, Modifier.weight(1f), keyH) { m.key(k) }
        Key(op, Kind.Op, Modifier.weight(1f), keyH, opName) { m.key(op) }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KeyGap), content = content)
}

private enum class Kind { Digit, Op, Edit, Equals, Fn, Mine }
private enum class Glyph { Left, Right, Back }

private val Mono = TextStyle(fontFamily = JetBrainsMono, fontFeatureSettings = TNUM)

/**
 * One key. [repeat]: held down, it goes again every 70 ms after a short wait (⌫, Del, the
 * arrows). The label shrinks to fit, so "Clear all" stays on one line at a large font size.
 */
@Composable
private fun Key(
    label: String?,
    kind: Kind,
    modifier: Modifier,
    height: Dp,
    description: String? = null,
    glyph: Glyph? = null,
    repeat: Boolean = false,
    sizeSp: Int = 26,
    onClick: () -> Unit,
) {
    val c = Stack.colors
    val view = LocalView.current
    val bg = when (kind) {
        Kind.Digit, Kind.Edit, Kind.Fn -> c.panel
        Kind.Op -> c.grid
        Kind.Equals -> c.accent
        Kind.Mine -> c.accentSoft
    }
    val fg = when (kind) {
        Kind.Equals -> c.accentInk
        Kind.Edit -> c.muted
        else -> c.ink
    }
    val press = remember { MutableInteractionSource() }
    val tap by rememberUpdatedState(onClick)
    var repeated by remember { mutableStateOf(false) }
    if (repeat) {
        val pressed by press.collectIsPressedAsState()
        LaunchedEffect(pressed) {
            if (!pressed) return@LaunchedEffect
            repeated = false
            delay(450)
            while (true) {
                repeated = true
                tap()
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                delay(70)
            }
        }
    }
    Box(
        modifier
            .height(height)
            .clip(KeyShape)
            .background(bg)
            .border(1.dp, if (kind == Kind.Equals) c.accent else c.line, KeyShape)
            .clickable(interactionSource = press, indication = LocalIndication.current, role = Role.Button) {
                // A key that repeated while held has done its work; the release adds nothing.
                if (repeated) repeated = false else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    tap()
                }
            }
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) KeyGlyph(glyph, fg)
        else if (label != null) {
            val font = if (kind == Kind.Edit) InstrumentSans else JetBrainsMono
            BasicText(
                label,
                style = TextStyle(fontFamily = font, fontSize = sizeSp.sp, color = fg, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = sizeSp.sp, stepSize = 1.sp),
            )
        }
    }
}

/** ← → and ⌫ drawn, since the Stack fonts are cut to Latin. */
@Composable
private fun KeyGlyph(g: Glyph, color: Color) {
    Canvas(Modifier.size(width = 26.dp, height = 20.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (g) {
            Glyph.Left, Glyph.Right -> {
                val p = Path()
                val (a, b) = if (g == Glyph.Left) 0.62f to 0.38f else 0.38f to 0.62f
                p.moveTo(w * a, h * 0.15f); p.lineTo(w * b, h * 0.5f); p.lineTo(w * a, h * 0.85f)
                drawPath(p, color, style = stroke)
            }
            Glyph.Back -> {
                val p = Path()
                p.moveTo(w * 0.04f, h * 0.5f); p.lineTo(w * 0.32f, h * 0.08f); p.lineTo(w * 0.96f, h * 0.08f)
                p.lineTo(w * 0.96f, h * 0.92f); p.lineTo(w * 0.32f, h * 0.92f); p.close()
                drawPath(p, color, style = stroke)
                drawLine(color, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.76f, h * 0.68f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(w * 0.76f, h * 0.32f), Offset(w * 0.5f, h * 0.68f), stroke.width, StrokeCap.Round)
            }
        }
    }
}
