# M1 — First light

### D1.1 · 2026-11-03 · Package, toolchain, min SDK
`io.github.whatwhatwhoa.dailynote`, app name *Daily Note*, minSdk 31 (Android 12: the
widget's rounded corners, mutable fill-in intents, dynamic color; as the owner's other apps),
target 37, PowerMeter's AGP/Kotlin/Gradle versions, Material 3 and nothing else (release APK
~2.3 MB with the font). Status: as built 2026-11-03, owner to confirm the name.

### D1.2 · 2026-11-03 · Papers are vector shapes drawn in code
Owner: "different background (all in svg generated): old school yellow note pad, college pad,
legal pad, grid, and dot matrix". The papers are drawn by `PaperArt` from rectangles, lines and
circles (what an SVG would hold) on an Android canvas; one renderer serves the app, the widget
and the picker's thumbnails. Rejected: real `.svg` files (Android draws SVG only through a
library, and a page that grows with the text needs repeat rules, not a fixed picture);
VectorDrawables (can't tile). "Dot matrix" was read as dot-grid paper, labeled *Dot grid*; the
old green-bar printer paper is the other reading. "Old school yellow note pad" and "legal pad"
were told apart as a pale pad with a brown binding and no margin, and a canary legal pad with
blue rules, a red double margin and a red binding. Status: as built 2026-11-03, owner to
confirm "dot matrix" and the two yellows.

### D1.3 · 2026-11-03 · Ink on the rules: whole-pixel line height in em
The line spacing is 1.8 × the text size, rounded to whole pixels; the text field's line height
is that spacing in `em`, and the paper's rules are drawn from the field's first baseline. First
built in `sp`: on the owner's Pixel (font size 130%) Compose's nonlinear font scaling made the
lines ~20% taller than the rules (`gotchas.md`). Then Handwriting crept off the rules line by
line (owner, B1.2): its size was given as sp × 1.15, which a large system font scales by less
than the em assumed; now the size comes from pixels and the em from the size Compose draws.
Status: as built 2026-11-03.

### D1.4 · 2026-11-03 · Tap a line to write
Owner: "tap on a line to write". A tap on text is the text field's own; a tap on a blank line
below adds blank lines down to it. First, blank lines left at the end were trimmed only when the
field lost focus, which hiding the keyboard doesn't do; owner: "self clear breaks at the end, so
the page scroll doesn't slowly build up". Now every edit and cursor move drops the blank lines
past both the text and the cursor. Status: as built 2026-11-03.

### D1.5 · 2026-11-03 · Erase by word: scratch, fade, close the gap
Owner, mid-session: "Only delete by word, and have a scratch animation, then disappearing,
then collapsing the space". First built as the original ask ("swipe across the note to erase
it"): a wipe of the whole note, replaced the same session. Now a quick sideways swipe on a line
scratches out every word it touches, whole; the space beside them goes too, and a line left
empty goes with its newline, the lines below sliding up (`architecture.md` *The eraser*). The
gesture yields to scrolling and to long-press selection. Added by me, owner to confirm: an
"Erased · Undo" pill for 5 s. Owner, later: a *Swipe to erase* switch, on by default.
Status: as built 2026-11-03, owner to confirm the undo pill and the feel.

### D1.6 · 2026-11-03 · The widget: the page, read-only, tap a line to write
Owner: "It can also exist as a full sized widget". A home screen can't host a text field or
tell a swipe on a widget from a page turn, so the widget shows the page (scrolling) and a tap on
a line opens the app with the cursor there. First built as one bitmap per line (paper and ink
together): on Android 17 the system inlines a widget's whole list into one bundle, and the
owner's widget held 12.4 MB of bitmaps (and showed no text). Then a text view over a shared
paper strip: the launcher ignored the bundled font (owner: "widget rendering wrong font").
Now the ink is drawn by the app as an alpha-only picture per line over the shared strip, about
1 MB for the owner's widget; and the rows go in each update, as the system kept a list
service's first answer (owner: "widget is not refreshing"). Still not refreshing (B1.1): the
launcher reapplies rows onto recycled ones, and a row's ink, hidden on a blank line, was never
shown again; each row now sets its ink visible. It also scrolled with nothing below (B1.3):
the rows were rounded up and one added, ~170 px past the bottom; now, while the note fits,
they fill the widget a dp short, the last row cut to fit. Rejected: Glance (a library, the same limits).
Status: as built 2026-11-03, owner to confirm on the home screen.

### D1.7 · 2026-11-03 · Release signing: a key of its own, outside the repo
As PowerMeter (D1.6 there): a 4096-bit RSA key for this app alone in the user's `.android`
folder, its path and passwords in the gitignored `local.properties` (`release.*`); without them
a release build is unsigned. Every later release must use the same key: compare
`apksigner verify --print-certs` with the last release's asset. Status: as built 2026-11-03;
the owner backs up the key.

### D1.8 · 2026-11-03 · Two fonts: Normal and Handwriting (Patrick Hand)
Owner: "set up two fonts: a handwriting style font, and the normal font". Normal is the system
sans. Handwriting was first Caveat (most like pen writing); owner: "a more legible hand writing
style font". Now Patrick Hand (SIL OFL 1.1, no reserved name): upright, even print with a pen's
feel, picked from ten Google Fonts handwriting faces side by side (Playpen Sans as clear but
reads as a rounded sans; Kalam legible but slanted). Bundled Latin only, 74 KB, by
`tools/cut_font.py`, its copyright and license kept in the file and in `licenses/`. Drawn 1.15×
larger for the normal font's x-height, on the same line spacing. Rejected: downloadable Google
Fonts (needs Play services and the internet). Status: as built 2026-11-03, owner to confirm.
