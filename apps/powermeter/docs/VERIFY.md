# Verify

Built but not hand-tested. One section per feature; delete a section when the owner confirms it.

## V-M1 — First light (2026-11-01)
- [ ] Now: the watts look right while charging and on battery (green in, orange out).
- [ ] Unplugged with the app open a few minutes, then plugged in: "From the cable ≈" appears and
      sits below the charger max.
- [ ] Settings → Background monitoring on: notification permission asked, the notification shows
      the live reading, Stop turns monitoring off.
- [ ] Overnight on battery: History shows the screen-off stretch with %/h and mA.
- [ ] A charge from low: History shows the session with energy in and peak.
- [ ] Export CSV opens the save dialog and the file has rows.
- [ ] Phone width: bottom tabs, one column, nothing cut off.
- [ ] Reboot with monitoring on: the notification comes back.

## V-M2 — Alerts (2026-11-04)
- [ ] Settings → Alerts → Add alert: the editor fits the phone; Save makes a line like
      "Level ≥ 80% while plugged in"; tapping the line edits it, Delete removes it.
- [ ] Saving the first alert with monitoring off turns monitoring on and asks for notifications.
- [ ] Quick test: "Level ≥ 1% any time, at once" → an alert notification (sound, heads-up) at once.
- [ ] Unplug for over a minute, then plug in: "Charging ≤ 100 W for 30 s" fires about 90 s after
      plugging in (the first minute doesn't count).
- [ ] On battery with the screen on: "Drain ≥ 0.5 W for 1 min" fires after a minute.
- [ ] A fired alert doesn't repeat while still breached; "Last alert" shows its time.
- [ ] Display → °F: a temperature alert's line shows °F.
