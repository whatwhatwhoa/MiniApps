# Handoff (series)

Current state only. **Rewrite this file at the end of every session that worked at the series
level**: carry forward what is open, delete what is resolved, never append history.

**State (2026-11-07, ~5 am):** Four apps, every release in the one public repo
(`whatwhatwhoa/MiniApps`): Power Meter 0.2.0, Calculator 0.1.0 (new), Daily Note 0.3.0,
Display Compare 0.1.1, plus the old releases copied from the first repos (now archived). Every
app updates itself from those releases (`:update`, series D1.6). The old local folders
`C:\Projects\PowerMeter` and `DailyNote` are in the Recycle Bin (T1.9; keys and passwords
checked in the backup first). All four are on the owner's Pixel.

## Apps (one line each; state is in each app's HANDOFF)
- `apps/powermeter`: Power Meter 0.2.0 released 2026-11-07 (chart readout, rolling average, Stack look, alerts, updates).
- `apps/calculator`: Calculator 0.1.0 released 2026-11-07 (formula typing, history, own functions).
- `apps/dailynote`: Daily Note 0.3.0 released 2026-11-07 (Stack sheet, updates).
- `apps/displaycompare`: Display Compare 0.1.1 released 2026-11-07 (updates).

## Next
1. The owner's update test on the Pixel (`VERIFY.md` *V-updates*): Power Meter there is a 0.1.0
   test build, so it should offer 0.2.0.
2. The owner looks at the Stack look (*V-stack-look*) and the apps' own V- lists.

## Waiting on the owner
- D1.6 as built: the daily check on by default, the banner, Daily Note without one.
- D1.2–D1.4 as built: the Stack look in the apps, the two levels of notes, the scaffold.
- Updates for PortalLauncher and Tablet Debloat too (*Later* in TASKS)?

## Uncommitted
Nothing.

## Watch out (this week only)
- Installing over an app the owner has open closes it.
- A release without the `<app>-v<version>` tag or the `.apk` is invisible to the apps.
