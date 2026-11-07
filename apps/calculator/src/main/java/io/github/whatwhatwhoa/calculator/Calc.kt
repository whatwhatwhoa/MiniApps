package io.github.whatwhatwhoa.calculator

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/** The formula can't be read: a typo, an unknown name, a wrong number of values. */
open class CalcError(message: String) : Exception(message)

/** The formula reads fine but has no real answer: ÷ 0, √ of a negative, too large. */
class MathError(message: String) : CalcError(message)

/** A function the user made: `name(params) = body`. Zero params makes it a named value. */
data class UserFn(val name: String, val params: List<String>, val body: String) {
    val signature: String get() = if (params.isEmpty()) name else "$name(${params.joinToString(", ")})"
}

/** What a formula can see: the angle unit, the last answer, the user's functions, and arguments. */
class Env(
    val degrees: Boolean,
    val ans: Double,
    val functions: Map<String, UserFn>,
    val vars: Map<String, Double> = emptyMap(),
    val depth: Int = 0,
)

/**
 * Formulas as they are typed on the keypad or a keyboard: `2(3+4)²`, `√2π`, `sin(30)`, `200 + 10%`.
 * - Both the keypad's signs (× ÷ −) and a keyboard's (* / -) work.
 * - Multiplying needs no sign next to a bracket, a name or π: `2π`, `3sin(30)`, `(1+2)(3+4)`.
 * - Brackets left open at the end are closed (the display shows them faint).
 * - `a + b%` and `a − b%` add or take b percent of a; anywhere else b% is b ÷ 100.
 * - `^` is a power, right to left (`2^3^2` = 2^9); −2^2 is −4, as in math.
 * - `E` is the exponent of a number (`1.5E3`), `e` Euler's number.
 */
object Calc {
    /** Built-in functions and how many values each takes (-1: one or more). */
    val builtins: Map<String, Int> = mapOf(
        "sin" to 1, "cos" to 1, "tan" to 1, "asin" to 1, "acos" to 1, "atan" to 1,
        "sinh" to 1, "cosh" to 1, "tanh" to 1, "ln" to 1, "log" to 1, "log2" to 1, "exp" to 1,
        "sqrt" to 1, "cbrt" to 1, "abs" to 1, "round" to 1, "floor" to 1, "ceil" to 1,
        "min" to -1, "max" to -1, "mod" to 2, "nCr" to 2, "nPr" to 2,
    )

    /** Names a function can't take: the built-ins and the constants. */
    val reserved: Set<String> = builtins.keys + setOf("pi", "e", "ans", "E")

    fun eval(text: String, env: Env): Double {
        val v = Parser(text, env).parse()
        return when {
            v.isNaN() -> throw MathError("No real answer")
            v.isInfinite() -> throw MathError("Too large")
            else -> v
        }
    }

    /** The result, or null while the formula is unfinished or has no answer (for the live preview). */
    fun preview(text: String, env: Env): Double? = try {
        if (text.isBlank()) null else eval(text, env)
    } catch (_: CalcError) {
        null
    }

    /**
     * Checks a function before it is saved: it must read, and every name in it must be known. A
     * math error with the trial values (1 for each parameter) is fine: `1 ÷ (x − 1)` is a good function.
     */
    fun check(fn: UserFn, functions: Map<String, UserFn>, degrees: Boolean): String? {
        val all = functions + (fn.name to fn)
        return try {
            Parser(fn.body, Env(degrees, 0.0, all, fn.params.associateWith { 1.0 }, 1)).parse()
            null
        } catch (e: MathError) {
            null
        } catch (e: CalcError) {
            e.message
        }
    }

    /** How many brackets are open at the end: the display draws that many faint `)`. */
    fun openBrackets(text: String): Int {
        var n = 0
        for (ch in text) when (ch) {
            '(' -> n++
            ')' -> if (n > 0) n--
        }
        return n
    }
}

private class Parser(private val s: String, private val env: Env) {
    private var i = 0

    fun parse(): Double {
        if (env.depth > 40) throw CalcError("Too deep: does a function use itself?")
        val v = expr()
        skip()
        if (i < s.length) {
            throw CalcError(if (s[i] == ')') "A ) without its (" else "Can't read \"${s[i]}\" here")
        }
        return v
    }

