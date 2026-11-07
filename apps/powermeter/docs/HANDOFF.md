# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-07):** 0.2.2 is the release (versionCode 4), in the series repo (tag
`powermeter-v0.2.2`, `PowerMeter-0.2.2.apk`, 2.43 MB, cert 7760ce6a…c869 as before). 0.2.2 is
the series' top bar (series D1.11): *Power Meter · History* (the name's tab is the old Now),
the Theme icon, Settings as a gear; "monitoring on/off" is no longer at the top. 0.2.1 brought
the Theme icon; 0.2.0 the Stack look, M2 alerts, M3 (a readout on every chart by touch, a
rolling average under the big number, 10 s by default) and updates from GitHub (series D1.6);
0.1.0 was copied to the series repo from the first repo (to be archived on the owner's OK). On
the owner's Pixel: 0.2.2, installed by adb, not opened (the phone was locked). The TCL phone
still has the signed 0.1.0 without the updater.

## Next
1. The owner tries M3 (`VERIFY.md` *V-M3*) and the top bar (series *V-stack-look*).
2. New README screenshots: `docs/screenshots/` still show the look before the Stack one.

## Waiting on the owner
- D3.1, D3.2 as built: the cursor stays until Clear; the plain mean for the average.

## Uncommitted
Nothing.
