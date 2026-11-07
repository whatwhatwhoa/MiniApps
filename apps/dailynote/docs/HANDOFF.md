# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-03, ~2 am):** 0.2.0 released (versionCode 2): the owner's first bug batch,
B1. The widget showed no new lines (the launcher reapplies rows onto recycled ones and a blank
row's hidden ink stayed hidden), scrolled with nothing below (rows ran ~170 px past its
bottom; now they fill it exactly), and Handwriting crept off the rules line by line at the
owner's 130% font, 7 px a line on Large (`gotchas.md`). The session saw the widget follow
edits and a font switch, and its list not scrollable with a short note. 0.2.0 is on the Pixel
(same cert as 0.1.0); lint clean; release APK 2.2 MB. The owner uses Handwriting, Large,
College paper. The release key is backed up with every other app's (`docs/local.md`).

## Next
1. The owner sees B1 (`VERIFY.md` *V-B1*), mainly Handwriting on Large down a long note.
2. The owner tries the rest of M1 (`VERIFY.md` *V-M1*, T1.8).
3. README screenshots, when the Pixel is free (each paper, the sheet, the widget).

## Waiting on the owner
- As built, to confirm: the name *Daily Note* (D1.1); "dot matrix" read as dot grid, and the
  two yellows told apart (D1.2); the *Undo* pill I added (D1.5); Patrick Hand (D1.8).

## Uncommitted
Nothing.

## Watch out (this week only)
- The Pixel is the owner's own phone and they use it while the session works: check
  `dumpsys activity activities | grep topResumedActivity` before launching or installing, and
  say so in chat when it is locked (the owner asked to be pinged).
