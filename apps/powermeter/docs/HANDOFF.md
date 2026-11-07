# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-04, ~5 pm):** 0.1.0 is the release on GitHub. **M2 Alerts built** (T2.1, owner's
ask): Settings → Alerts, rules on level, charging power, drain or temperature, at or above /
below a value, for at least a time, checked by background monitoring (D2.1–D2.4). Debug builds
are now signed with the release key (D2.5). The test build is on the owner's **Pixel 11 Pro
XL** over USB (it had 0.1.0; history kept); the owner was trying the editor live. Seen working
at 130% font: the panel and the editor. Not yet seen: an alert actually firing. Lint: the
same two style notes (UseKtx, DataExtractionRules). APK size not checked for a release build.

## Next
1. The owner tries alerts (`VERIFY.md` V-M2); quick test "Level ≥ 1% any time" fires at once.
2. The owner's V-M1 round (T1.6) is still open.
3. A release (0.2.0) only when the owner asks.

## Waiting on the owner
- D2.2: "for at least" means held on every reading; the average over the time is the
  alternative (steadier with a noisy gauge). Asked 2026-11-04.
- D2.1: temperature alerts were added beyond the ask; keep or drop.
- Back up the release key with its passwords (`docs/local.md` says where).
- D1.1–D1.6 as built (`decisions/M1-first-light.md`); the cable estimate's method (D1.4) most.

## Uncommitted
All of M2: `Alerts.kt`, `ui/AlertsPanel.kt`, the Settings, service, Format and Prefs hooks,
debug signing in `app/build.gradle.kts`, and these notes.

## Watch out (this week only)
- The Pixel and the TCL phone are daily devices: check
  `dumpsys activity activities | grep topResumedActivity` before launching anything.
- Wi-Fi adb to the Pixel failed on 2026-11-04 (PC and phone on different networks); USB works
  with USB debugging on.
