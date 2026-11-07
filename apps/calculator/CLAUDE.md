# Calculator — app rules (keep this file short; it loads with the series' CLAUDE.md)

A calculator for phones and tablets that reads like typing a formula: the whole formula with a
cursor, its answer live under it, a history of answers, and the user's own functions. Package
`io.github.whatwhatwhoa.calculator`, module `:calculator`. Why and how: `docs/architecture.md`.
The series' rules, commands and look: `../../CLAUDE.md` (loaded with this file).

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md`: the first unchecked task under *Open*, or the owner's explicit ask |
| How it works (engine, editor, screens) | `docs/architecture.md` |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry |
| Hand-testing with the owner | `docs/VERIFY.md` |
| Traps of this app only | `docs/gotchas.md` (made with the first one; shared traps are the series') |

## Hard rules (this app; the series' rules apply too)
1. **Editing keys, not "all that jazz"** (owner: "history, backspace, del, clear all instead of
   whatever all that jazz is"): ⌫, Del, Clear all and the arrows. No AC / C / CE, no memory keys.
2. **A formula, as typed** (owner: "closer to a formula typing experience"): the display is the
   formula being edited, never just the last number; the answer shows under it.
3. **The engine is tested.** A change to `Calc.kt` comes with its case in `CalcTest.kt`, and
   every rule the Settings notes promise has a test.

## Commands (this app; from the series root)
- Build + lint + tests: `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew :calculator:assembleDebug :calculator:lintDebug :calculator:testDebugUnitTest`
- Run: `adb -s <dev> shell am start -n io.github.whatwhatwhoa.calculator/.MainActivity`
- Release asset name: `Calculator-<version>.apk`
