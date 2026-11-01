# Handoff

Current state only. **Rewrite this file at the end of every session**: carry forward what is
open, delete what is resolved, never append history (git and `docs/decisions/` hold it).

**State (2026-11-01, ~12:45 am):** M1 *First light* built in one session (T1.1–T1.5): the Now,
History and Settings screens, background monitoring with a notification, sessions, screen-off
drain, CSV export, and (owner's mid-session ask) the charger's rating plus an estimate of the
cable's power. Debug build installed and running on the **Yoga Tab** (owner: "push it to my
lenovo yoga tab"); seen on its own screen in the tablet layout, charging ~11–13 W, charger max
19.8 W. Background monitoring is **off** there (not turned on by the agent). Not tried on a
phone yet. Lint clean but for two style notes (UseKtx, DataExtractionRules). On the owner's
ask the project is public on GitHub (`whatwhatwhoa/PowerMeter`) under the MIT license, with a
short README; no GitHub release or APK there yet. Scrubbed first with the publish skill:
commits use the GitHub noreply identity, this PC's paths and device IDs live in the gitignored
`docs/local.md`, and a pre-push hook runs the privacy scan.

## Next
1. The owner tries it (`VERIFY.md` V-M1): turn on background monitoring, unplug for a few
   minutes so the cable estimate learns, then plug in.
2. T1.6: phone-width check on the TCL phone, if the owner wants it there.

## Waiting on the owner
- D1.1–D1.5 as built (`decisions/M1-first-light.md`); the cable estimate's method (D1.4) most.
- Release signing, and when to put a release with the APK on GitHub.

## Uncommitted
Nothing: the first commit is on GitHub (`main`).

## Watch out (this week only)
- The Yoga Tab had an LG monitor attached at the end of the session: screenshots need `-d`.
