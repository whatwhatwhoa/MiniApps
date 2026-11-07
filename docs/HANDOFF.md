# Handoff (series)

Current state only. **Rewrite this file at the end of every session that worked at the series
level**: carry forward what is open, delete what is resolved, never append history.

**State (2026-11-07, ~11 am):** Five apps, every release in the one public repo
(`whatwhatwhoa/MiniApps`): Scan 0.1.0 (new, T1.15), Power Meter 0.2.1, Calculator 0.1.1,
Daily Note 0.3.0, Display Compare 0.1.2, plus the old releases copied from the first repos.
The Theme icon is in every app with a theme switch (T1.16, D1.8: `StackHeader`'s default;
Power Meter 0.2.1 and Display Compare 0.1.2 are only that). Every app updates itself from
those releases (`:update`, D1.6). On the owner's Pixel: Scan 0.1.0 and Power Meter 0.2.1
installed by adb (the phone was in use, then locked: not opened), Calculator 0.1.1 (it was
0.1.0: updated by the app or by hand, not known), Display Compare 0.1.0 and Daily Note 0.2.0
as test builds with the updater, for the update test.

## Apps (one line each; state is in each app's HANDOFF)
- `apps/scan`: Scan 0.1.0 released 2026-11-07, not hand-tested (receipt photos to a PDF: crop, filters, Save or Share).
- `apps/powermeter`: Power Meter 0.2.1 released 2026-11-07 (the Theme icon; 0.2.0: chart readout, rolling average, alerts, updates).
- `apps/calculator`: Calculator 0.1.1 released 2026-11-07 (formula typing, history, own functions; one-row header).
- `apps/dailynote`: Daily Note 0.3.0 released 2026-11-07 (Stack sheet, updates; no theme switch: paper is light).
- `apps/displaycompare`: Display Compare 0.1.2 released 2026-11-07 (the Theme icon; 0.1.1: updates).

## Next
1. The owner's update test on the Pixel (`VERIFY.md` *V-updates*): Display Compare there is a
   0.1.0 test build and Daily Note a 0.2.0 one, so they should offer 0.1.2 and 0.3.0.
2. The owner looks at the Stack look (*V-stack-look*, now with the Theme icon) and the apps'
   own V- lists; Scan's (`apps/scan/docs/VERIFY.md` *V-M1*) wants real receipts.

## Waiting on the owner
- D1.6 as built: the daily check on by default, the banner, Daily Note without one.
- D1.2–D1.4 as built: the Stack look in the apps, the two levels of notes, the scaffold.
- Archive `whatwhatwhoa/PowerMeter` and `DailyNote` (T1.14): a permission check stopped it;
  their README line "The releases here stay" should first say the releases moved (the moved
  releases' notes already call the first repos archived).
- Updates for PortalLauncher and Tablet Debloat too (*Later* in TASKS)?

## On main, not released
T1.17 and T1.18 (D1.9, D1.10): in Calculator and Scan the name is larger and bold, and
Settings is a gear beside the Theme icon. Pushed 2026-11-07, not seen on a screen;
Calculator 0.1.2 and Scan 0.1.1 when the owner asks.

## Uncommitted
Nothing.

## Watch out (this week only)
- Installing over an app the owner has open closes it.
- A release without the `<app>-v<version>` tag or the `.apk` is invisible to the apps.
