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
update/                   :update, updates from GitHub: Updates, UpdateCard, UpdateBanner
apps/<app>/               one app: build.gradle.kts, src/, its notes (CLAUDE.md, docs/),
                          README.md + LICENSE (its public face), .gitignore, local.properties
templates/app/            the new-app skeleton tools/new_app.py fills in
tools/                    cut_fonts.py (the Stack fonts), new_app.py (the scaffold),
                          gallery_* (the README gallery)
docs/gallery/             the README's app cards, one PNG per app
```
A module is named after its folder: `apps/powermeter` builds as `:powermeter`. The package is
`io.github.whatwhatwhoa.<folder>`, set when the app is made and never changed (it is the
app's identity on a phone).

## Toolchain
AGP 9 (it compiles Kotlin itself; the Kotlin plugin only pins the Compose compiler), Kotlin,
Compose BOM, activity-compose: all in `gradle/libs.versions.toml`, so one bump moves every
app. compileSdk/targetSdk 37, minSdk 31 (Android 12) everywhere. JDK 21 from
`~/AppData/Local/Android/jdk-21`. An app depends on `project(":stack")` (which brings Compose
and Material 3 as `api`), `project(":update")` and `libs.activity.compose`; nothing else unless
its notes say why. Tests (JUnit, test scope only) where an app has logic worth pinning down.

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

## Updates
Every app updates itself from this repo's GitHub releases (`:update`, `Updates.kt`). It asks
GitHub's API for the releases, takes the newest tagged `<folder>-v<version>` with an `.apk`
(the folder is the last part of the package name, so an app needs no setting), and offers it
when its version is higher than the installed one. *Update* downloads the APK into the app's
cache, checks it is the same package signed with the same key (Android would refuse it anyway,
with a vaguer message), and hands it to Android's installer in a session; the installer asks,
replaces the app and keeps its data. The first time, Android asks to allow installs from it.
- **When:** Settings → Updates (*Check for updates*), and at most once a day when the app
  opens (on by default; a switch in the card). Between checks a newer release found earlier is
  offered from what was stored, without the network.
- **Where:** `UpdateCard()` in each app's settings; `UpdateBanner()` at the top of the page in
  the apps with a Stack frame, until *Not now* for that version. Daily Note has only the card,
  in its paper sheet (its page stays paper).
- **What it costs:** INTERNET and REQUEST_INSTALL_PACKAGES, merged from `:update`'s manifest;
  one anonymous call to api.github.com (60 an hour per address are allowed).
- **What a release must have:** the tag and an `.apk` signed with the app's key. A copy signed
  with another key (a debug key) can't take the update and says so in the card.

## Releases
1. Raise `versionCode` and `versionName` in `apps/<app>/build.gradle.kts`.
2. `./gradlew :<app>:assembleRelease :<app>:lintDebug`; check the APK's size (near 2 MB).
3. Install the signed release on a device and run it (foreground check first); its certificate
   must match the last release's (`docs/local.md` has each key's SHA-256).
4. Commit; tag `<app>-v<version>` (tags are per app in this repo).
5. Only when asked: push, and a GitHub release titled `<Name> <version>` on the tag, with
   `<Name>-<version>.apk` (the publish skill's *Shipping an app*); the apps' updater reads
   exactly that tag and asset. An app's download link is a
   search of this repo's releases (`releases?q=<Name>&expanded=true`): `releases/latest` is
   whichever app shipped last.

## Repos
This repo, public as `whatwhatwhoa/MiniApps` (MIT), holds every app with its full history (each
was imported with `git subtree add`; the apps' old tags carry an `<app>-` prefix). Every
release of every app is here: Power Meter's and Daily Note's first repos
(`whatwhatwhoa/PowerMeter`, `whatwhatwhoa/DailyNote`) had their releases copied here on their
tags with the same APKs (2026-11-07), point here from their READMEs; archiving them waits on the owner's OK. The root README opens with
the gallery (*Gallery*), then lists every app with its download and guide; each app's README is
written for someone installing it.

## Gallery
The root README's catalog: one card per app in `docs/gallery/<folder>.png` (600 × 933, shown
290 wide; each links to the app's download, as in the table), made by `tools/gallery_cards.py` from raw phone
screenshots: the app's name, a one-line subtitle and an edited shot of what it does, each in its
color from the series palette. A new app gets a line in the script's `cards` and a card in the README.
- **The shots** come from debug copies installed beside the real apps as
  `io.github.whatwhatwhoa.<folder>.shot` (`tools/gallery-shot.init.gradle` sets
  `applicationIdSuffix = ".shot"` on the debug build type), filled with made-up demo content,
  uninstalled afterwards: nobody's own data is shown, and no installed release is touched.
- **Content:** Calculator: `1250*1.08`, `√2`, `ctof(21)` with the four example functions, then
  `tip(80,15)` typed; Power Meter: open for 6+ minutes so the 10-minute chart fills; Display
  Compare: its first-run pair; Daily Note: a short day's list in Handwriting; Scan: the made-up
  receipt from `tools/gallery_receipt.py` added with *Choose photos*, before (Crop) and after
  (Filter & review) side by side.
- The raw screenshots stay out of the repo: their status bar shows the phone's notifications.

## Notes at two levels
Claude Code loads every CLAUDE.md from the session's folder up, so a session in `apps/<app>/`
reads the app's CLAUDE.md and the series'. Both stay small: together with the app's HANDOFF
they are that session's tip. The series CLAUDE.md holds what is true for every app; an app's
CLAUDE.md holds only what is true for it (its purpose, its hard rules, its package and
commands) and never repeats a series rule. Each level has the same files (HANDOFF, TASKS,
architecture, decisions, gotchas, VERIFY), created when first needed. The audit's `--series`
mode checks every level and flags a sentence an app repeats from the series.
