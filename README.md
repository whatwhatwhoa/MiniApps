# Power Meter

**A plain, light battery power meter for Android phones and tablets.**
See what your device really takes in and draws: how fast a charger charges, what an app or
standby costs.

Free, no ads, no account, no tracking, no internet. Android 12 or newer. Early days (0.1.0):
no downloadable APK yet, build it yourself (below).

## What it shows

- **Now:** watts in or out, current, voltage and level, time to full or empty, a 10-minute
  chart, the battery's details (temperature, health, cycle count), and the session in progress.
- **Cable power (estimated):** while plugged in, the charger's rated maximum and an estimate of
  what comes through the cable.
- **History:** 1 h, 6 h, 24 h or 7 days of power and level, sessions on each power source
  (energy in or used, peak), and screen-off drain in %/h.
- **Background monitoring** (optional): logs while the app is closed, with a quiet notification
  showing the live watts. Holds no wake lock, so the meter doesn't cause the drain it measures.
- **Export** the log as CSV from Settings.

Works at phone width (bottom tabs) and on tablets (side rail, two columns).

## Good to know

- **Measured vs estimated.** Battery power is measured: current × voltage at the battery, the
  net of what the charger gives and the device uses. Android doesn't let apps read the USB
  port's power, so the cable figure adds back the device's own draw (learned while on battery)
  and is labeled as an estimate. Until the device has run on battery for a few minutes with the
  meter on, it says "not known yet".
- **Odd readings?** Devices disagree on the current's unit (µA or mA) and sign. The app learns
  both on its own; Settings → *Current reading* shows the raw value and lets you set them.
- While the device sleeps, readings pause; the gap is bridged with the battery's charge counter.

## Building it yourself

With JDK 21 and the Android SDK (platform 37):

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew assembleRelease` gives a small unsigned APK; sign it with your own key to install it.

Power Meter is open source under the [MIT License](LICENSE).
