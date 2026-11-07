package io.github.whatwhatwhoa.calculator

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** One line of history: the formula as worked out (brackets closed) and its answer, typed-back form. */
data class Entry(val formula: String, val answer: String, val time: Long)

/**
 * The calculator's state: the formula being typed with its cursor, the history, the user's
 * functions and the angle unit. Everything is kept in one preferences file, so the app opens
 * where it was left.
 */
class CalcModel(context: Context) {
    private val sp = context.getSharedPreferences("calc", Context.MODE_PRIVATE)

    var text by mutableStateOf(sp.getString("text", "") ?: "")
        private set
    var cursor by mutableIntStateOf(sp.getInt("cursor", text.length).coerceIn(0, text.length))
        private set

    /** The text is the last answer: a digit starts a new formula, an operator goes on from it. */
    var fresh by mutableStateOf(sp.getBoolean("fresh", false))
        private set

    /** Why the last = gave no answer; cleared by the next key. */
    var error by mutableStateOf<String?>(null)
        private set

    var degrees by mutableStateOf(sp.getBoolean("degrees", true))
        private set

    var ans: Double = sp.getString("ans", null)?.toDoubleOrNull() ?: 0.0
        private set

    /** Newest first. */
    val history = mutableStateListOf<Entry>().apply { addAll(loadHistory()) }
    val functions = mutableStateListOf<UserFn>().apply { addAll(loadFunctions()) }

    fun env() = Env(degrees, ans, functions.associateBy { it.name })

    // --- Keys

    /** A key's text: a digit, an operator, `sin(`, `π`… Operators replace an operator just typed. */
    fun key(s: String) {
        error = null
        if (fresh) {
            fresh = false
            // After =, an operator goes on from the answer; anything else starts a new formula.
            if (s.first() !in CONTINUES) set("", 0)
            // −5 then ² is (−5)², and 1.5×10^20 then ^2 is (1.5×10^20)^2.
            else if (s.first() in "^²³!%" && (text.startsWith('−') || '×' in text)) set("($text)", text.length + 2)
        }
        val before = text.getOrNull(cursor - 1)
        when {
            s == "." && inNumberWithPoint() -> return
            s in BINARY && before != null && before in BINARY && !(s == "−" && before in "×÷^") -> {
                // 5+ then × makes 5×; but 5× then − is 5×−, a negative number.
                set(text.substring(0, cursor - 1) + s + text.substring(cursor), cursor - 1 + s.length)
                return
            }
            s in "+×÷^" && before == null && text.isEmpty() -> return
        }
        insert(s)
    }

    /** A value from history or a result: wrapped in ( ) when it is negative and would join what is before. */
    fun value(plain: String) {
        error = null
        if (fresh) { fresh = false; set("", 0) }
        val before = text.getOrNull(cursor - 1)
        val joins = before != null && (before.isLetterOrDigit() || before == ')' || before == '.' || before in "²³!%π")
        insert(if (plain.startsWith('−') && joins) "($plain)" else plain)
    }

    fun backspace() {
        error = null
        fresh = false
        if (cursor == 0) return
        val from = stops().last { it < cursor }
        set(text.removeRange(from, cursor), from)
    }

    /** Del: the character (or name) after the cursor. */
    fun delete() {
        error = null
        fresh = false
        if (cursor >= text.length) return
        val to = stops().first { it > cursor }
        set(text.removeRange(cursor, to), cursor)
    }

    fun left() {
        fresh = false
        if (cursor > 0) cursor = stops().last { it < cursor }
        save()
    }

    fun right() {
        fresh = false
        if (cursor < text.length) cursor = stops().first { it > cursor }
        save()
    }

    /** A tap in the formula: the nearest place the cursor can be (never inside a name). */
    fun moveTo(offset: Int) {
        fresh = false
        val o = offset.coerceIn(0, text.length)
        cursor = stops().minBy { kotlin.math.abs(it - o) }
        save()
    }

    fun clearAll() {
        error = null
        fresh = false
        set("", 0)
    }

    /** Work it out: into history, the answer becomes the formula, `ans` is it. */
    fun equals() {
        if (text.isBlank() || fresh) return
        try {
            val v = Calc.eval(text, env())
            val formula = text + ")".repeat(Calc.openBrackets(text))
            history.add(0, Entry(formula, Num.plain(v), System.currentTimeMillis()))
            while (history.size > MAX_HISTORY) history.removeAt(history.lastIndex)
            ans = v
            val shown = Num.plain(v)
            set(shown, shown.length)
            fresh = true
            error = null
            save()
            saveHistory()
        } catch (e: CalcError) {
            error = e.message
        }
    }

