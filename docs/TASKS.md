# Tasks (series)

Series work only: `:stack`, the toolchain, the notes, a change to every app, a new app's
start. An app's own work is in `apps/<app>/docs/TASKS.md`. Rules: one task per session unless
the owner bundles several; tick with the date and one clause, the detail to `decisions/`.

## Open (in order)

### M1 — The series (owner, 2026-11-06)
Goal: one folder for the mini apps, the Stack look in each, notes that work at the series and
the app level, and a quick start for the next app. Decisions: `decisions/M1-series.md`.
- [x] T1.1 One repo and build, histories imported — 2026-11-06: subtree, apps flattened (D1.1)
- [x] T1.2 `:stack` — 2026-11-06: tokens, fonts, theme, components (D1.2)
- [x] T1.3 Stack look in Power Meter and Daily Note — 2026-11-06: theme, frame, controls (D1.2)
- [x] T1.4 Display Compare, the third app — 2026-11-06: its own M1 (`apps/displaycompare`)
- [x] T1.5 New-app scaffold and recipe — 2026-11-06: `tools/new_app.py`, `new-app.md` (D1.4)
- [x] T1.6 Notes at two levels, the skill's series support — 2026-11-06 (D1.3)
- [ ] T1.7 VERIFY round with the owner (`VERIFY.md` *V-stack-look*).
- [x] T1.8 Public — 2026-11-06: one series repo (owner's pick), old repos point to it (D1.1)
- [x] T1.9 Recycle `C:\Projects\PowerMeter` and `DailyNote` — 2026-11-07: keys and passwords checked in the backup first
- [x] T1.10 Updates from GitHub in every app (owner) — 2026-11-07: `:update`, card and banner (D1.6)
- [x] T1.11 Calculator, the fourth app (owner) — 2026-11-07: its own M1 (`apps/calculator`)
- [x] T1.12 Every release in the one repo (owner) — 2026-11-07: old releases copied, READMEs link here (D1.7)
- [ ] T1.13 VERIFY round with the owner (`VERIFY.md` *V-updates*).
- [ ] T1.14 Archive the first repos, on the owner's OK: their README line first, then archive (D1.7).
- [x] T1.15 Scan, the fifth app, ported from NZFieldDocs' Scan Stack (owner) — 2026-11-07: its own M1 (`apps/scan`), 0.1.0 released
- [x] T1.16 The Theme icon in every app (owner) — 2026-11-07: `StackHeader`'s default; Power Meter 0.2.1, Display Compare 0.1.2 (D1.8)

### Later
- √, π and ⌫ in the cut Stack fonts (`tools/cut_fonts.py`): they come from Android's fallback
  font now (the Calculator draws its ⌫ and arrows).
- Updates in PortalLauncher and Tablet Debloat (own repos): `:update` takes a repo name.
- One command for the release routine (bump, build, sign check, tag) instead of five steps.
- Tablet Debloat (`C:\Projects\DebloatYourAndroidTablet`) could join the series, if the owner
  wants it here.
- A `:stack` table component, once a second app needs the one Display Compare draws.

## Done (one line each)
