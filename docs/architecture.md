# Architecture (series)

## Goals
The owner makes small Android apps for their own phones and tablets, each plain and light, one
job per app. The series exists so a new one is quick: one toolchain, one look, one set of rules
and notes, and a scaffold that turns a name into a building app with its notes in minutes. Each
app keeps its own purpose, rules and history under `apps/<app>/`.

## Layout
```
CLAUDE.md, docs/          series notes (rules, state, tasks, the look, decisions, traps)
settings.gradle.kts       :stack + every apps/<folder> with a build.gradle.kts, found by folder
build.gradle.kts          plugin versions only (gradle/libs.versions.toml holds every version)
stack/                    :stack, the Stack look for Compose (stack-android.md)
apps/<app>/               one app: build.gradle.kts, src/, its notes (CLAUDE.md, docs/),
                          README.md + LICENSE (its public face), .gitignore, local.properties
templates/app/            the new-app skeleton tools/new_app.py fills in
tools/                    cut_fonts.py (the Stack fonts), new_app.py (the scaffold)
```
A module is named after its folder: `apps/powermeter` builds as `:powermeter`. The package is
`io.github.whatwhatwhoa.<folder>`, set when the app is made and never changed (it is the
app's identity on a phone).

## Toolchain
AGP 9 (it compiles Kotlin itself; the Kotlin plugin only pins the Compose compiler), Kotlin,
Compose BOM, activity-compose: all in `gradle/libs.versions.toml`, so one bump moves every
app. compileSdk/targetSdk 37, minSdk 31 (Android 12) everywhere. JDK 21 from
`~/AppData/Local/Android/jdk-21`. An app depends on `project(":stack")` (which brings Compose
and Material 3 as `api`) and `libs.activity.compose`; nothing else unless its notes say why.

## The look
Every app uses `StackTheme` from `:stack`: the Stack Design Guide's tokens, Instrument Sans and
JetBrains Mono (bundled, cut to Latin), Material 3 mapped onto the tokens, Auto/Light/Dark kept
per app. What the components are and how a screen is laid out: `stack-android.md`. An app
whose page is not chrome (Daily Note's paper) keeps that page its own and uses Stack for the
rest.

## Signing
Each app has its own release key in `~/.android/<app>-release.jks`; `apps/<app>/local.properties`
(gitignored) names it with its passwords (`release.storeFile`, `release.storePassword`,
`release.keyAlias`, `release.keyPassword`), so the keys never mix. The root `local.properties`
holds only `sdk.dir`. When the key is there, debug builds are signed with it too, so a test
build and a published release replace each other on a phone without losing data. Every key and
its `local.properties` is backed up in the owner's backup folder (`docs/local.md`).

## Releases
1. Raise `versionCode` and `versionName` in `apps/<app>/build.gradle.kts`.
2. `./gradlew :<app>:assembleRelease :<app>:lintDebug`; check the APK's size (near 2 MB).
3. Install the signed release on a device and run it (foreground check first); its certificate
   must match the last release's (`docs/local.md` has each key's SHA-256).
4. Commit; tag `<app>-v<version>` (tags are per app in this repo).
5. Only when asked: push, and a GitHub release titled `<Name> <version>` with
   `<Name>-<version>.apk` (the publish skill's *Shipping an app*). An app's download link is a
   search of this repo's releases (`releases?q=<Name>&expanded=true`): `releases/latest` is
   whichever app shipped last.

## Repos
This repo, public as `whatwhatwhoa/MiniApps` (MIT), holds every app with its full history (each
was imported with `git subtree add`; the apps' old tags carry an `<app>-` prefix). Power
Meter's and Daily Note's first repos (`whatwhatwhoa/PowerMeter`, `whatwhatwhoa/DailyNote`)
keep their releases and open with a note pointing here; their apps' READMEs still link there
for downloads until each app's first release from the series. The root README lists every
app with its download and guide; each app's README is written for someone installing it.

## Notes at two levels
Claude Code loads every CLAUDE.md from the session's folder up, so a session in `apps/<app>/`
reads the app's CLAUDE.md and the series'. Both stay small: together with the app's HANDOFF
they are that session's tip. The series CLAUDE.md holds what is true for every app; an app's
CLAUDE.md holds only what is true for it (its purpose, its hard rules, its package and
commands) and never repeats a series rule. Each level has the same files (HANDOFF, TASKS,
architecture, decisions, gotchas, VERIFY), created when first needed. The audit's `--series`
mode checks every level and flags a sentence an app repeats from the series.
