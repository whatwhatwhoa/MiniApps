# Display Compare — app rules (keep this file short; it loads with the series' CLAUDE.md)

Compares display sizes (monitors, TVs, laptops, tablets, phones) the way displaywars.com does:
up to four displays drawn to scale on the Stack mat, four comparison tiles, a table, presets in
families. Package `io.github.whatwhatwhoa.displaycompare`, module `:displaycompare`. Why and
how: `docs/architecture.md`. The series' rules, commands and look: `../../CLAUDE.md`.

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md`: the first unchecked task under *Open*, or the owner's explicit ask |
| The model, the mat, the screen, presets | `docs/architecture.md`, the section by name |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry |
| Hand-testing with the owner | `docs/VERIFY.md` |
| Traps of this app only | `docs/gotchas.md` (made with the first one) |

## Hard rules (this app; the series' rules apply too)
1. **Honest geometry.** Sizes are the picture's; the shape comes from the pixels when a
   resolution is given (square pixels); a preset's diagonal is the viewable one; "This phone"
   says it is as Android reports it. Show the source of a number, not a guess.
2. **The mat shows the truth to scale**: one scale for every display, a grid in real units.
3. **Custom is always possible**: every field stays editable after a preset (owner).

## Commands (this app; from the series root)
- Build + lint: `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew :displaycompare:assembleDebug :displaycompare:lintDebug`
- Run: `adb -s <dev> shell am start -n io.github.whatwhatwhoa.displaycompare/.MainActivity`
- Release asset name: `DisplayCompare-<version>.apk`
