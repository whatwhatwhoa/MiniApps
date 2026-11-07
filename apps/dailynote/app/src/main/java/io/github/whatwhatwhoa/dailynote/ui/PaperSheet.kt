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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

private val Hand = FontFamily(Font(R.font.patrick_hand))

/** The only settings there are: paper, size, font, swipe to erase, and how the page works. */
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
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        Text("Paper", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (p in Paper.entries) Swatch(p, p == paper, Modifier.weight(1f)) { onPaper(p) }
        }
        Spacer(Modifier.height(24.dp))
        Text("Size", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            NoteSize.entries.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = s == size,
                    onClick = { onSize(s) },
                    shape = SegmentedButtonDefaults.itemShape(i, NoteSize.entries.size),
                ) { Text(s.label) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Font", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            NoteFont.entries.forEachIndexed { i, f ->
                SegmentedButton(
                    selected = f == font,
                    onClick = { onFont(f) },
                    shape = SegmentedButtonDefaults.itemShape(i, NoteFont.entries.size),
                ) {
                    // Each choice in its own letters.
                    if (f == NoteFont.HAND) Text(f.label, fontFamily = Hand, fontSize = 17.sp) else Text(f.label)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(swipeErase, role = Role.Switch, onValueChange = onSwipeErase),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Swipe to erase", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = swipeErase, onCheckedChange = null)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "• Tap a line to write on it\n" +
                (if (swipeErase) "• Swipe across words to scratch them out\n" else "") +
                "• Widget: hold your home screen → Widgets → Daily Note",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Swatch(p: Paper, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val d = LocalDensity.current.density
    val shape = RoundedCornerShape(6.dp)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(shape).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(shape)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
        ) {
            drawIntoCanvas { PaperArt.drawThumb(it.nativeCanvas, p, d, size.width, size.height) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            p.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.primary else colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
