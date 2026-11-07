package io.github.whatwhatwhoa.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The formula engine: what each rule in Settings → How it reads a formula promises. */
class CalcTest {
    private val fns = CalcModel.EXAMPLES.associateBy { it.name } +
        ("g" to UserFn("g", emptyList(), "9.80665")) +
        ("f2" to UserFn("f2", listOf("x"), "x × 2"))
    private val deg = Env(degrees = true, ans = 7.0, functions = fns)
    private val rad = Env(degrees = false, ans = 7.0, functions = fns)

    private fun shown(s: String, env: Env = deg) = Num.shown(Calc.eval(s, env))

    private fun fails(s: String, env: Env = deg): String {
        try {
            Calc.eval(s, env)
        } catch (e: CalcError) {
            return e.message ?: ""
        }
        fail("no error for $s")
        return ""
    }

    @Test fun basics() {
        assertEquals("7", shown("1+2×3"))
        assertEquals("7", shown("1 + 2 * 3"))
        assertEquals("2.5", shown("5÷2"))
        assertEquals("−1", shown("2−3"))
        assertEquals("−1", shown("2-3"))
        assertEquals("0.3", shown("0.1+0.2"))
        assertEquals("0.5", shown(".5"))
    }

    @Test fun signsAndPowers() {
        assertEquals("−4", shown("−2^2"))
        assertEquals("4", shown("(−2)^2"))
        assertEquals("−4", shown("−2²"))
        assertEquals("512", shown("2^3^2"))
        assertEquals("0.5", shown("2^−1"))
        assertEquals("−6", shown("2×−3"))
        assertEquals("8", shown("2³"))
        assertEquals("−2", shown("(−8)^(1÷3)"))
    }

    @Test fun implicitTimes() {
        assertEquals("14", shown("2(3+4)"))
        assertEquals("21", shown("(1+2)(3+4)"))
        assertEquals(Num.shown(2 * Math.PI), shown("2π"))
        assertEquals("1", shown("2sin(30)"))
        assertEquals("14", shown("ans2"))
        assertEquals("14", shown("2ans"))
        assertEquals(Num.shown(2 * Math.E), shown("e2"))
        assertEquals("19.6133", shown("2g"))
    }

    @Test fun bracketsCloseThemselves() {
        assertEquals("14", shown("2(3+4"))
        assertEquals("0.5", shown("sin(30"))
        assertEquals("3", shown("√(4+5"))
        assertEquals(2, Calc.openBrackets("min(1,(2"))
        assertEquals("A ) without its (", fails("2)"))
        assertEquals("Empty ( )", fails("()"))
    }

    @Test fun percent() {
        assertEquals("220", shown("200+10%"))
        assertEquals("180", shown("200−10%"))
        assertEquals("20", shown("200×10%"))
        assertEquals("0.1", shown("10%"))
        assertEquals("200.05", shown("200+10%÷2"))
    }

    @Test fun angles() {
        assertEquals("0", shown("sin(180)"))
        assertEquals("0", shown("cos(90)"))
        assertEquals("1", shown("tan(45)"))
        assertEquals("30", shown("asin(0.5)"))
        assertEquals("0", shown("sin(π)", rad))
        assertEquals("1", shown("cos(0)", rad))
        assertTrue(fails("tan(90)").startsWith("tan(90°)"))
    }

    @Test fun functions() {
        assertEquals("12", shown("tip(80, 15)"))
        assertEquals("5", shown("hyp(3,4)"))
        assertEquals("212", shown("ctof(100)"))
        assertEquals("100", shown("ftoc(212)"))
        assertEquals("6", shown("f2(3)"))
        assertEquals("3", shown("√9"))
        assertEquals("3", shown("sqrt(9)"))
        assertEquals("120", shown("5!"))
        assertEquals("10", shown("nCr(5,2)"))
        assertEquals("20", shown("nPr(5,2)"))
        assertEquals("1", shown("mod(7,3)"))
        assertEquals("1", shown("min(4,1,9)"))
        assertEquals("3", shown("log(1000)"))
        assertEquals("tip takes 2 values, not 1", fails("tip(80)"))
        assertEquals("sin needs ( )", fails("sin"))
        assertEquals("Unknown name: xyz", fails("xyz"))
    }

    @Test fun mathErrors() {
        assertTrue(Calc.preview("1÷0", deg) == null)
        assertEquals("Can't divide by zero", fails("1÷0"))
        assertEquals("No real square root of a negative", fails("√−4"))
        assertEquals("ln needs a value above 0", fails("ln(0)"))
        assertEquals("Too large", fails("171!"))
    }

    @Test fun checkingAFunction() {
        assertNull(Calc.check(UserFn("inv", listOf("x"), "1 ÷ (x − 1)"), fns, true)) // ÷ 0 at x = 1 is fine
        assertNotNull(Calc.check(UserFn("bad", listOf("x"), "x + y"), fns, true))
        assertNotNull(Calc.check(UserFn("me", listOf("x"), "me(x)"), fns, true))
        assertNull(Calc.check(UserFn("area", listOf("r"), "πr²"), fns, true))
    }

    @Test fun numbers() {
        assertEquals("1,234,567.5", Num.shown(1234567.5))
        assertEquals("−1,000", Num.shown(-1000.0))
        assertEquals("1×10^15", Num.plain(1e15))
        assertEquals("1.5×10^−10", Num.plain(1.5e-10))
        assertEquals("1,500", shown("1.5E3"))
        assertEquals("2.5×10^20", Num.shown(Calc.eval("2.5×10^20", deg)))
        // An answer typed back in reads as itself.
        assertEquals(1.5e-10, Calc.eval(Num.plain(1.5e-10), deg), 1e-22)
        assertEquals("Two points in one number", fails("1.2.3"))
    }
}
