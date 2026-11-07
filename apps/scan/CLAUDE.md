# Scan — app rules (keep this file short; it loads with the series' CLAUDE.md)

Turns phone photos of receipts and documents into a cropped, cleaned-up PDF on the device:
take or add photos, fix the crop (edges found on their own), pick a filter, export; for phones
and tablets. A port of the owner's Scan Stack web tool. Package `io.github.whatwhatwhoa.scan`,
module `:scan`. Why and how: `docs/architecture.md`. The series' rules, commands and look:
`../../CLAUDE.md` (loaded with this file).

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md`: the first unchecked task under *Open*, or the owner's explicit ask |
| How it works (detection, filters, PDF, screens) | `docs/architecture.md` |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry |
| Hand-testing with the owner | `docs/VERIFY.md` |
| Traps of this app only | `docs/gotchas.md` (made with the first one; shared traps are the series') |

## Hard rules (this app; the series' rules apply too)
1. **A port of Scan Stack** (owner: "port over scan app for receipts from NZFielddocs"): the
   web tool (`C:\Projects\NZFieldDocs\library\General\nz-scan-stack.html`) is the reference
   for what Scan does: its detection, filters, page sizes and wording. A change of behavior is
   the owner's call; a change for the phone (touch, camera, share) is noted in `decisions/`.
2. **Pages stay on the device**, as the web tool promises: no upload, no online OCR, no
   permission beyond the series'. Photos come in through the camera app, the photo picker or a
   share; the PDF leaves only by Save or Share.
3. **The core is tested.** A change to `Detect.kt`, `Filters.kt` or `Pdf.kt` comes with its
   case in `CoreTest.kt`.

## Commands (this app; from the series root)
- Build + lint + tests: `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew :scan:assembleDebug :scan:lintDebug :scan:testDebugUnitTest`
- Run: `adb -s <dev> shell am start -n io.github.whatwhatwhoa.scan/.MainActivity`
- Release asset name: `Scan-<version>.apk`
