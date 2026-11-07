# Power Meter — app rules (keep this file short; it loads with the series' CLAUDE.md)

A plain, light battery power meter for phones and tablets: watts in or out, current, voltage,
an estimate of the cable's power, a live chart, history, sessions, screen-off drain, alerts.
Public on GitHub in the series repo (MIT), with every release (its first repo points here;
series D1.7).
Package `io.github.whatwhatwhoa.powermeter`, module `:powermeter`. Why and how:
`docs/architecture.md`. The series' rules, commands and look: `../../CLAUDE.md`.

## Read before working
| Task | Read |
|---|---|
| Any | `docs/HANDOFF.md` (state, next, waiting on the owner) |
| Pick or plan work | `docs/TASKS.md`: the first unchecked task under *Open*, or the owner's explicit ask |
| How readings, logging, screens work | `docs/architecture.md` |
| Why something is the way it is | `docs/decisions/INDEX.md`, then the one entry |
| Odd device readings, Android limits | `docs/gotchas.md` |
| Hand-testing with the owner | `docs/VERIFY.md` |

## Hard rules (this app; the series' rules apply too)
1. **Say what is measured and what is estimated.** Battery power is measured; the cable figure
   is an estimate and is labeled so (`decisions/M1-first-light.md` D1.4).
2. **No wake lock.** The meter must not cause the drain it measures (D1.3).

## Commands (this app; from the series root)
- Build + lint: `JAVA_HOME="$HOME/AppData/Local/Android/jdk-21" ./gradlew :powermeter:assembleDebug :powermeter:lintDebug`
- Run: `adb -s <dev> shell am start -n io.github.whatwhatwhoa.powermeter/.MainActivity`
- Release asset name: `PowerMeter-<version>.apk`; the public README carries the download link
  and a user guide (the publish skill's *Shipping an app*).
