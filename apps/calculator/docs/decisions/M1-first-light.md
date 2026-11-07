# M1 — First light

### D1.1 · 2026-11-07 · Package and toolchain
`io.github.whatwhatwhoa.calculator`, app name *Calculator*, made from the series template: the
series' toolchain, `:stack` and `:update`, no other library; JUnit for the engine's tests only.
Its own release key, made before the first install. Status: as built 2026-11-07, owner to
confirm the name.

### D1.2 · 2026-11-07 · The display is the formula, with a cursor
Owner: "closer to a formula typing experience." The formula stays on screen as typed, with a
cursor that a tap or the arrows move and that steps over a name and its bracket as one; the
answer shows live under it; brackets close themselves, shown faint. After = the answer becomes
the formula and an operator goes on from it, as most phone calculators do. Rejected: a text
field with the system keyboard (the keypad is the point); showing only the last number.
Status: as built 2026-11-07, owner to confirm.

### D1.3 · 2026-11-07 · ⌫, Del, Clear all and arrows instead of AC / C / CE
Owner: "history, backspace, del, clear all instead of whatever all that jazz is." ⌫ deletes
before the cursor, Del after it (so the arrows matter: they sit beside them), Clear all empties
the formula and leaves history; the three repeat when held (not Clear all). Clear history is
its own button, with a question first. Status: as built 2026-11-07, owner to confirm.

### D1.4 · 2026-11-07 · Percent adds a share, a bare percent is a hundredth
`200 + 10%` is 220 and `200 − 10%` is 180, as a tip or a discount is meant; `200 × 10%` is 20
and `10%` alone 0.1. Rejected: % always ÷ 100 (then 200 + 10% is 200.1, which nobody means).
Status: as built 2026-11-07, owner to confirm.

### D1.5 · 2026-11-07 · Functions are named formulas, checked before they are saved
Owner: "customizable functions." A function is a name, up to six values and a formula of
them, typed with the system keyboard on the Functions tab; it can use the built-ins and other
functions, and goes in the strip, first, to be used like sin. No values makes a named number.
It is checked by trying it with 1 for each value (a function that uses itself fails); *Try it*
works it out with the user's values. Four examples on request, none forced in. Rejected: a
script language with conditions (not a calculator any more); functions on the keypad itself.
Status: as built 2026-11-07, owner to confirm.

### D1.6 · 2026-11-07 · Doubles, shown to 12 digits
The engine computes in doubles and shows 12 significant digits, so float noise never shows
(0.1 + 0.2 is 0.3); trig in degrees is exact at multiples of 90°. Rejected: BigDecimal
throughout (exact decimals, but no fast exact trig, roots or powers, and more code).
Status: as built 2026-11-07.
