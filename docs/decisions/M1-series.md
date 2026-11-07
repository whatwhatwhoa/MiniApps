# M1 — The series (series decisions)

The owner, 2026-11-06: "consolidate these mini apk apps into one larger project folder, use
/notes-architecture that can also be targeted at the app level as well. The theme is to have a
general notes architecture that makes iterating mini apps quick and easy", and "update the
look and feel, closer to my 'stack' design of apps".

### D1.1 · 2026-11-06 · One repo, one build; the apps keep their history
`C:\Projects\MiniApps`, one git repo, one Gradle build. Each app is a module under
`apps/<folder>` (its old `app/` module flattened into it), found by folder; the wrapper, the
root build file and `gradle.properties` moved to the root (both apps' copies were identical).
Power Meter and Daily Note came in by `git subtree add` with every commit, then one layout
commit; their tags carry an `<app>-` prefix (both had a `v0.1.0`). Power Meter's uncommitted M2
work came over still uncommitted. The old folders `C:\Projects\PowerMeter` and `DailyNote`
and their public GitHub repos are untouched.
Rejected: a folder of separate repos (no shared module without submodules); a fresh repo
without history (loses the apps' story).
Open, the owner's call: how the series meets GitHub. (a) One public repo for the series, the
two old repos' READMEs pointing to it (their releases stay); per-app download links then use
tag URLs, since `releases/latest` is whichever app shipped last. (b) Keep a public repo per
app, fed with `git subtree push --prefix=apps/<app>`; but an app needs `:stack` to build, so a
lone app repo no longer builds by itself. Recommended: (a).
Status: as built 2026-11-06, owner to confirm. The owner chose (a) the same day: public as
`whatwhatwhoa/MiniApps`, the old repos pointing to it (one README commit each). Recycling the
old folders waits on the owner.

### D1.2 · 2026-11-06 · `:stack`: the Stack Design Guide for Compose, in every app
A library module with the guide's tokens copied value for value (light and dark), the type
scale, Material 3 mapped onto the tokens, Auto / Light / Dark per app with a header button,
system bars that follow the theme, and the components (`stack-android.md`). Instrument Sans
(400–700) and JetBrains Mono (400, 500) are bundled, cut to Latin static weights by
`tools/cut_fonts.py` (~56–60 KB each, OFL notices kept in `stack/licenses/`). Release APKs grew
about 0.2–0.5 MB (Power Meter 2.38 MB, Daily Note 2.37 MB, Display Compare 2.26 MB).
Changed in the apps: Power Meter's wallpaper colors, bottom bar and rail gave way to the Stack
palette, a header with a mono status count and tabs on top; its cards, segmented choices,
buttons and chart colors are Stack's; charging is `good` green, draining `s1` orange. Daily
Note's paper is untouched; its sheet and undo pill are Stack's, and it keeps its own bars
(`StackTheme(systemBars = false)`).
Rejected: downloadable Google Fonts (needs Play services and a network); the variable fonts
whole (bigger, and Compose's variable-font support is the less-trodden path).
Status: as built 2026-11-06, owner to confirm (`VERIFY.md` *V-stack-look*).

### D1.3 · 2026-11-06 · Notes at two levels
The series tip (`CLAUDE.md`, `docs/HANDOFF.md`) holds what every app shares; an app's tip
(`apps/<app>/CLAUDE.md`, its HANDOFF) holds only that app's. Claude Code loads both CLAUDE.md
files in an app session, so the app's never repeats a series rule. A fact lives at the lowest
level it is true for; IDs count per level and a pointer across levels names the level. The
shared traps moved from the apps' gotchas to the series' verbatim (Power Meter's heredoc entry
dropped: Daily Note's covers it). The apps' `docs/local.md` files became one, the series'. The
notes-architecture skill learned the shape (*A series of projects*) and its audit counts
parent CLAUDE.md files in a tip and checks a whole series with `--series`.
Status: as built 2026-11-06, owner to confirm.

### D1.4 · 2026-11-06 · A new app is a script and a recipe
`tools/new_app.py <folder> "<Name>"` fills `templates/app/` into `apps/<folder>`: a module that
builds at once with a Stack page, the icon on the mat, and the notes skeleton; the template's
CLAUDE.md is stored as `CLAUDE.md.in` so it is not loaded as a live tip. The rest (key, notes,
phone) is `new-app.md`. Tried once with a throwaway app: built, linted, audit green, removed.
Status: as built 2026-11-06, owner to confirm.

### D1.5 · 2026-11-06 · Signing stays per app, in the app's folder
Each app reads `apps/<app>/local.properties` (the old repos' files copied over unchanged, so
no password was retyped); the root `local.properties` has only `sdk.dir`. Display Compare got
its own key before its first install. Status: as built 2026-11-06.

### D1.6 · 2026-11-07 · Every app updates itself from GitHub
Owner: "Make sure all of our apps have update capabilities to update from github." One module,
`:update`, for every app: the newest release tagged `<app>-v…` with an APK, downloaded and handed
to Android's installer after a package-and-key check (`architecture.md` *Updates*). A check at
most once a day when the app opens is on by default, with a switch; a banner on the page, a card
in Settings; Daily Note only the card, so its page stays paper. The apps' "no internet" lines
became "the only internet use: checking GitHub for updates".
Rejected: only a link to the releases page (the user still finds, downloads and opens the APK);
a silent install (Android allows it only for the app's installer of record, and these apps are
sideloaded); a check on every open (needless calls); a library (the API is one GET).
Status: as built 2026-11-07, owner to confirm (`VERIFY.md` *V-updates*).

### D1.7 · 2026-11-07 · Every release in one repo
Owner: "reorganize it into the one repo." Power Meter 0.1.0 and Daily Note 0.1.0 and 0.2.0
were copied from their first repos onto the same tags here, with the same APKs (so their
certificates are the ones on record); the first repos point here and are to be archived, not deleted
(old links keep working); the archive step was blocked by a permission check and waits on the
owner's OK. Every README and the series README link to this repo's releases.
Status: releases moved 2026-11-07; archive on the owner's OK.

### D1.8 · 2026-11-07 · The Theme icon in every app
Owner: "make sure the updated light/dark mode icon all of the apps get it". `ThemeIconButton()`
(made for Calculator's one-row header) is `StackHeader`'s default action, and the worded
*Theme: Auto* button is gone from `:stack`, so Power Meter and Display Compare show the icon
too (0.2.1 and 0.1.2, nothing else changed). Daily Note has no theme switch: its paper is
light whatever the system theme, so it gets no icon.
Status: built and released 2026-11-07 (Power Meter 0.2.1, Display Compare 0.1.2).

### D1.9 · 2026-11-07 · The app's name larger in a one-row header
Owner: "For all the apps that have the single row, make the name bigger than the other tabs
... That should be the bolded main tab font." `StackTabs(brand = true)` sets the first tab in
`StackType.h1` (Instrument Sans Bold 19 sp, the header title of the two-row apps) and the rest
in the 15 sp tab style; Calculator and Scan use it. The row is bottom-aligned so the accent
lines stay level. A compact tab's side padding went from 9 to 7 dp: measured with the fonts,
Calculator's row is then about 323 dp at the normal font size (a 360 dp phone has 328) and
about 390 dp at 130 % (the owner's Pixel has 409).
Status: built 2026-11-07, not seen on a screen (the Pixel was locked), not released.

### D1.10 · 2026-11-07 · Settings as a gear in a one-row header
Owner: "let's make settings to the settings icon". In Calculator and Scan the Settings tab is
gone; `SettingsIconButton` (a gear in the Theme icon's 40 dp box, both drawn on a canvas, no
icon library) sits right of the Theme icon, rightmost as settings usually are. Open, it is
filled with the soft accent and edged with the accent (the tabs then show none selected); a
second tap, or the app's name, goes back. Scan's row is then its name alone. Power Meter keeps
its Settings tab under its header (two rows, not asked).
Status: built 2026-11-07, not seen on a screen, not released.
