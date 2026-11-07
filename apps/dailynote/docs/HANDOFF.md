# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-06):** 0.2.0 is the release (versionCode 2; GitHub release too). Since
2026-11-06 the app lives in the series (`C:\Projects\MiniApps\apps\dailynote`); the paper,
page and widget are unchanged, while the paper sheet and the undo pill wear the Stack look
(Stack labels, segments and notes; Handwriting still shown in its own letters; series D1.2).
That build is on the owner's Pixel (release-signed debug over 0.2.0; the note kept). Lint
clean; release APK 2.37 MB (was 2.2: the Stack fonts). The owner uses Handwriting, Large,
College paper.

## Next
1. The owner sees B1 (`VERIFY.md` *V-B1*), mainly Handwriting on Large down a long note.
2. The owner looks at the sheet's new look (series `VERIFY.md` *V-stack-look*).
3. The owner tries the rest of M1 (`VERIFY.md` *V-M1*, T1.8).
4. README screenshots, when the Pixel is free (each paper, the sheet, the widget).

## Waiting on the owner
- As built, to confirm: the name *Daily Note* (D1.1); "dot matrix" read as dot grid, and the
  two yellows told apart (D1.2); the *Undo* pill I added (D1.5); Patrick Hand (D1.8).

## Uncommitted
The Stack look (a series change): `ui/Theme.kt`, `ui/PaperSheet.kt`, the build file on `:stack`.
