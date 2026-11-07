# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-07):** 0.2.0 is the release (versionCode 2), in the series repo (tag
`powermeter-v0.2.0`, `PowerMeter-0.2.0.apk`, 2.43 MB, cert 7760ce6a…c869 as 0.1.0's); 0.1.0 was
copied there from the first repo, now archived. 0.2.0 carries the Stack look, M2 alerts, M3 (a
readout on every chart by touch, a rolling average under the big number, 10 s by default) and
updates from GitHub (series D1.6). On the owner's Pixel: the 0.2.0 release (a cold start ran clean;
the phone was locked, so the screen wasn't seen). The TCL phone still has
the signed 0.1.0 without the updater.

## Next
1. The owner tries M3 (`VERIFY.md` *V-M3*).
2. New README screenshots: `docs/screenshots/` still show the look before the Stack one.

## Waiting on the owner
- D3.1, D3.2 as built: the cursor stays until Clear; the plain mean for the average.

## Uncommitted
Nothing (M2 and the Stack look committed and pushed to the series repo 2026-11-06).
