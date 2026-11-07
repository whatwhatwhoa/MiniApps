# Daily Note — agent rules (keep this file short; it loads every session)

A single-page note for Android phones: one scrolling page of paper (yellow pad, college, legal
pad, grid, dot grid), tap a line to write, swipe across words to scratch them out, and a
home-screen widget up to a full page. Kotlin + Compose, built from the command line on a
Windows ARM laptop. Public on GitHub (`whatwhatwhoa/DailyNote`), MIT. Why and how:
`docs/architecture.md`.

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md` (rules at the top): the first unchecked task under *Open*, or the owner's explicit ask |
| Paper, page, eraser or widget work | `docs/architecture.md`, the section by name |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry (`grep -n "^### " docs/decisions/<file>.md`) |
| Odd text placement, widget or device trouble | `docs/gotchas.md` |
| Hand-testing with the owner | `docs/VERIFY.md` |

**Each fact has one home.** Rules here · state → `HANDOFF.md` · tasks → `TASKS.md` · how it
works → `architecture.md` · why → `decisions/` · traps → `gotchas.md` · built-not-clicked →
`VERIFY.md`. Link, don't copy.

## Hard rules
1. **No extras, no features** (owner: "Just a single page, daily note, no extras, no
   features"). One page, two settings (paper, size), the widget. Anything more is the owner's
   call, never a session's: add it to *Later* in TASKS and ask.
2. **Paper is vector, drawn in code.** Every paper comes from `PaperArt` (owner: "all in svg
   generated"); no bitmaps or image files for paper. The app and the widget share it.
3. **Ink sits on the rules.** Line height in whole pixels and in `em`, rules drawn from the
   text's own first baseline; check alignment at a large system font size too (`gotchas.md`).
4. **Light.** No new libraries without a reason; the release APK is ~2 MB.
5. **The owner's devices are daily devices.** The target is the owner's Pixel 11 Pro XL. Check
   the foreground app and the lock state before launching anything; if it is locked, tell the
   owner (they asked to be pinged).
6. **Commit wording**: "commit" = local only; "commit to github" = commit + push.

## Commands
- This PC's exact paths and the owner's device IDs: `docs/local.md` (gitignored, never
  published; the repo is public, so IDs, IPs and home paths stay out of tracked files).
- adb (Bash): `~/AppData/Local/Android/Sdk/platform-tools/adb.exe`; Wi-Fi devices from
  `adb mdns services`, then `adb connect <ip:port>` (a new PC pairs first: the owner reads out
  the pairing code)
- Build (Bash): `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew assembleDebug lintDebug`
  → `app/build/outputs/apk/debug/app-debug.apk`; `assembleRelease` gives the ~2 MB release
  APK, signed with the key that the gitignored `local.properties` names (D1.7), else unsigned
- Run: `adb -s <dev> shell am start -n io.github.whatwhatwhoa.dailynote/.MainActivity`
- Widget without touching the home screen (debug builds): once
  `adb shell appwidget grantbind --package io.github.whatwhatwhoa.dailynote --user 0`, then
  `am start -n io.github.whatwhatwhoa.dailynote/.WidgetHostActivity` (extras `--ei w`/`--ei h` in dp)
- Screenshot: `adb exec-out screencap -p > shot.png`
- Release: bump versionCode/versionName; build + lint; signed release run on the Pixel; same
  cert as the last release; tag `v<v>`; push; `gh release create` with `DailyNote-<v>.apk`
- Notes audit: `py -I "$HOME/.claude/skills/notes-architecture/scripts/notes_audit.py" .`
- Privacy scan before a push (the pre-push hook runs it too):
  `py -I "$HOME/.claude/skills/publish/scripts/privacy_scan.py" .`

## End of session
1. Tick the task in `docs/TASKS.md` (date, one clause); its detail goes to a decision entry.
2. New knowledge to its one home (table above); a finished feature is described in
   `architecture.md` in present tense, no task IDs.
3. Rewrite `docs/HANDOFF.md` from scratch: state ≤ 10 lines, next, waiting on the owner,
   uncommitted.
4. Build state goes in the commit message, not the notes.
5. Run the notes audit; fix what it flags.

## Style
Kotlin, Compose; comments explain why, not what. American English in the UI, code and notes.
UI help text one line per point.
