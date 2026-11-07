package io.github.whatwhatwhoa.calculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.whatwhatwhoa.calculator.Calc
import io.github.whatwhatwhoa.calculator.CalcError
import io.github.whatwhatwhoa.calculator.CalcModel
import io.github.whatwhatwhoa.calculator.Env
import io.github.whatwhatwhoa.calculator.Num
import io.github.whatwhatwhoa.calculator.UserFn
import io.github.whatwhatwhoa.stack.FLabel
import io.github.whatwhatwhoa.stack.JetBrainsMono
import io.github.whatwhatwhoa.stack.Notes
import io.github.whatwhatwhoa.stack.Stack
import io.github.whatwhatwhoa.stack.StackButton
import io.github.whatwhatwhoa.stack.StackCard
import io.github.whatwhatwhoa.stack.StackField
import io.github.whatwhatwhoa.stack.StackType
import io.github.whatwhatwhoa.stack.TNUM

/** A function being made or changed; [original] is its name before, null for a new one. */
private data class Draft(val original: String?, val name: String, val params: String, val body: String)

private val Mono = TextStyle(fontFamily = JetBrainsMono, fontFeatureSettings = TNUM, fontSize = 15.sp)

/** The user's functions: the list (use, edit, delete), or the editor for one. */
@Composable
fun FunctionsPage(m: CalcModel, onUse: () -> Unit) {
    var editing by remember { mutableStateOf<Draft?>(null) }
    var deleting by remember { mutableStateOf<UserFn?>(null) }
    val d = editing
    if (d != null) {
        Editor(m, d) { editing = null }
        Help()
        return
    }
    StackCard(
        title = "Your functions",
        info = "Your functions are in the strip above the keys, first, in the accent color.",
        action = { StackButton("New function", { editing = Draft(null, "", "", "") }, primary = true, small = true) },
    ) {
        if (m.functions.isEmpty()) {
            Notes(
                listOf(
                    "Make a function once and use it like sin: tip(80, 15) is 12.",
                    "A function has a name, the values it takes (none to six) and a formula of them.",
                    "It may use the built-in functions and your other functions.",
                )
            )
            StackButton("Add four examples", { m.addExamples() })
        } else {
            m.functions.forEachIndexed { i, f ->
                if (i > 0) HorizontalDivider(color = Stack.colors.line)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${f.signature} = ${f.body}", style = Mono, color = Stack.colors.ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StackButton("Use", {
                            m.key(if (f.params.isEmpty()) f.name else f.name + "(")
                            onUse()
                        }, small = true)
                        StackButton("Edit", { editing = Draft(f.name, f.name, f.params.joinToString(", "), f.body) }, small = true)
                        StackButton("Delete", { deleting = f }, small = true)
                    }
                }
            }
        }
    }
    Help()

    deleting?.let { f ->
        val users = m.usersOf(f.name)
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete ${f.name}?") },
            text = {
                Text(
                    if (users.isEmpty()) "${f.signature} = ${f.body}"
                    else "${users.joinToString(", ")} use${if (users.size == 1) "s" else ""} it and will stop working."
                )
            },
            confirmButton = { TextButton({ m.deleteFunction(f.name); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } },
            containerColor = Stack.colors.panel,
        )
    }
}

@Composable
private fun Help() {
    StackCard(title = "Writing a formula") {
        Notes(
            listOf(
                "Type * / - for × ÷ −, and ^ for a power: x^2.",
                "sqrt(x) is √x, pi is π, e is Euler's number.",
                "Built in: sin cos tan asin acos atan sinh cosh tanh ln log log2 exp sqrt cbrt abs round floor ceil min max mod nCr nPr.",
                "Angles follow Deg / Rad, as on the keys.",
                "No values makes a named number: g = 9.80665, then 2g is 19.6133.",
            )
        )
    }
}

@Composable
private fun Editor(m: CalcModel, start: Draft, onDone: () -> Unit) {
    val c = Stack.colors
    var name by remember { mutableStateOf(start.name) }
    var params by remember { mutableStateOf(start.params) }
    var body by remember { mutableStateOf(start.body) }
    var trial by remember { mutableStateOf("") }

    val list = params.split(',', ' ').map { it.trim() }.filter { it.isNotEmpty() }
    val others = m.functions.filter { it.name != start.original }
    val fn = UserFn(name.trim(), list, tidy(body))
    val problem = problem(fn, others, start.original, m.degrees, body.isBlank())
    val tried: Pair<String, Boolean>? = if (problem != null || trial.isBlank() && list.isNotEmpty()) null else try {
        val env = Env(m.degrees, m.ans, (others + fn).associateBy { it.name })
        val call = if (list.isEmpty()) fn.name else "${fn.name}($trial)"
        "$call = ${Num.shown(Calc.eval(call, env))}" to true
    } catch (e: CalcError) {
        (e.message ?: "No answer") to false
    }

    StackCard(title = if (start.original == null) "New function" else "Change ${start.original}") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FLabel("Name")
            StackField(name, { name = it }, Modifier.fillMaxWidth(), placeholder = "tip", keyboardType = KeyboardType.Ascii)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FLabel("Values it takes")
            StackField(params, { params = it }, Modifier.fillMaxWidth(), placeholder = "bill, pct", keyboardType = KeyboardType.Ascii)
            Text("Names with commas between; none makes a named number.", style = StackType.sub, color = c.muted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FLabel("Formula")
            StackField(body, { body = it }, Modifier.fillMaxWidth(), placeholder = "bill * pct / 100", keyboardType = KeyboardType.Ascii)
        }
        if (fn.name.isNotEmpty() && body.isNotBlank()) Text("${fn.signature} = ${fn.body}", style = Mono, color = c.ink)
        Text(problem ?: "Reads fine.", style = StackType.note, color = if (problem != null) c.danger else c.good)
        if (problem == null && list.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FLabel("Try it")
                StackField(trial, { trial = it }, Modifier.fillMaxWidth(),
                    placeholder = List(list.size) { listOf("80", "15", "2", "3", "4", "5")[it] }.joinToString(", "))
            }
        }
        tried?.let { (text, ok) -> Text(text, style = Mono, color = if (ok) c.ink else c.danger) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StackButton("Save", {
                m.saveFunction(fn, start.original)
                onDone()
            }, primary = true, enabled = problem == null)
            StackButton("Cancel", onDone)
        }
    }
}

/** The keyboard's * / - shown as the keypad's × ÷ −, so a function reads like the formula line. */
private fun tidy(body: String) = body.trim().replace('*', '×').replace('/', '÷').replace('-', '−')

private val NAME = Regex("[A-Za-z_][A-Za-z0-9_]{0,15}")

/** Why [fn] can't be saved yet, or null. */
private fun problem(fn: UserFn, others: List<UserFn>, original: String?, degrees: Boolean, noBody: Boolean): String? {
    val n = fn.name
    if (n.isEmpty()) return "Give it a name."
    if (!NAME.matches(n)) return "A name is a letter, then letters or digits (16 at most)."
    if (n in Calc.reserved) return "$n is built in; pick another name."
    if (n != original && others.any { it.name == n }) return "You have a $n already."
    if (fn.params.size > 6) return "Six values at most."
    for (p in fn.params) {
        if (!NAME.matches(p)) return "\"$p\" can't be a value's name: a letter, then letters or digits."
        if (p in Calc.reserved) return "$p is built in; call the value something else."
        if (p == n) return "A value can't have the function's name."
    }
    if (fn.params.toSet().size != fn.params.size) return "Two values have the same name."
    if (noBody) return "Write its formula."
    return Calc.check(fn, others.associateBy { it.name }, degrees)
}
