# Verify

Built but not hand-tested. One section per feature; delete a section when the owner confirms it.

## V-M1 — First light (2026-11-07)
- [ ] Type `2(3+4`: the display shows `2(3+4)` with a faint `)`, and "= 14" under it.
- [ ] = puts "2(3+4) = 14" in history; then × 2 = gives 28; then 5 starts a new formula.
- [ ] Tap in the middle of a formula: the cursor goes there; ⌫ deletes before it, Del after;
      ◀ ▶ step over `sin(` in one go; holding ⌫ keeps deleting.
- [ ] Clear all empties the formula; history stays. Clear history asks first.
- [ ] `200 + 10%` = 220; `sin(30)` = 0.5 in Deg; Deg → Rad changes the header to "radians".
- [ ] Tap a history line: its answer goes in at the cursor; hold one: its formula comes back.
- [ ] Hold the formula: the answer is copied.
- [ ] Functions → Add four examples: tip, hyp, ctof, ftoc in the strip; `tip(80, 15)` = 12.
- [ ] New function `area`, values `r`, formula `pi*r^2`: it reads "area(r) = pi×r^2"; Try it
      with 2 gives 12.5663706144; Save; it is first in the strip.
- [ ] A long formula shrinks and wraps; nothing is cut off at 130% font.
- [ ] Landscape (≥ 600 dp): history and display left, keys right, keys at least 36 dp.
- [ ] Turn the phone or leave and come back: the formula and history are where they were.
