package io.github.whatwhatwhoa.scan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.whatwhatwhoa.scan.ScanModel
import io.github.whatwhatwhoa.stack.BlockGap
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackTabs
import io.github.whatwhatwhoa.stack.ThemeIconButton
import io.github.whatwhatwhoa.stack.stackColumn
import io.github.whatwhatwhoa.update.UpdateBanner

private val tabs = listOf("Scan", "Settings")

/**
 * The Stack frame: the tabs as the header (the first is the app's name, its count the pages),
 * the Theme icon, the update note when there is one, then the tab. Scan fills the screen, so
 * the mat gets every spare dp; Settings scrolls.
 */
@Composable
fun MainScreen(model: ScanModel) {
    val c = Stack.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val wide = maxWidth >= 600.dp
        Column(Modifier.stackColumn().fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StackTabs(tabs, tab, { tab = it }, Modifier.weight(1f), counts = listOf("${model.pages.size}", null),
                    compact = true)
                ThemeIconButton()
            }
            UpdateBanner(Modifier.padding(top = 10.dp))
            when (tab) {
                0 -> ScanPage(model, wide, Modifier.weight(1f).padding(top = 10.dp, bottom = 12.dp))
                else -> Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = BlockGap, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(BlockGap),
                    ) { SettingsPage(model) }
                }
            }
        }
    }
}
