# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-01, pm):** M1 *First light* built (T1.1–T1.5); public on GitHub under MIT.
**0.1.0 released** on the owner's ask: GitHub release `v0.1.0` with a signed
`PowerMeter-0.1.0.apk` (its own key, D1.6), and the README now opens with a download link,
install steps and a short user guide. The signed release (R8) ran on the TCL phone: it opened
in 0.6 s, readings right, no crash; left installed there. The Yoga Tab still has the debug
build (different key: uninstall it before installing a release; that deletes its history).
README screenshots (Now charging, Now on battery, Settings; TCL phone, app area only) in
`docs/screenshots/`. Lint: the same two
style notes (UseKtx, DataExtractionRules).

## Next
1. The owner tries it (`VERIFY.md` V-M1): turn on background monitoring, unplug for a few
   minutes so the cable estimate learns, then plug in.
2. T1.6: phone-width check, now possible on the TCL phone (it has 0.1.0).
3. A History screenshot for the README once monitoring has logged a day (it was empty).

## Waiting on the owner
- Back up the release key with its passwords (`docs/local.md` says where); lose it and no
  update can install over 0.1.0.
- D1.1–D1.6 as built (`decisions/M1-first-light.md`); the cable estimate's method (D1.4) most.

## Uncommitted
Nothing: the release commit and tag `v0.1.0` are on GitHub.

## Watch out (this week only)
- Someone else uses the TCL phone (a video was playing at 1:28 pm): check
  `dumpsys activity activities | grep topResumedActivity` before launching anything there.