    private fun skip() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun peek(): Char? {
        skip()
        return s.getOrNull(i)
    }

    private fun take(c: Char): Boolean {
        if (peek() == c) { i++; return true }
        return false
    }

    private fun isMinus(c: Char?) = c == '-' || c == '−'

    // expr := term (± term)*   with a + b% = a(1 + b/100)
    private fun expr(): Double {
        var v = term().first
        while (true) {
            val c = peek()
            val sign = when {
                c == '+' -> 1.0
                isMinus(c) -> -1.0
                else -> return v
            }
            i++
            if (peek() == null) throw CalcError("Something is missing after ${if (sign > 0) "+" else "−"}")
            val (r, percent) = term()
            v = if (percent) v + sign * v * r else v + sign * r
        }
    }

    // term := unary ((× | ÷ | implicit) unary)*; the flag says the term was just "n%".
    private fun term(): Pair<Double, Boolean> {
        var (v, percent) = unary()
        while (true) {
            val c = peek()
            when {
                c == '×' || c == '*' -> {
                    i++
                    v *= operand("×")
                }
                c == '÷' || c == '/' -> {
                    i++
                    val d = operand("÷")
                    if (d == 0.0) throw MathError("Can't divide by zero")
                    v /= d
                }
                startsOperand(c) -> v *= unary().first
                else -> return v to percent
            }
            percent = false
        }
    }

    private fun operand(op: String): Double {
        if (peek() == null) throw CalcError("Something is missing after $op")
        return unary().first
    }

    /** A primary can start here, so two side by side multiply: `2π`, `3(4)`, `2sin(30)`. */
    private fun startsOperand(c: Char?): Boolean =
        c != null && (c.isDigit() || c == '.' || c == '(' || c == 'π' || c == '√' || c == '∛' || isNameStart(c))

    // unary := (− | +) unary | power
    private fun unary(): Pair<Double, Boolean> {
        val c = peek()
        if (isMinus(c)) {
            i++
            val (v, p) = unary()
            return -v to p
        }
        if (c == '+') {
            i++
            return unary()
        }
        return power()
    }

    // power := postfix (^ unary)?  — right to left, and the exponent may be negative.
    private fun power(): Pair<Double, Boolean> {
        val (base, percent) = postfix()
        if (take('^')) {
            if (peek() == null) throw CalcError("Something is missing after ^")
            val e = unary().first
            return pow(base, e) to false
        }
        return base to percent
    }

    private fun pow(b: Double, e: Double): Double {
        if (b == 0.0 && e < 0) throw MathError("Can't divide by zero")
        if (b < 0 && e != floor(e)) {
            // An odd root of a negative is real: (−8)^(1/3) = −2.
            val inv = 1 / e
            if (abs(inv - round(inv)) < 1e-9 && round(inv).toLong() % 2 != 0L) return -(-b).pow(e)
            throw MathError("No real answer")
        }
        return b.pow(e)
    }

    // postfix := primary (! | % | ² | ³)*
    private fun postfix(): Pair<Double, Boolean> {
        var v = primary()
        var percent = false
        while (true) {
            when (peek()) {
                '!' -> { i++; v = factorial(v); percent = false }
                '%' -> { i++; v /= 100; percent = true }
                '²' -> { i++; v *= v; percent = false }
                '³' -> { i++; v = v * v * v; percent = false }
                else -> return v to percent
            }
        }
    }

    private fun primary(): Double {
        val c = peek() ?: throw CalcError("Something is missing at the end")
        return when {
            c.isDigit() || c == '.' -> number()
            c == '(' -> {
                i++
                if (peek() == ')') throw CalcError("Empty ( )")
                val v = expr()
                // A bracket left open at the end closes itself.
                if (!take(')') && peek() != null) throw CalcError("Missing )")
                v
            }
            c == 'π' -> { i++; Math.PI }
            c == '√' -> { i++; root(postfixValue(), 2) }
            c == '∛' -> { i++; root(postfixValue(), 3) }
            isNameStart(c) -> name()
            c == ')' -> throw CalcError("A ) without its (")
            else -> throw CalcError("Can't read \"$c\" here")
        }
    }

    /** √ takes the value right after it, with its sign and powers: √9² = √81, √−4 has no answer. */
    private fun postfixValue(): Double {
        if (peek() == null) throw CalcError("Something is missing after √")
        return unary().first
    }

