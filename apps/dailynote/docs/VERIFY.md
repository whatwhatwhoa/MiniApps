# Verify

Built but not hand-tested. One section per feature; delete a section when the owner confirms it.

## V-B1 — Owner's bugs (2026-11-03)
Seen by the session: the home-screen widget followed the owner's edits and a font switch
(Handwriting → Normal) each time they left the app; with a short note its list (1992 px) held
14 rows and a 99 px last one, ending 3 px short, `scrollable` false. Still for the owner:
- [ ] Handwriting, Large: a long note keeps every line on its rule, down to the last.
- [ ] Add a new line in the app, go home: the widget shows it (and an edit to an old line).
- [ ] A short note: the widget doesn't scroll; a note longer than the widget scrolls to its end.

## V-M1 — First light (2026-11-03)
Seen working on the Pixel by the session: typing on the rules (Normal, College and yellow pad,
Small and Medium, 130% system font), a word scratched out, a whole line scratched out with the
lines below moving up, the widget's paper on the home screen. Still for the owner:
- [ ] Each paper looks right: yellow pad, college, legal pad, grid, dot grid (sheet → *Paper*).
- [ ] Each size and both fonts keep the ink on the rules, also after scrolling a long note.
- [ ] Tap a blank line far below the text: the cursor lands on that line.
- [ ] Scratch one word, then a whole line: the scratch, the fade and the gap closing feel right;
      *Undo* brings the words back.
- [ ] A slow sideways drag after a long press still selects text; an up/down drag scrolls.
- [ ] *Swipe to erase* off: a sideways swipe does nothing.
- [ ] The widget shows the note's text in the chosen font after leaving the app; a tap on a
      line opens the app with the cursor on it; a tap below the text adds lines down to it.
- [ ] The widget resized to a whole home-screen page.
