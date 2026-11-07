package io.github.whatwhatwhoa.dailynote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.dailynote.NoteFont
import io.github.whatwhatwhoa.dailynote.NoteSize
import io.github.whatwhatwhoa.dailynote.Paper
import io.github.whatwhatwhoa.dailynote.PaperArt
import io.github.whatwhatwhoa.dailynote.R
import io.github.whatwhatwhoa.stack.FLabel
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Seg
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.update.UpdateCard

private val Hand = FontFamily(Font(R.font.patrick_hand))

/** The only settings there are: paper, size, font, swipe to erase, how the page works, updates. */
@Composable
fun PaperSheet(
    paper: Paper,
    size: NoteSize,
    font: NoteFont,
    swipeErase: Boolean,
    onPaper: (Paper) -> Unit,
    onSize: (NoteSize) -> Unit,
    onFont: (NoteFont) -> Unit,
    onSwipeErase: (Boolean) -> Unit,
) {
    // Scrolls: with the Updates card it is taller than a landscape phone.
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        FLabel("Paper")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (p in Paper.entries) Swatch(p, p == paper, Modifier.weight(1f)) { onPaper(p) }
        }
        Spacer(Modifier.height(24.dp))
        FLabel("Size")
        Spacer(Modifier.height(8.dp))
        Seg(NoteSize.entries, size, onSize, { it.label })
        Spacer(Modifier.height(24.dp))
        FLabel("Font")
        Spacer(Modifier.height(8.dp))
        // Each choice in its own letters.
        Seg(NoteFont.entries, font, onFont, { it.label }, optionStyle = {
            if (it == NoteFont.HAND) StackType.seg.copy(fontFamily = Hand, fontSize = 17.sp) else StackType.seg
        })
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(swipeErase, role = Role.Switch, onValueChange = onSwipeErase),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Swipe to erase", style = StackType.h2, color = Stack.colors.ink, modifier = Modifier.weight(1f))
            Switch(checked = swipeErase, onCheckedChange = null)
        }
        Spacer(Modifier.height(20.dp))
        Notes(
            listOfNotNull(
                "Tap a line to write on it",
                if (swipeErase) "Swipe across words to scratch them out" else null,
                "Widget: hold your home screen → Widgets → Daily Note",
            )
        )
        Spacer(Modifier.height(20.dp))
        // The series' updates from GitHub (owner, 2026-11-07: every app updates itself).
        UpdateCard()
    }
}

@Composable
private fun Swatch(p: Paper, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val d = LocalDensity.current.density
    val shape = RoundedCornerShape(6.dp)
    val c = Stack.colors
    Column(
        modifier.clip(shape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(shape)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) c.accent else c.line, shape)
        ) {
            drawIntoCanvas { PaperArt.drawThumb(it.nativeCanvas, p, d, size.width, size.height) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            p.label,
            style = StackType.seg.copy(fontSize = 12.5.sp),
            color = if (selected) c.accent else c.ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
