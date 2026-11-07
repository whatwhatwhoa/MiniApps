# Architecture

## Goals
The owner wants one page to jot the day's notes on, on a phone: paper that looks like paper,
tap a line and write, scratch words out with a swipe, and the same page on the home screen.
Single page, no extras: no list of notes, no formatting, no sync, no account, no internet.

## Files
| File | What it holds |
|---|---|
| `Paper.kt` | `Paper`, `NoteSize`, `NoteFont`; `PaperArt`, the one renderer for every paper |
| `Note.kt` | The note file (`Note`) and `Jump`, where a widget tap puts the cursor |
| `Prefs.kt` | Paper, size, font (by name) and swipe to erase, in SharedPreferences |
| `MainActivity.kt` | Edge-to-edge window, widget taps in, save and widget refresh on stop |
| `ui/NotePage.kt` | The page: paper, text field, tap below, band, undo pill, the sheet |
| `ui/Eraser.kt` | The word eraser: gesture, what to erase, the scratch, the fade and collapse |
| `ui/PaperSheet.kt` | The settings sheet: paper, size, font, swipe to erase |
| `widget/` | `NoteWidget` (provider and its lines), `WidgetPage` (wrapping and drawing) |
| `res/font/patrick_hand.ttf` | The handwriting font: Patrick Hand, Latin only (74 KB, cut by `tools/cut_font.py`); OFL, `licenses/` |
| `src/debug/.../WidgetHostActivity.kt` | A stand-in launcher for trying the widget |

## Paper
`PaperArt` draws on an `android.graphics.Canvas` with rectangles, lines and circles, the shapes
an SVG would hold; there are no image files. Each paper is a `Look`: paper color, rule color
(and a major color every fifth line for the grid), margin lines in dp from the left, where the
ink starts, and its band. `drawSheet` draws the page between two y values given the y of the
first rule and the line spacing; ruled papers have no rules above the first line, the grid and
dots run to the top. Rules are whole-pixel rectangles, so they stay crisp. `drawBand` draws
the strip along the top: a glued binding with a darker lip (yellow pad brown, legal pad red),
a wire spiral through punched holes (college), or a plain edge (grid, dot grid). The marks sit
at the band's bottom, so the app's band (which also covers the status bar) and the widget's
short band look alike. `drawThumb` is the picker's miniature.

| Paper | Paper color | Rules | Margin | Band |
|---|---|---|---|---|
| Yellow pad | pale yellow | grey-blue | none | brown binding |
| College | white | light blue | one pink line | spiral |
| Legal pad | canary yellow | blue | two red lines | red binding |
| Grid | off-white | light blue, darker every 5th | none | plain |
| Dot grid | off-white | grey dots | none | plain |

## The page
Sizes are Small, Medium and Large: 15, 18 and 22 sp text, the line spacing 1.8 × the text
size rounded to whole pixels. Fonts are Normal (the system sans) and Handwriting (Patrick
Hand, drawn 1.15× larger for about the same x-height, on the same line spacing, so the paper
does not change with the font). The ink's size is set from pixels (the text size's pixels ×
the font's factor, back to sp), and the text field's line height is that spacing given in `em`
of the pixels Compose draws that size at (a hair under, as Compose rounds line heights up),
so the lines match the rules at any system font size, with the font's line centred in each line
box, so every line is the same height. The paper is drawn in a layer behind the scrolling text
(only the visible part, moved with the scroll), its first rule just under the field's first
baseline and the rest every line below. The band is fixed on top with the paper button at its
right; the status bar icons turn light over a dark binding.

Writing: a tap on a line with text puts the cursor there (the text field's own handling). A
tap on a blank line below the text adds blank lines down to it and puts the cursor on it.
Blank lines at the end clear themselves on every edit or cursor move: only those down to the
cursor stay, so the page never slowly grows. The page always runs six lines past the text or
to the bottom of the screen.

## The eraser
A swipe across a line scratches out the words it crosses (unless *Swipe to erase* is off).
`scratchGesture` watches touches in the Initial pass and takes one over only when it starts on
a line with words and moves sideways quickly (twice the touch slop, twice as far across as down, before the long-press timeout), so
scrolling and text selection are untouched. While the finger moves, a zig-zag pen stroke follows
it along the middle of the letters. On release `findErase` takes every word touched, whole: the
words, plus the spaces after them (or before them at the end of a line); if nothing but space
would be left on that line, the whole line and its newline. Then, in about 0.7 s: the scratch
stretches over the whole words, the words and the scratch fade, and the gap closes. The words
narrow to nothing (a scale-x span, so the rest of the line slides over and reflows); a line left
empty is skipped in drawing and the lines below slide up into its place. Only then is the text
changed. An "Erased · Undo" pill stays for 5 s.

## Saving
`Note` keeps the note in `files/note.txt` through `AtomicFile`: written 0.5 s after typing
stops and at once when the app leaves the screen, so a cut-short write never leaves half a
note. The note and the two settings go with Android's backup and device transfer.

## The widget
A home-screen widget can't host a text field, and a launcher can't tell a swipe on it from a
page turn, so the widget shows the page and a tap on a line opens the app with the cursor
there. It is resizable with no upper bound, up to a whole home-screen page.

`NoteWidget.update` sets the paper color (a tinted image, so the launcher's rounded corners
stay), the band as one bitmap, and the list's rows themselves, in every update (no list
service, which newer Android reads once and keeps). `WidgetPage`
wraps the note to the widget's width with a `StaticLayout` in the chosen font. Each row is a
strip of paper with the line's ink over it. Every row of a paper is the same strip, and every
row gets the very same bitmap object, so the launcher stores it once (the grid has two: its
darker fifth lines). The ink is drawn by the app, since a launcher ignores an app's fonts: an
alpha-only picture cut to the line's width (about 100 KB a line), tinted in the row layout,
its baseline where the app puts it. The launcher lays rows over recycled ones, so each row sets
all it shows, the ink's visibility too. Rows run past the text to fill the widget: while the
note fits they end a dp short of its bottom (the launcher reports whole dp), the last row's
strip cut to fit, so the list has nothing to scroll; a longer note scrolls to one blank line
past its end. Each row carries where a tap puts the cursor: the end of that line, or, below the
text, how many blank lines to add (`Jump`). The app refreshes every widget when it leaves the
screen.
