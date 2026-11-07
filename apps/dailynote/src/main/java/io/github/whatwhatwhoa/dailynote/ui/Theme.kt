package io.github.whatwhatwhoa.dailynote.ui

import androidx.compose.runtime.Composable
import io.github.whatwhatwhoa.stack.StackTheme

/**
 * The series' Stack look for the paper sheet and the undo pill; the page is the paper's own.
 * The bars stay as MainActivity sets them: paper is light whatever the system theme.
 */
@Composable
fun NoteTheme(content: @Composable () -> Unit) {
    StackTheme(systemBars = false, content = content)
}
