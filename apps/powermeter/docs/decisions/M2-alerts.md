# M2 — Alerts

### D2.1 An alert is one rule, in Settings
Owner, 2026-11-04: "alerts setting that allows users to receive notifications based on certain
criterias, like current percentage, maximum or minimum power, and if the limit breached over a
specificed time duration." Each alert: what it watches (battery level, charging power, drain,
battery temperature), at or above / at or below a value, for at least a time (seconds or
minutes, 0 = at once), and for level and temperature when it counts (any time, plugged in, on
battery). Power alerts carry their state: charging = plugged in, drain = on battery, so there is
no "drain while plugged in" nonsense to choose. Temperature is an addition the owner didn't name
(cheap, and useful while charging). "At or" (≥, ≤) so "80%" fires at 80, not 81. Stored as JSON
in Prefs (`org.json`, no library). Rejected: separate max/min fields per metric (two numbers to
explain, and one direction is usually wanted); a fourth tab (the owner asked for a setting).
Status: as built 2026-11-04, settled 2026-11-07 on the owner's word ("settle all the verify on power meter"): temperature alerts stay.

### D2.2 "For at least" means on every reading, then once per breach
The condition must hold on each reading through the whole time; one reading back inside the
limit restarts the clock. A fired alert shows again only after its condition has been off for a
minute in a row, or after it is edited. Rejected: the average over the time (steadier with noisy
gauges, but harder to explain); repeating while breached (nags). Asked 2026-11-04 whether the
average is better; settled 2026-11-07 on the owner's word ("settle all the verify on power meter"): every reading stays.

### D2.3 Checked by background monitoring, still no wake lock
Alerts run in `MonitorService`, so they need background monitoring; saving or switching on an
alert turns it on. The meter still never wakes the device (D1.3): readings pause in sleep.
Level and temperature change slowly, so a hit before and after a sleep counts as held; power
doesn't, so a power alert's clock restarts after a gap over 30 s (it counts awake time only).
The battery's own broadcasts wake the device on level changes, so alerts are also checked
inside the broadcast. Rejected: an exact alarm or a wake lock to check while asleep (adds the
drain it reports). Status: as built 2026-11-04.

### D2.4 Charging power ignores what isn't slow charging
Full, or held at a limit or by heat (`BATTERY_STATUS_NOT_CHARGING`), doesn't count for a
charging-power alert: those aren't a weak charger. The first minute after the plug changes
doesn't count for power alerts either, since the gauge's averaged current lags (as `Battery`'s
learning does). Charging slowing down near full by itself still counts, and the editor says so.
Status: as built 2026-11-04.

### D2.5 Debug builds signed with the release key
The owner's Pixel had the signed 0.1.0 from GitHub; a debug build with the debug key can't
install over it without wiping the history. As in Daily Note, debug builds on this PC are now
signed with the release key when `local.properties` names it (D1.6), so test builds and
releases replace each other. Status: as built 2026-11-04.
