# Handoff (series)

Current state only. **Rewrite this file at the end of every session that worked at the series
level**: carry forward what is open, delete what is resolved, never append history.

**State (2026-11-06, ~9:30 pm):** The series was set up in one session (series M1): one repo
and Gradle build with Power Meter's and Daily Note's histories, `:stack` (the Stack look for
Compose) in every app, Display Compare as the third app, a new-app scaffold, and notes at two
levels. All three apps build, lint and are on the owner's Pixel as release-signed debug builds.
Committed: the import and the layout move only (4 commits, local; no remote). Everything
since is uncommitted.

## Apps (one line each; state is in each app's HANDOFF)
- `apps/powermeter`: Power Meter 0.1.0; M2 Alerts built, uncommitted; Stack look on.
- `apps/dailynote`: Daily Note 0.2.0; Stack look on its sheet and pill.
- `apps/displaycompare`: Display Compare, M1 built 2026-11-06, not released.

## Next
1. The owner looks at the Stack look (`VERIFY.md` *V-stack-look*) and Display Compare (its V-M1).
2. The owner's calls below; then commit (docs and code as separate commits, the skill's rule 11).

## Waiting on the owner
- D1.1: how the series meets GitHub, (a) one public repo for the series (recommended) or
  (b) a repo per app; and whether to recycle `C:\Projects\PowerMeter` and `DailyNote` (both
  untouched; PowerMeter's M2 work is copied here).
- D1.2–D1.4 as built: the Stack look in the apps, the two levels of notes, the scaffold.

## Uncommitted
Group as: (1) `:stack`, the version catalog, settings by folder, `tools/cut_fonts.py`;
(2) the Stack look in Power Meter and Daily Note; (3) Power Meter's M2 (carried over);
(4) Display Compare; (5) `tools/new_app.py` + `templates/app/`; (6) the notes, docs only.

## Watch out (this week only)
- The old folders still have their own CLAUDE.md: a session opened there works on a stale copy.
  Their CLAUDE.md files carry a "moved" line at the top since 2026-11-06.
- Installing over an app the owner has open closes it (it happened once today).
