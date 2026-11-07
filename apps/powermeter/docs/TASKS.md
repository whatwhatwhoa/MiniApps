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
- [x] T1.6 VERIFY round with the owner — 2026-11-07: settled on the owner's word, not hand-tested

### M2 — Alerts (owner, 2026-11-04)
Goal: notifications when the level, charging power, drain or temperature passes a limit, held
for a chosen time. Decisions: `decisions/M2-alerts.md`.
- [x] T2.1 Alerts in Settings — 2026-11-04: rules, checked by background monitoring (D2.1–D2.4)
- [x] T2.2 VERIFY round with the owner — 2026-11-07: settled on the owner's word, not hand-tested

### M3 — Readings on the chart (owner, 2026-11-07)
Goal: read any point of a chart, a steadier figure under the big number, and 0.2.0.
Decisions: `decisions/M3-readout.md`.
- [x] T3.1 Chart readout — 2026-11-07: tap or drag sideways, bubble and readout line, History shared (D3.1)
- [x] T3.2 Rolling average, 10 s, set in Settings (owner, mid-session) — 2026-11-07 (D3.2)
- [x] T3.3 Updates from GitHub (series D1.6) — 2026-11-07: note on top, card in Settings
- [x] T3.4 Release 0.2.0 — 2026-11-07: series repo, `powermeter-v0.2.0` (D3.3)
- [ ] T3.5 VERIFY round with the owner (`VERIFY.md` *V-M3*).

### Later
- Home-screen widget or Quick Settings tile with the live watts.
- Per-charger records: remember peak and average per charger rating.
- Temperature on the history chart.

## Done (one line each)
- 0.1.0 released on GitHub with a signed APK, download link and user guide in the README —
  2026-11-01 (owner's ask; D1.6); copied to the series repo 2026-11-07 (series D1.7)
- 0.2.0 released in the series repo — 2026-11-07 (D3.3)
