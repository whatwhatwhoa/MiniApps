# Power Meter — agent rules (keep this file short; it loads every session)

A plain, light Android app that shows battery power: watts in or out, current, voltage, an
estimate of the cable's power, a live chart, history, sessions and screen-off drain. Phones and
tablets. Kotlin + Jetpack Compose (Material 3), built from the command line on a Windows ARM
laptop. Public on GitHub (`whatwhatwhoa/PowerMeter`), MIT license. Why and how:
`docs/architecture.md`.

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md`: the first unchecked task under *Open*, or the owner's explicit ask |
| How readings, logging, screens work | `docs/architecture.md` |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry |
| Odd device readings, Android limits | `docs/gotchas.md` |
| Hand-testing with the owner | `docs/VERIFY.md` |

**Each fact has one home.** Rules here · state → `HANDOFF.md` · tasks → `TASKS.md` · how it
works → `architecture.md` · why → `docs/decisions/` · traps → `gotchas.md` · built-not-clicked →
`VERIFY.md`. Link, don't copy.

## Hard rules
1. **Plain and light** (owner: "doesn't need to be fancy; quick, light, decent font/layout").
   No new libraries without a reason; the release APK is ~2 MB, keep it near that.
2. **Phones and tablets.** Every screen works at phone width and at ≥ 600 dp (rail + two columns).
3. **Say what is measured and what is estimated.** Battery power is measured; the cable figure
   is an estimate and is labeled so (`decisions/M1-first-light.md` D1.4).
4. **No wake lock.** The meter must not cause the drain it measures (D1.3).
5. **The owner's devices are daily devices.** Install and launch over adb freely; ask before
   anything that changes device settings.
6. **Commit wording**: "commit" = local only; "commit to github" = commit + push.

## Commands
- This PC's exact paths and the owner's device IDs: `docs/local.md` (gitignored, never
  published; the repo is public, so IDs, IPs and home paths stay out of tracked files).
- adb (Bash): `~/AppData/Local/Android/Sdk/platform-tools/adb.exe`; Wi-Fi devices from
  `adb mdns services`, then `adb connect <ip:port>`
- Build (Bash): `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew assembleDebug lintDebug`
  → `app/build/outputs/apk/debug/app-debug.apk` (signed with the release key when
  `local.properties` names it, so it installs over a release, D2.5); `assembleRelease` gives the ~2 MB release APK,
  signed with the key that the gitignored `local.properties` names (D1.6), else unsigned
- Release: bump versionCode/versionName; build + lint both; signed release run on a device
  (check what's in the foreground first: these are daily devices); same cert as the last
  release; tag `v<v>`; push; `gh release create` with `PowerMeter-<v>.apk`
- Install + run: `adb -s <dev> install -r <apk>`, then
  `adb -s <dev> shell am start -n io.github.whatwhatwhoa.powermeter/.MainActivity`
- Screenshot: `adb exec-out screencap -p > shot.png` (add `-d <id>` when a monitor is attached;
  ids from `adb shell dumpsys SurfaceFlinger --display-id`)
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
Kotlin, Compose, one screen per file under `ui/`; comments explain why, not what. American
English in the UI, code and notes. UI help text one line per point where it lists options.
