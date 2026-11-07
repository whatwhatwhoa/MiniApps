# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `decisions/` hold it).

**State (2026-11-07, ~noon):** 0.1.1 released (versionCode 2): GitHub release `scan-v0.1.1`
in the series repo with `Scan-0.1.1.apk` (2.49 MB, cert 509fad7f…2caa, Scan's own key). 0.1.1
is the series' top bar (series D1.9–D1.11): *Scan* larger and bold with the page count, the
Theme icon, Settings as a gear. 0.1.0 was the first release, public with a README and a user
guide (T1.4). M1 is the NZFieldDocs Scan Stack ported (T1.2): photos in by camera, picker or
share; crop with the edges found (the web tool's detection, line for line); five filters; PDF
by Save or Share, written by the app. 10 core tests pass. Not hand-tested: on the Pixel only
a screenless check has run (`VERIFY.md` *V-M1*, first lines). The Pixel has 0.1.1, installed
by adb, not opened (the phone was locked).

## Next
1. T1.3: the owner's hand test with real receipts (`VERIFY.md` *V-M1*); what it finds is a
   0.1.x patch.

## Waiting on the owner
- The name *Scan* (D1.1): the package and the release tags stay `scan` either way; the
  launcher name, the README and the asset name can change.
- D1.2–D1.8 as built: the port's choices, the pages kept in the app, snap on by default, the
  filter colors.

## Uncommitted
Nothing.
