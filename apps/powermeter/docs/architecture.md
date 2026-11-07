# Architecture

## Goals
The owner wants to see what a phone or tablet really draws and takes in: how fast a charger
charges, what an app or standby costs. Plain and light beats feature-rich: three screens, stock
Material 3, the device's wallpaper colors, tabular figures so numbers don't jiggle.

## Readings
`Battery.read()` takes one `Reading` from `BatteryManager` (CURRENT_NOW, CHARGE_COUNTER, the
time-to-full estimate) and the sticky `ACTION_BATTERY_CHANGED` intent (level, voltage, status,
plug, temperature, health, cycle count on Android 14+, the charger's max current and voltage).
Current is normalized to signed µA, + into the battery. Devices disagree on unit (µA or mA) and
sign; in Auto, `Battery` learns both from what the device does and stores the verdicts in Prefs;
Settings can force either and shows the raw value. Voltage given in V or µV is rescaled to mV.

Power = current × voltage at the battery: a net figure. While charging it is the charger's
output minus what the device uses and loses. The cable estimate adds those back:
(battery power + the device's own draw) / 0.9. The own draw is a ~2-minute moving average of
the power on battery, kept separately for screen on and off, learned whenever the meter runs
on battery and kept in Prefs. Until it is learned the cable figure says "not known yet". The
charger's rating (max current × max voltage from the broadcast's extras) is shown beside it.

## Sampling
`Monitor` is the one sampler: a coroutine loop that runs while a client needs it. The screen
samples every 1, 2 or 5 s (setting); the background service alone every 5 s. A battery
broadcast or a new client takes a reading at once. Readings feed `Monitor.live` (the Now
screen), `Monitor.recent` (the last 10 minutes, preloaded from the log when the service has
been running) and a flow the service logs from.

## Background monitoring
`MonitorService` is a foreground service (specialUse) with a low-importance notification: the
live watts, level, cable estimate, current, voltage, temperature, and a Stop action. It restarts
after boot and after an app update when the setting is on. It holds no wake lock: while the
device sleeps readings pause, and the gap is bridged with the charge counter.

`Tracker` turns readings into the log, in SQLite (`Store`, no library):
- **samples**: time-weighted 5 s averages (current, voltage, power, level, temperature, state);
  kept 1, 7 or 30 days (setting); exported as CSV from Settings.
- **sessions**: one per stretch on a power source (charge or on battery): levels, energy in or
  used, peak, screen-off time. An open session resumes if the service restarts within 15 min.
- **screen_off**: each screen-off stretch over a minute: level and charge drop, %/h, average
  mA, and whether power was connected (then it isn't a drain figure).

Sessions and screen-off rows are kept at least 90 days.

## Alerts
`Alerts` holds the user's rules (JSON in Prefs) and checks each reading the service sees, plus
one taken inside each battery broadcast, since a sleeping device wakes for those. A rule watches
the level (%), charging power (W, plugged in only), drain (W, on battery only) or battery
temperature (°C, shown in °F when set), at or above / at or below a value, for at least a time;
level and temperature rules can be limited to plugged in or on battery. Charging power doesn't
count when full or held at a limit, and power doesn't count in the first minute after the plug
changes (the gauge lags). The condition must hold on every reading for the whole time; a gap
over 30 s between readings restarts a power rule's clock (asleep it isn't measured), while level
and temperature bridge it. A rule fires once, a high-importance notification on its own
"Alerts" channel, and again only after its condition has been off for a minute. Alerts need
background monitoring, and turning one on turns monitoring on.

## Screens
- **Now**: direction and source, the big watts (green in, amber out), current · voltage · level,
  the cable estimate when plugged in, time to full or empty, the 10-minute chart with
  min/avg/max, the battery's details, and the session in progress (or a card to turn
  monitoring on).
- **History**: 1 h / 6 h / 24 h / 7 d; power and level charts (a gap where nothing was logged),
  totals for the range, the sessions, the screen-off stretches. Refreshes every 30 s.
- **Settings**: monitoring, update rate, alerts (list with switches, an editor dialog, a note
  when notifications or monitoring are off), °C/°F, current direction and units (with the raw
  reading and "Learn again"), history length, export, clear, about.

Under 600 dp: bottom tabs and one column; from 600 dp: a rail and two columns.