    /** Long-press on a history line: its formula back, to change it. */
    fun edit(e: Entry) {
        error = null
        fresh = false
        set(e.formula, e.formula.length)
    }

    fun clearHistory() {
        history.clear()
        saveHistory()
    }

    fun chooseDegrees(on: Boolean) {
        degrees = on
        sp.edit().putBoolean("degrees", on).apply()
    }

    // --- Functions

    /** Saves [fn] in place of [replacing] (by name), or adds it. */
    fun saveFunction(fn: UserFn, replacing: String?) {
        val at = functions.indexOfFirst { it.name == (replacing ?: fn.name) }
        if (at >= 0) functions[at] = fn else functions.add(fn)
        saveFunctions()
    }

    fun deleteFunction(name: String) {
        functions.removeAll { it.name == name }
        saveFunctions()
    }

    /** The functions that use [name], so deleting it can say what stops working. */
    fun usersOf(name: String): List<String> =
        functions.filter { f -> f.name != name && Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])").containsMatchIn(f.body) }
            .map { it.name }

    fun addExamples() {
        for (f in EXAMPLES) if (functions.none { it.name == f.name }) functions.add(f)
        saveFunctions()
    }

    // --- Inside

    private fun insert(s: String) = set(text.substring(0, cursor) + s + text.substring(cursor), cursor + s.length)

    private fun set(t: String, c: Int) {
        text = t
        cursor = c.coerceIn(0, t.length)
        save()
    }

    private fun inNumberWithPoint(): Boolean {
        var j = cursor - 1
        while (j >= 0 && text[j].isDigit()) j--
        var k = cursor
        while (k < text.length && text[k].isDigit()) k++
        return (j >= 0 && text[j] == '.') || (k < text.length && text[k] == '.')
    }

    /** Where the cursor may be: anywhere but inside a name and its "(" (sin( is one key, one step). */
    private fun stops(): List<Int> {
        val out = mutableListOf(0)
        var i = 0
        val t = text
        while (i < t.length) {
            var j = i + 1
            if (t[i].isAsciiName()) {
                while (j < t.length && t[j].isAsciiName()) j++
                // log2( is one step; the 2 of ans2 is a digit of its own.
                var k = j
                while (k < t.length && t[k] in '0'..'9') k++
                if (k < t.length && t[k] == '(') j = k + 1
            }
            out += j
            i = j
        }
        return out
    }

    private fun Char.isAsciiName() = this in 'a'..'z' || this in 'A'..'Z' || this == '_'

    private fun save() {
        sp.edit().putString("text", text).putInt("cursor", cursor).putBoolean("fresh", fresh)
            .putString("ans", ans.toString()).apply()
    }

    private fun saveHistory() {
        val a = JSONArray()
        history.forEach { a.put(JSONObject().put("f", it.formula).put("a", it.answer).put("t", it.time)) }
        sp.edit().putString("history", a.toString()).apply()
    }

    private fun loadHistory(): List<Entry> = runCatching {
        val a = JSONArray(sp.getString("history", "[]"))
        List(a.length()) { a.getJSONObject(it).let { o -> Entry(o.getString("f"), o.getString("a"), o.getLong("t")) } }
    }.getOrDefault(emptyList())

    private fun saveFunctions() {
        val a = JSONArray()
        functions.forEach {
            a.put(JSONObject().put("name", it.name).put("params", JSONArray(it.params)).put("body", it.body))
        }
        sp.edit().putString("functions", a.toString()).apply()
    }

    private fun loadFunctions(): List<UserFn> = runCatching {
        val a = JSONArray(sp.getString("functions", "[]"))
        List(a.length()) {
            val o = a.getJSONObject(it)
            val p = o.getJSONArray("params")
            UserFn(o.getString("name"), List(p.length()) { k -> p.getString(k) }, o.getString("body"))
        }
    }.getOrDefault(emptyList())

    companion object {
        const val MAX_HISTORY = 200
        private const val BINARY = "+−×÷^"
        private const val CONTINUES = "+−×÷^!%²³"

        val EXAMPLES = listOf(
            UserFn("tip", listOf("bill", "pct"), "bill × pct ÷ 100"),
            UserFn("hyp", listOf("a", "b"), "√(a² + b²)"),
            UserFn("ctof", listOf("c"), "c × 9 ÷ 5 + 32"),
            UserFn("ftoc", listOf("f"), "(f − 32) × 5 ÷ 9"),
        )
    }
}
