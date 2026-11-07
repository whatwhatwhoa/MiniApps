# Tasks

Rules: one task per session unless the owner bundles several; work on the first unchecked task
under *Open*, or the owner's explicit ask; tick with the date and one clause, then its detail
goes to `decisions/` and the line stays one line. Owner bug/tweak batches are B1, B2, …
Past the foundation this becomes a feature board (the owner's preference), when the owner says.

## Open (in order)

### M1 — First light (owner, 2026-11-01)
Goal: a decent, plain power meter for the owner's Android phones and tablets: live watts,
history, and what charging and standby really cost. Decisions: `decisions/M1-first-light.md`.
- [x] T1.1 Scaffold — 2026-11-01: PortalLauncher's toolchain, Material 3, no other libraries (D1.5)
- [x] T1.2 Readings — 2026-11-01: one sign and unit convention, learned per device (D1.2)
- [x] T1.3 Now, History, Settings — 2026-11-01: rail and two columns from 600 dp (D1.5)
- [x] T1.4 Background monitoring — 2026-11-01: service, sessions, screen-off drain, CSV (D1.3)
- [x] T1.5 Cable estimate (owner's ask) — 2026-11-01: charger rating + estimated cable power (D1.4)
- [ ] T1.6 VERIFY round with the owner (`VERIFY.md` *V-M1*), on the tab and a phone.

### Later
- Home-screen widget or Quick Settings tile with the live watts.
- An alert at a chosen level (e.g. 80%) to unplug.
- Per-charger records: remember peak and average per charger rating.
- Temperature on the history chart.

## Done (one line each)
- 0.1.0 released on GitHub with a signed APK, download link and user guide in the README —
  2026-11-01 (owner's ask; D1.6)
