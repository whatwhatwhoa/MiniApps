# Handoff (series)

Current state only. **Rewrite this file at the end of every session that worked at the series
level**: carry forward what is open, delete what is resolved, never append history.

**State (2026-11-07, ~noon):** Five apps, every release in the one public repo
(`whatwhatwhoa/MiniApps`): Scan 0.1.1, Power Meter 0.2.2, Calculator 0.1.2, Daily Note 0.3.0,
Display Compare 0.1.3, plus the old releases copied from the first repos. The owner's new rule
(rule 3, D1.11): one layout, look and feel, inherited from `:stack`. Every Stack app's top is
`StackTopBar` (the name as the first tab, bold and larger, D1.9; its tabs; the Theme icon,
D1.8; the Settings gear, D1.10), and the new-app template starts with it; Daily Note keeps its
paper page. None of the top bars has been seen on a screen. Every app updates itself from the
releases (`:update`, D1.6). On the owner's Pixel: Scan 0.1.1, Calculator 0.1.2 and Power
Meter 0.2.2 installed by adb (the phone was locked: not opened), Display Compare 0.1.0 and
Daily Note 0.2.0 as test builds with the updater, for the update test. The README now opens
with a gallery: a card per app (`docs/gallery/`, `architecture.md` *Gallery*), shot on the TCL
phone from `.shot` debug copies with demo content.

## Apps (one line each; state is in each app's HANDOFF)
- `apps/scan`: Scan 0.1.1 released 2026-11-07, not hand-tested (receipt photos to a PDF: crop, filters, Save or Share; the top bar).
- `apps/powermeter`: Power Meter 0.2.2 released 2026-11-07 (the top bar: Power Meter · History; chart readout, rolling average, alerts, updates).
- `apps/calculator`: Calculator 0.1.2 released 2026-11-07 (formula typing, history, own functions; the top bar).
- `apps/dailynote`: Daily Note 0.3.0 released 2026-11-07 (Stack sheet, updates; its paper page, no top bar).
- `apps/displaycompare`: Display Compare 0.1.3 released 2026-11-07 (the top bar; How it measures and Updates behind the gear).

## Next
1. The owner's update test on the Pixel (`VERIFY.md` *V-updates*): Display Compare there is a
   0.1.0 test build and Daily Note a 0.2.0 one, so they should offer 0.1.3 and 0.3.0.
2. The owner looks at the top bar in every app and the rest of *V-stack-look*, then the apps'
   own V- lists; Scan's (`apps/scan/docs/VERIFY.md` *V-M1*) wants real receipts.

## Waiting on the owner
- Decisions as built, for the owner's word: D1.2–D1.4 (the Stack look, notes at two levels,
  the scaffold), D1.6 (the daily check on, the banner, Daily Note without one), D1.11 (Power
  Meter without "monitoring on/off" at the top).
- Archive `whatwhatwhoa/PowerMeter` and `DailyNote` (T1.14): a permission check stopped it;
  their README line "The releases here stay" should first say the releases moved (the moved
  releases' notes already call the first repos archived).
- Updates for PortalLauncher and Tablet Debloat too (*Later* in TASKS)?

## Uncommitted
Nothing.

## Watch out (this week only)
- Installing over an app the owner has open closes it.
- A release without the `<app>-v<version>` tag or the `.apk` is invisible to the apps.
