package io.github.whatwhatwhoa.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
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
import io.github.whatwhatwhoa.calculator.CalcModel
import io.github.whatwhatwhoa.stack.BlockGap
import io.github.whatwhatwhoa.stack.SettingsIconButton
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackTabs
import io.github.whatwhatwhoa.stack.ThemeIconButton
import io.github.whatwhatwhoa.stack.stackColumn
import io.github.whatwhatwhoa.update.UpdateBanner

private val tabs = listOf("Calculator", "Functions")

/** Settings is the gear at the right, not a tab (owner); `tab` holds it as the index after the tabs. */
private const val SETTINGS = 2

/**
 * The Stack frame: header (the angle unit as its count), tabs, the update note when there is
 * one, then the tab. The calculator fills the screen; the other tabs scroll.
 */
@Composable
fun MainScreen(model: CalcModel) {
    val c = Stack.colors
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        val wide = maxWidth >= 600.dp
        Column(Modifier.widthIn(max = 1100.dp).stackColumn().fillMaxSize()) {
            // One row: the first tab is the app's name, so no title above it (owner: "very
            // redundant to have calculator, and then calculator below it"); Theme and Settings
            // as icons.
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StackTabs(tabs, if (tab == SETTINGS) -1 else tab, { tab = it }, Modifier.weight(1f),
                    compact = true, brand = true)
                ThemeIconButton()
                SettingsIconButton(tab == SETTINGS, { tab = if (tab == SETTINGS) 0 else SETTINGS })
            }
            UpdateBanner(Modifier.padding(top = 10.dp))
            when (tab) {
                0 -> CalcPage(model, wide, Modifier.weight(1f).padding(top = 10.dp, bottom = 12.dp)) { tab = 1 }
                else -> Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = BlockGap, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(BlockGap),
                    ) {
                        if (tab == 1) FunctionsPage(model) { tab = 0 } else SettingsPage(model)
                    }
                }
            }
        }
    }
}
