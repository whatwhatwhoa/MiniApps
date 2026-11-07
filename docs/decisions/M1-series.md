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
Status: as built 2026-11-06, owner to confirm; the GitHub shape and recycling the old folders
waiting on the owner.

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
