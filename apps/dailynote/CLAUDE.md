# Daily Note — app rules (keep this file short; it loads with the series' CLAUDE.md)

A single-page note for Android phones: one scrolling page of paper (yellow pad, college, legal
pad, grid, dot grid), tap a line to write, swipe across words to scratch them out, and a
home-screen widget up to a full page. The target is the owner's Pixel 11 Pro XL. Public on
GitHub in the series repo (MIT), with every release (its first repo is archived; series D1.7). Package
`io.github.whatwhatwhoa.dailynote`, module `:dailynote`. Why and how: `docs/architecture.md`.
The series' rules, commands and look: `../../CLAUDE.md`.

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md` (rules at the top): the first unchecked task under *Open*, or the owner's explicit ask |
| Paper, page, eraser or widget work | `docs/architecture.md`, the section by name |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry (`grep -n "^### " docs/decisions/<file>.md`) |
| Odd text placement or widget trouble | `docs/gotchas.md` |
| Hand-testing with the owner | `docs/VERIFY.md` |

## Hard rules (this app; the series' rules apply too)
1. **No extras, no features** (owner: "Just a single page, daily note, no extras, no
   features"). One page, its few settings (paper, size, font, swipe to erase), the widget.
   Anything more is the owner's call, never a session's: add it to *Later* in TASKS and ask.
2. **Paper is vector, drawn in code.** Every paper comes from `PaperArt` (owner: "all in svg
   generated"); no bitmaps or image files for paper. The app and the widget share it. The paper
   is the page's own look; the Stack look is for the sheet and the pill only (series D1.2).
3. **Ink sits on the rules.** Line height in whole pixels and in `em`, rules drawn from the
   text's own first baseline; check alignment at a large system font size too (`gotchas.md`).

## Commands (this app; from the series root)
- Build + lint: `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew :dailynote:assembleDebug :dailynote:lintDebug`
- Run: `adb -s <dev> shell am start -n io.github.whatwhatwhoa.dailynote/.MainActivity`
- Widget without touching the home screen (debug builds): once
  `adb shell appwidget grantbind --package io.github.whatwhatwhoa.dailynote --user 0`, then
  `am start -n io.github.whatwhatwhoa.dailynote/.WidgetHostActivity` (extras `--ei w`/`--ei h` in dp)
- Font re-cut: `apps/dailynote/tools/cut_font.py` (fontTools: `../../docs/local.md`)
- Release asset name: `DailyNote-<version>.apk`
