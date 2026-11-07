# Architecture

## Goals
The owner wants a calculator "with history, backspace, del, clear all instead of whatever all
that jazz is", that "should readout closer to a formula typing experience", with "customizable
functions". So: the display is the formula with a cursor that can go anywhere in it; the answer
shows live under it; = puts the line in history; the user makes functions once and uses them
like sin. Left out: memory keys, AC/C/CE, a scientific mode switch, graphing, units.

## The engine (`Calc.kt`)
A recursive-descent parser that works the formula out as it reads it, in doubles:
`expr := term (± term)*`, `term := unary ((× | ÷ | nothing) unary)*`,
`unary := (− | +) unary | power`, `power := postfix (^ unary)?`, `postfix := primary (! | % | ² | ³)*`,
`primary := number | ( expr ) | π | √ unary | ∛ unary | name [ ( args ) ]`.
- **Signs:** the keypad's × ÷ − and a keyboard's * / -. `E` is a number's exponent (`1.5E3`), `e`
  Euler's number.
- **Times without a sign** before a bracket, a name, π or √: `2π`, `3(4)`, `2sin(30)`. A name
  followed by digits keeps them only if that makes a known name (`log2`, a user's `f2`), so
  `ans2` is ans × 2.
- **Brackets left open at the end close themselves** (`openBrackets` counts them for the faint
  `)` in the display).
- **Percent:** `a + b%` and `a − b%` add or take b percent of a (the term was a bare `n%`);
  anywhere else `b%` is b ÷ 100.
- **Powers:** right to left; −2^2 is −4; an odd root of a negative is real ((−8)^(1/3) = −2).
- **Angles:** degrees or radians (`Env.degrees`); in degrees multiples of 90° are exact, and
  trig answers within 1e-15 of a whole number are snapped to it (sin(π) is 0).
- **Built in:** sin cos tan asin acos atan sinh cosh tanh ln log log2 exp sqrt cbrt abs round
  floor ceil min max mod nCr nPr; constants π (pi), e, ans.
- **Errors:** `CalcError` when the formula can't be read (shown after =, never while typing),
  `MathError` when it has no real answer (÷ 0, √ of a negative, too large).
- **Numbers shown:** `Num.plain` rounds to 12 significant digits (0.1 + 0.2 is 0.3), writes
  ×10^n from 10^15 up and below 10^-9, and the keypad's minus, so an answer reads back in as
  itself; `Num.shown` groups the whole part by thousands.
- **User functions** (`UserFn`: name, values, formula) are parsed afresh at each call with their
  values bound; a value's name hides a constant. A call deeper than 40 is an error, which also
  stops a function that uses itself. `Calc.check` tries a new function with 1 for each value:
  a read error blocks saving it, a math error doesn't.

## The editor (`Model.kt`, `CalcModel`)
The formula is text with a cursor. The cursor steps over a name and its `(` as one (`sin(` is
one key, one ⌫, one arrow press); a tap in the formula goes to the nearest such place.
- **Keys:** an operator typed right after one replaces it (5+ then × is 5×), except − after × ÷ ^
  (a negative number); a second point in one number is ignored; × ÷ + ^ on an empty formula do
  nothing.
- **=** works it out: the formula with its brackets closed and the answer go to the top of
  history (200 kept), `ans` is the answer, and the answer becomes the formula, marked *fresh*:
  an operator then goes on from it (a negative or ×10^ answer is bracketed first before ^ ² ³ !
  %), anything else starts a new formula. A formula that can't be worked out stays, with the
  reason under it in the danger color.
- **History:** tap a line to put its answer in at the cursor (bracketed when it is negative and
  would join what is before); hold it to get its formula back.
- **Saved** in one preferences file ("calc") on every change: the formula, cursor, fresh, ans,
  the angle unit, history and functions (JSON), so the app opens where it was left.

## Screens
One row on top: the tabs Calculator · Functions (the first tab is the app's name, so there is no
title above them; it is set larger and bold, series D1.9), then the Theme icon and
the Settings gear (series D1.10): the series' `StackTopBar` (D1.11); under it the
series' update note while a newer release is out. The angle unit shows on the Deg/Rad key.
- **Calculator** (fills the screen, no page scroll): history (newest at the bottom, *Clear
  history* above the oldest), the display (the formula in JetBrains Mono, right-aligned, 34 sp
  shrinking to 23 sp as it grows, a blinking accent cursor, the faint closing brackets; the
  live answer "= …" under it, or the error), a sideways strip (Deg/Rad, the user's functions in
  the accent soft color, √ x² xʸ ! % π e ans and the functions, *+ Function*), and the keypad:
  ◀ ▶ Del ⌫ · Clear all ( ) ÷ · 7 8 9 × · 4 5 6 − · 1 2 3 + · 0 . , =. Under 600 dp one column,
  keys half the height (44–66 dp each); from 600 dp history and the display on the left, the
  strip and keys on the right (up to 460 dp wide, keys 36–72 dp). ⌫, Del and the arrows repeat
  while held; every key ticks the haptic feedback.
- **Functions:** your functions with Use, Edit, Delete (Delete names the functions that use it);
  *New function* opens the editor in the page: Name, Values it takes, Formula (the keyboard's
  * / - become × ÷ −), the function as it will read, a check line, *Try it* with values; *Add
  four examples* when there are none (tip, hyp, ctof, ftoc). A card on writing a formula.
- **Settings:** Angles, History (count, Clear history), the series' Updates card, how it reads
  a formula (one line per rule), About.
- **A keyboard** types into the Calculator tab: digits and signs, * / - for × ÷ −, letters for
  names, Enter or = to work out, ⌫ and Delete, the arrows, Esc for Clear all.

## Files
- `Calc.kt`: the engine (`Calc`, `Parser`, `Num`, `UserFn`, `Env`, the errors).
- `Model.kt`: `CalcModel` (the editor, history, functions, saving), `Entry`, the examples.
- `MainActivity.kt`: the model and `StackTheme` around `ui/MainScreen`.
- `ui/MainScreen.kt`: the frame and tabs. `ui/CalcPage.kt`: history, display, strip, keypad,
  keyboard. `ui/FunctionsPage.kt`: the list and the editor. `ui/SettingsPage.kt`.
- `src/test/.../CalcTest.kt`: the engine's rules, one test group each (JUnit, test scope only).
