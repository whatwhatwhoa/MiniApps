# Mini Apps — series rules (keep this file short; it loads in every session, series or app)

The owner's small, plain Android apps for their phones and tablets, in one repo and one Gradle
build; each app is a folder under `apps/` (one line each in `docs/HANDOFF.md`). Kotlin + Jetpack
Compose with the shared Stack look (`:stack`) and updates from GitHub (`:update`), built from the
command line on a Windows ARM laptop. Public on GitHub (`whatwhatwhoa/MiniApps`, MIT). Why and how: `docs/architecture.md`.

## Two levels of notes
- **Series** (this file, `docs/`): what every app shares: the toolchain, `:stack`, these
  rules, the new-app recipe, cross-app traps.
- **App** (`apps/<app>/CLAUDE.md`, `apps/<app>/docs/`): one app's purpose, rules, state, tasks,
  decisions. A session started in `apps/<app>/` loads both CLAUDE.md files.
- A fact lives at the lowest level it is true for. IDs (T, D, B, V-) count per level; a pointer
  across levels names the level: "powermeter D1.4", "series D1.2".

## Read before working
| Task | Read |
|---|---|
| Work on one app (the owner names it) | `apps/<app>/CLAUDE.md`, then its `docs/HANDOFF.md` |
| Series work: `:stack`, toolchain, notes, a change to every app | `docs/HANDOFF.md`, then `docs/TASKS.md` |
| Any screen or UI change | `docs/stack-android.md` (tokens, components, layout rules) |
| A new app | `docs/new-app.md` |
| Build, signing, releases, the repo's layout | `docs/architecture.md` |
| Why the series is the way it is | `docs/decisions/INDEX.md`, then the one entry |
| Build, adb or device trouble | `docs/gotchas.md` |
| Hand-testing a series change | `docs/VERIFY.md` |

**Each fact has one home.** Rules here · state → `HANDOFF.md` · tasks → `TASKS.md` · how it
works → `architecture.md` · the look → `stack-android.md` · why → `decisions/` · traps →
`gotchas.md` · built-not-clicked → `VERIFY.md`, each at its level. Link, don't copy.

## Hard rules (every app)
1. **Plain and light** (owner: "doesn't need to be fancy; quick, light, decent font/layout").
   No library beyond `:stack`'s and `:update`'s without a reason; a release APK stays near 2 MB.
2. **Phones and tablets.** Every screen works at phone width and at ≥ 600 dp.
3. **One layout, look and feel, inherited from `:stack`** (owner: "for most situations, all apps
   should inherit similar layout, look and feel"). Every app's top is `StackTopBar` (the name as
   the first tab, its tabs, Theme, the Settings gear; settings, About and Updates behind the
   gear); colors and fonts are `:stack`'s, a new token goes in both themes (`stack-android.md`).
   A frame or look change for one app is made in `:stack` (or the template) and every app takes
   it: build them all. An app that differs says why in its decisions (Daily Note's paper).
4. **The owner's devices are daily devices.** Check the foreground app and the lock state
   before launching or installing (an install closes the app if it is open); ask before changing
   device settings; say so in chat when a device is locked.
5. **The repo is public.** Names, emails, home paths, device IDs and IPs stay out of tracked
   files: they go in `docs/local.md` (gitignored). The pre-push hook runs the privacy scan.
6. **Wording.** "commit" = local only; "commit to github" = commit + push; "release" = raise the
   version and build the signed release APK (the GitHub release is asked for separately).
   A GitHub release is tagged `<app>-v<version>` with `<Name>-<version>.apk`: the apps' updater
   finds it by that (`architecture.md` *Updates*).
7. **One app per session** unless the owner bundles several.

## Commands (Bash, from the series root)
- This PC's paths, device IDs, keys: `docs/local.md`
- Build + lint one app: `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew :<app>:assembleDebug :<app>:lintDebug`
  → `apps/<app>/build/outputs/apk/debug/<app>-debug.apk`; every app: `./gradlew assembleDebug lintDebug`
- Release: `./gradlew :<app>:assembleRelease` → `apps/<app>/build/outputs/apk/release/`; the
  routine is in `architecture.md` *Releases*
- adb: `~/AppData/Local/Android/Sdk/platform-tools/adb.exe -s <dev> install -r <apk>`, then
  `shell am start -n io.github.whatwhatwhoa.<app>/.MainActivity`; screenshot `exec-out screencap -p > shot.png`
- New app: `py -3.13 -I tools/new_app.py <folder> "<Name>"` (`docs/new-app.md`)
- Notes audit, series and every app: `py -I "$HOME/.claude/skills/notes-architecture/scripts/notes_audit.py" . --series`
- Privacy scan before a push: `py -I "$HOME/.claude/skills/publish/scripts/privacy_scan.py" .`

## End of session
1. Tick the task at its level (`apps/<app>/docs/TASKS.md` or `docs/TASKS.md`): date, one
   clause; the detail goes to a decision entry at that level.
2. New knowledge to its one home at the lowest level it is true for; a finished feature is
   described in that level's `architecture.md`, present tense, no task IDs.
3. Rewrite the HANDOFF of each level worked at; the series HANDOFF keeps one line per app.
4. Build state goes in the commit message, not the notes.
5. Run the notes audit with `--series`; fix what it flags.

## Style
Kotlin, Compose, one screen per file under `ui/`; comments explain why, not what. American
English in the UI, code and notes. UI help text one line per point (`Notes` in `:stack`).