    private fun known(n: String) = n in Calc.builtins || n in env.functions || n in env.vars

    /** Names are ASCII (π is a letter to Kotlin but a constant here, so 2rπ is 2·r·π). */
    private fun isNameStart(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'

    /** x mod m for doubles, always from 0 up to m. */
    private fun fmod(x: Double, m: Double) = x - m * floor(x / m)

    private fun root(v: Double, n: Int): Double = when {
        n == 3 -> cbrt(v)
        v < 0 -> throw MathError("No real square root of a negative")
        else -> sqrt(v)
    }

    private fun number(): Double {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        val digits = s.substring(start, i)
        if (digits.count { it == '.' } > 1) throw CalcError("Two points in one number")
        if (digits == ".") throw CalcError("A point without digits")
        var text = digits
        // E3, E−3: the exponent of this number, only when a digit follows.
        if (i < s.length && s[i] == 'E') {
            var j = i + 1
            val neg = j < s.length && isMinus(s[j])
            if (neg || (j < s.length && s[j] == '+')) j++
            if (j < s.length && s[j].isDigit()) {
                val e0 = j
                while (j < s.length && s[j].isDigit()) j++
                text += "E" + (if (neg) "-" else "") + s.substring(e0, j)
                i = j
            }
        }
        return text.toDouble()
    }

    private fun name(): Double {
        val start = i
        while (i < s.length && isNameStart(s[i])) i++
        // Digits belong to the name only when that makes a known one (log2, f2); else ans2 is ans × 2.
        var k = i
        while (k < s.length && (isNameStart(s[k]) || s[k] in '0'..'9')) k++
        if (k > i && known(s.substring(start, k))) i = k
        val n = s.substring(start, i)
        val call = peek() == '('
        if (!call) {
            env.vars[n]?.let { return it }
            when (n) {
                "pi" -> return Math.PI
                "e" -> return Math.E
                "ans" -> return env.ans
            }
            env.functions[n]?.let { f ->
                if (f.params.isEmpty()) return user(f, emptyList())
                throw CalcError("${f.name} needs ${f.params.size} value${if (f.params.size == 1) "" else "s"} in ( )")
            }
            if (n in Calc.builtins) throw CalcError("$n needs ( )")
            throw CalcError("Unknown name: $n")
        }
        i++ // (
        val args = args(n)
        env.functions[n]?.let { return user(it, args) }
        val want = Calc.builtins[n] ?: throw CalcError("Unknown function: $n")
        if (want >= 0 && args.size != want) {
            throw CalcError("$n takes $want value${if (want == 1) "" else "s"}, not ${args.size}")
        }
        if (want < 0 && args.isEmpty()) throw CalcError("$n needs a value")
        return builtin(n, args)
    }

    /** Values in a call, up to its ) or the end (a call left open at the end closes itself). */
    private fun args(n: String): List<Double> {
        if (take(')')) return emptyList()
        if (peek() == null) throw CalcError("$n( needs a value")
        val out = mutableListOf(expr())
        while (take(',')) {
            if (peek() == null || peek() == ')') throw CalcError("A value is missing after ,")
            out += expr()
        }
        if (!take(')') && peek() != null) throw CalcError("Missing ) after $n(")
        return out
    }

    private fun user(f: UserFn, args: List<Double>): Double {
        if (args.size != f.params.size) {
            throw CalcError("${f.name} takes ${f.params.size} value${if (f.params.size == 1) "" else "s"}, not ${args.size}")
        }
        val inner = Env(env.degrees, env.ans, env.functions, f.params.zip(args).toMap(), env.depth + 1)
        return try {
            Parser(f.body, inner).parse()
        } catch (e: MathError) {
            throw e
        } catch (e: CalcError) {
            throw CalcError(if (e.message?.startsWith("In ") == true) e.message!! else "In ${f.name}: ${e.message}")
        }
    }

    private fun builtin(n: String, a: List<Double>): Double {
        val x = a.firstOrNull() ?: 0.0
        return when (n) {
            "sin" -> trig(x) { sin(it) }
            "cos" -> trig(x) { cos(it) }
            "tan" -> {
                if (env.degrees && fmod(x, 180.0) == 90.0) throw MathError("tan(${plainDeg(x)}°) has no value")
                trig(x) { tan(it) }
            }
            "asin" -> inverse(x) { asin(it) }
            "acos" -> inverse(x) { acos(it) }
            "atan" -> fromRad(atan(x))
            "sinh" -> sinh(x)
            "cosh" -> cosh(x)
            "tanh" -> tanh(x)
            "ln" -> if (x <= 0) throw MathError("ln needs a value above 0") else ln(x)
            "log" -> if (x <= 0) throw MathError("log needs a value above 0") else log10(x)
            "log2" -> if (x <= 0) throw MathError("log2 needs a value above 0") else log2(x)
            "exp" -> exp(x)
            "sqrt" -> root(x, 2)
            "cbrt" -> cbrt(x)
            "abs" -> abs(x)
            "round" -> round(x)
            "floor" -> floor(x)
            "ceil" -> ceil(x)
            "min" -> a.min()
            "max" -> a.max()
            "mod" -> if (a[1] == 0.0) throw MathError("mod by zero") else a[0] - a[1] * floor(a[0] / a[1])
            "nCr", "nPr" -> choose(n, a[0], a[1])
            else -> throw CalcError("Unknown function: $n")
        }
    }

    private fun plainDeg(x: Double) = if (x == floor(x)) x.toLong().toString() else x.toString()

    /** In degrees, multiples of 90° come out exact: sin(180) is 0, not 1.2×10^−16. */
    private fun trig(x: Double, f: (Double) -> Double): Double {
        val r = if (env.degrees) {
            val m = fmod(x, 360.0)
            if (m % 90.0 == 0.0) Math.toRadians(m) else Math.toRadians(x)
        } else x
        val v = f(r)
        return if (abs(v) < 1e-15) 0.0 else if (abs(v - round(v)) < 1e-15) round(v) else v
    }

    private fun inverse(x: Double, f: (Double) -> Double): Double {
        if (x < -1 || x > 1) throw MathError("Needs a value from −1 to 1")
        return fromRad(f(x))
    }

    private fun fromRad(r: Double) = if (env.degrees) Math.toDegrees(r) else r

    private fun factorial(v: Double): Double {
        if (v < 0 || v != floor(v)) throw MathError("! needs a whole number, 0 or more")
        if (v > 170) throw MathError("Too large")
        var r = 1.0
        for (k in 2..v.toInt()) r *= k
        return r
    }

    private fun choose(n: String, a: Double, b: Double): Double {
        if (a != floor(a) || b != floor(b) || a < 0 || b < 0) throw MathError("$n needs whole numbers, 0 or more")
        if (b > a) return 0.0
        var r = 1.0
        val k = if (n == "nCr") minOf(b, a - b).toInt() else b.toInt()
        for (j in 0 until k) {
            r *= (a - j)
            if (n == "nCr") r /= (j + 1)
        }
        return round(r)
    }
}

/** Numbers as the calculator shows them: 12 significant digits, so 0.1 + 0.2 is 0.3. */
object Num {
    private val ctx = MathContext(12)

    /** To type back into a formula: no grouping, the keypad's minus, a power of ten as ×10^. */
    fun plain(x: Double): String {
        if (x == 0.0) return "0"
        val bd = BigDecimal(x).round(ctx)
        val a = abs(x)
        val text = if (a >= 1e15 || a < 1e-9) {
            val exp = bd.precision() - bd.scale() - 1
            val mant = bd.movePointLeft(exp).stripTrailingZeros().toPlainString()
            "$mant×10^$exp"
        } else bd.stripTrailingZeros().toPlainString()
        return text.replace('-', '−')
    }

    /** To read: as [plain], with the whole part grouped by thousands (1,234,567.5). */
    fun shown(x: Double): String = group(plain(x))

    /** Groups a [plain] number by thousands; a power of ten stays as it is. */
    fun group(p: String): String {
        if ('×' in p || p.any { it != '−' && it != '.' && !it.isDigit() }) return p
        val neg = p.startsWith('−')
        val body = if (neg) p.substring(1) else p
        val whole = body.substringBefore('.')
        val frac = body.substringAfter('.', "")
        val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
        return (if (neg) "−" else "") + grouped + (if (frac.isNotEmpty()) ".$frac" else "")
    }
}
