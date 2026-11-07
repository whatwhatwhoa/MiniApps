package io.github.whatwhatwhoa.dailynote.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.github.whatwhatwhoa.dailynote.Jump
import io.github.whatwhatwhoa.dailynote.Note
import io.github.whatwhatwhoa.dailynote.NoteFont
import io.github.whatwhatwhoa.dailynote.PaperArt
import io.github.whatwhatwhoa.dailynote.Prefs
import io.github.whatwhatwhoa.dailynote.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private val Ink = Color(PaperArt.INK)
private val Hand = FontFamily(Font(R.font.patrick_hand))

/**
 * The whole app: one page of paper. Tap a line to write on it; swipe across words to scratch
 * them out (unless that is switched off). The paper is drawn behind a scrolling text field, aligned to the field's first
 * baseline, and a fixed band (the pad's binding) stays on top with the paper button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePage(jump: MutableState<Jump?>) {
    val density = LocalDensity.current
    val d = density.density
    var paper by remember { mutableStateOf(Prefs.paper) }
    var noteSize by remember { mutableStateOf(Prefs.size) }
    var noteFont by remember { mutableStateOf(Prefs.font) }
    var swipeErase by remember { mutableStateOf(Prefs.swipeErase) }
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(Note.text()))
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var focused by remember { mutableStateOf(false) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var undo by remember { mutableStateOf<TextFieldValue?>(null) }
    val eraser = remember { Eraser() }
    val scroll = rememberScrollState()
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val textPx = with(density) { noteSize.sp.sp.toPx() }
    val lineH = PaperArt.lineHeight(textPx)
    // The ink's size from the pixels back to sp, then the pixels Compose will really draw at:
    // with a large system font a bigger sp grows less, so (sp × 1.15) is not textPx × 1.15.
    val inkSp = with(density) { (textPx * noteFont.scale).toSp() }
    val inkPx = with(density) { inkSp.toPx() }
    val bandH = WindowInsets.statusBars.getTop(density) + (36 * d).roundToInt()
    val contentTop = bandH + (lineH * 0.35f).roundToInt()
    val darkBand = PaperArt.darkBand(paper)

    fun change(v: TextFieldValue) {
        val t = tidy(v)
        value = t
        Note.edit(t.text)
    }

    // A tap below the text: blank lines down to the line tapped, the cursor on it.
    fun writeBelow(lines: Int) {
        val t = value.text + "\n".repeat(lines)
        change(TextFieldValue(t, TextRange(t.length)))
        focus.requestFocus()
        keyboard?.show()
    }

    // The swipe is over: scratch across the whole words, fade them, close the gap, then take
    // them out of the text. A swipe over nothing but space just fades its scratch.
    fun finishScratch(cancelled: Boolean) {
        val l = layout
        val before = value
        val e = if (cancelled || l == null) null else findErase(before.text, l, eraser.line, eraser.from, eraser.to)
        scope.launch {
            if (e == null || l == null) {
                animate(1f, 0f, animationSpec = tween(180)) { v, _ -> eraser.ink = v }
                eraser.reset()
                return@launch
            }
            eraser.target = e
            val pad = textPx * 0.15f
            val left = l.getBoundingBox(e.words.start).left - pad
            val right = l.getBoundingBox(e.words.end - 1).right + pad
            val f0 = eraser.from
            val t0 = eraser.to
            val (f1, t1) = if (t0 >= f0) left to right else right to left
            animate(0f, 1f, animationSpec = tween(140)) { v, _ ->
                eraser.from = f0 + (f1 - f0) * v
                eraser.to = t0 + (t1 - t0) * v
            }
            delay(80)
            animate(1f, 0f, animationSpec = tween(220)) { v, _ -> eraser.ink = v }
            animate(0f, 1f, animationSpec = tween(260, easing = FastOutSlowInEasing)) { v, _ -> eraser.collapse = v }
            if (value.text == before.text) {
                val r = e.remove
                fun keep(i: Int) = when {
                    i >= r.end -> i - r.length
                    i > r.start -> r.start
                    else -> i
                }
                val sel = value.selection
                change(TextFieldValue(before.text.removeRange(r.start, r.end), TextRange(keep(sel.start), keep(sel.end))))
                undo = before
            }
            eraser.reset()
        }
    }

    // Light status bar icons over a dark binding, dark ones over a light band.
    val activity = LocalActivity.current
    val view = LocalView.current
    SideEffect {
        activity?.window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !darkBand }
    }

    LaunchedEffect(jump.value) {
        val j = jump.value ?: return@LaunchedEffect
        // The widget counted lines on the saved text, which never ends in blank lines.
        val t = value.text.trimEnd() + "\n".repeat(j.newlines)
        change(TextFieldValue(t, TextRange(if (j.newlines > 0) t.length else j.offset.coerceIn(0, t.length))))
        withFrameNanos { }
        focus.requestFocus()
        keyboard?.show()
        jump.value = null
    }

    // The line height in whole pixels (a hair under, as Compose rounds it up), every line box
    // the same, so the rules drawn from the first baseline stay under every line. In em, not
    // sp: with a large system font Compose scales an sp line height by its own ratio.
    val style = remember(noteFont, inkSp, inkPx, lineH) {
        TextStyle(
            color = Ink,
            fontFamily = if (noteFont == NoteFont.HAND) Hand else FontFamily.Default,
            fontSize = inkSp,
            lineHeight = ((lineH - 0.05f) / inkPx).em,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
    }

    val target = eraser.target
    val inkAlpha = eraser.ink
    val collapse = eraser.collapse
    val transformation = remember(target, inkAlpha, collapse) {
        if (target == null) VisualTransformation.None else eraseTransformation(target, Ink, inkAlpha, collapse)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        // Where the ink starts, in whole pixels: the text field's padding and the scratch agree.
        val inset = PaperArt.textStart(paper, d, width, lineH.toFloat()).roundToInt()

        // The paper: only the visible window is drawn, moved with the scroll.
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val l = layout
                    val base = if (l != null && l.lineCount > 0) l.getLineBaseline(0) else lineH * 0.5f + textPx * 0.38f
                    val first = contentTop - scroll.value + base + PaperArt.ruleGap(textPx)
                    drawIntoCanvas {
                        PaperArt.drawSheet(it.nativeCanvas, paper, d, size.width, 0f, size.height, first, lineH.toFloat())
                    }
                }
        )

        // The ink, scrolling over the paper.
        Box(
            Modifier
                .fillMaxSize()
                .imePadding()
                .pointerInput(contentTop, inset, lineH, swipeErase) {
                    if (!swipeErase) return@pointerInput
                    scratchGesture(
                        lineAt = { p -> lineWithWords(layout, value.text, p, eraser.busy, bandH, contentTop - scroll.value) },
                        onStart = { line, x0, x ->
                            eraser.line = line
                            eraser.from = x0 - inset
                            eraser.to = x - inset
                            eraser.ink = 1f
                        },
                        onMove = { eraser.to = it - inset },
                        onEnd = { finishScratch(it) },
                    )
                }
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewH = constraints.maxHeight
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Spacer(Modifier.height(with(density) { contentTop.toDp() }))
                    BasicTextField(
                        value = value,
                        onValueChange = { change(it) },
                        textStyle = style,
                        cursorBrush = SolidColor(Ink),
                        visualTransformation = transformation,
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus)
                            .onFocusChanged { f ->
                                // Blank lines left at the end by a tap below the text go when writing stops.
                                if (focused && !f.isFocused) {
                                    val t = value.text.trimEnd()
                                    if (t != value.text) change(TextFieldValue(t, TextRange(t.length)))
                                }
                                focused = f.isFocused
                            }
                            .drawWithContent {
                                val l = layout
                                val whole = eraser.target?.wholeLine
                                if (l != null && whole != null && eraser.collapse > 0f) {
                                    // A line left empty: the lines below slide up into its place.
                                    val top = l.getLineTop(l.getLineForOffset(whole.start))
                                    val bottom = l.getLineBottom(l.getLineForOffset(whole.end))
                                    clipRect(bottom = top) { this@drawWithContent.drawContent() }
                                    translate(top = -(bottom - top) * eraser.collapse) {
                                        clipRect(top = bottom) { this@drawWithContent.drawContent() }
                                    }
                                } else {
                                    drawContent()
                                }
                                if (l != null && eraser.busy && eraser.line < l.lineCount) {
                                    drawScratch(
                                        eraser.from, eraser.to, inset.toFloat(), l.getLineBaseline(eraser.line),
                                        textPx, Ink.copy(alpha = eraser.ink),
                                    )
                                }
                            },
                        decorationBox = { inner ->
                            Box(Modifier.padding(start = with(density) { inset.toDp() }, end = 16.dp)) { inner() }
                        },
                    )
                    // The rest of the page, always a few lines past the text: a tap writes there.
                    val textH = layout?.size?.height ?: lineH
                    val filler = max(viewH - contentTop - textH, 0) + 6 * lineH
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(with(density) { filler.toDp() })
                            .pointerInput(lineH) {
                                detectTapGestures { writeBelow(floor(it.y / lineH).toInt() + 1) }
                            }
                    )
                }
            }
        }

        // The band: fixed on top; taps on it go nowhere but the paper button.
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { bandH.toDp() })
                .pointerInput(Unit) { detectTapGestures { } }
                .drawBehind {
                    drawIntoCanvas {
                        PaperArt.drawBand(it.nativeCanvas, paper, d, size.width, size.height, endInset = 56 * d)
                    }
                }
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp, 36.dp)
                    .clickable { picking = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_paper),
                    contentDescription = "Settings",
                    tint = if (darkBand) Color(0xE6FFFFFF) else Color(0x991D2433),
                )
            }
        }
        Box(
            Modifier
                .offset { IntOffset(0, bandH) }
                .fillMaxWidth()
                .height(5.dp)
                .background(Brush.verticalGradient(listOf(Color(0x24000000), Color.Transparent)))
        )

        val saved = undo
        if (saved != null) {
            LaunchedEffect(saved) {
                delay(5000)
                undo = null
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 24.dp),
            ) {
                Row(Modifier.padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Erased", color = MaterialTheme.colorScheme.inverseOnSurface)
                    TextButton(onClick = {
                        change(saved)
                        undo = null
                    }) { Text("Undo", color = MaterialTheme.colorScheme.inversePrimary) }
                }
            }
        }
    }

    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }) {
            PaperSheet(
                paper = paper,
                size = noteSize,
                font = noteFont,
                swipeErase = swipeErase,
                onPaper = {
                    paper = it
                    Prefs.paper = it
                },
                onSize = {
                    noteSize = it
                    Prefs.size = it
                },
                onFont = {
                    noteFont = it
                    Prefs.font = it
                },
                onSwipeErase = {
                    swipeErase = it
                    Prefs.swipeErase = it
                },
            )
        }
    }
}

/**
 * Blank lines at the end clear themselves, so the page doesn't slowly grow: only those down to
 * the cursor stay (a tap below the text puts it there), and they go once it moves up.
 */
private fun tidy(v: TextFieldValue): TextFieldValue {
    val t = v.text
    val keep = max(t.trimEnd().length, v.selection.max)
    if (keep >= t.length) return v
    return TextFieldValue(t.substring(0, keep), v.selection, v.composition?.takeIf { it.max <= keep })
}

/**
 * The visual line under [p] (a point on the screen) when it holds words, else -1. [fieldTop] is
 * where the text field's top is on the screen; nothing under the band counts.
 */
private fun lineWithWords(
    l: TextLayoutResult?, text: String, p: Offset, busy: Boolean, bandH: Int, fieldTop: Int,
): Int {
    if (l == null || busy || p.y < bandH) return -1
    val y = p.y - fieldTop
    if (y < 0 || y >= l.size.height) return -1
    val line = l.getLineForVerticalPosition(y)
    val s = l.getLineStart(line)
    val e = l.getLineEnd(line, visibleEnd = true)
    return if (s < e && text.substring(s, e).isNotBlank()) line else -1
}
