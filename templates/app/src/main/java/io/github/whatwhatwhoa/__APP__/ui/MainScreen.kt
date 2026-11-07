package io.github.whatwhatwhoa.__APP__.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.stack.BlockGap
import io.github.whatwhatwhoa.stack.Mat
import io.github.whatwhatwhoa.stack.MatInk
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.SettingsPage
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackTopBar
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.stack.Tile
import io.github.whatwhatwhoa.stack.TileGrid
import io.github.whatwhatwhoa.stack.stackColumn

/**
 * The series' frame (series rule 3): `StackTopBar` on top (the name, the app's tabs if any,
 * Theme, the Settings gear), then the page in the Stack order (docs/stack-android.md §5): the
 * picture on the mat, four tiles, details, notes. Settings holds How it works, About and
 * Updates. Replace each block with the app's own; keep the frame and the order.
 */
@Composable
fun MainScreen() {
    val c = Stack.colors
    var page by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val columns = if (maxWidth >= 900.dp) 4 else 2
        Column(Modifier.stackColumn().fillMaxSize()) {
            StackTopBar("__NAME__", page, { page = it })
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(top = BlockGap, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(BlockGap),
            ) {
                if (page == SettingsPage) {
                    StackCard(title = "How it works") { Notes(listOf("One line per point.")) }
                } else {
                    Mat(Modifier.fillMaxWidth().height(160.dp)) {
                        Text("__NAME__ starts here.", Modifier.align(Alignment.Center), style = StackType.body,
                            color = MatInk.text)
                    }
                    TileGrid(columns, List(4) { i ->
                        { m: Modifier -> Tile("Tile ${i + 1}", "0", m, info = "What this tile measures.", sub = "Context line") }
                    })
                }
            }
        }
    }
}
