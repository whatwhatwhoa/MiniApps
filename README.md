# Power Meter

**A plain, light battery power meter for Android phones and tablets.**
See what your device really takes in and draws: how fast a charger charges, what an app or
standby costs.

### [⬇ Download the latest APK](https://github.com/whatwhatwhoa/PowerMeter/releases/latest)

Free, no ads, no account, no tracking, no internet. Android 12 or newer. About 2 MB.

---

## Install in two minutes

Do this on the phone or tablet itself.

1. **Download.** Open this page in the device's browser, tap **Download the latest APK** above,
   then tap `PowerMeter-….apk` under *Assets*.
2. **Install.** Open the download. If Android says your browser isn't allowed to install apps,
   tap **Settings**, switch on **Allow from this source**, go back, and tap **Install**.
   If Play Protect warns about an unknown app, tap **Install anyway**.
3. **Open Power Meter** from your app list. It starts measuring at once.

**Updating:** download the new APK and install it over the old one. Your history and settings
stay.

## Find your way around

Three tabs: at the bottom on a phone, down the side on a tablet.

- **Now:** the live reading. The big number is the power going into the battery (green) or
  coming out of it (amber), with current, voltage and level under it, the time to full or
  empty, a 10-minute chart, the battery's details and the session in progress.
- **History:** the last 1 h, 6 h, 24 h or 7 days: power and level charts, each stretch on
  battery or on power, and how fast the battery drains with the screen off.
- **Settings:** background monitoring, how often the screen updates, °C or °F, the current
  reading, how long to keep history, export and clear.

## First steps

1. **Let it learn for a few minutes.** Unplug the device and leave Power Meter open (or turn on
   background monitoring) for a few minutes. It learns what the device uses by itself, which
   it needs to estimate a charger's power.
2. **Turn on background monitoring** if you want history: on the Now tab tap **Turn on**, or
   Settings → **Background monitoring**, and allow the notification when asked. A quiet
   notification then shows the live watts; **Stop** in it turns monitoring off.
3. **Plug in.** The Now tab shows the power going into the battery, an estimate of what comes
   through the cable, and the charger's rating.

## How do I…

- **…see how fast a charger is?** Plug in and watch the Now tab for a minute.
  - The big green number: what goes into the battery.
  - *From the cable ≈ …*: an estimate of what the charger delivers (see *Good to know*).
  - *charger max …*: what the charger says it can give.
  - *Plugged in, still draining*: the charger or cable can't keep up with the device.
- **…see what standby costs?** Turn on background monitoring and leave the device unplugged
  overnight. History → **Screen off** lists each screen-off stretch with its %/h and mA.
- **…see what an app or game costs?** With background monitoring on and the device unplugged,
  use the app for a while, then open History → **1 h**: the power chart shows what it drew.
  For a quick look, the Now tab's 10-minute chart and its min · avg · max do the same.
- **…compare charges?** History → **Sessions**: each charge with its levels, energy in, peak
  and duration.
- **…keep the numbers?** Settings → **Export CSV** saves the log as a spreadsheet file.

## Good to know

- **Measured vs estimated.** Battery power is measured: current × voltage at the battery, the
  net of what the charger gives and the device uses. Android doesn't let apps read the USB
  port's power, so the cable figure adds back the device's own draw (learned on battery) and
  is labeled as an estimate. Until it has learned, it says *not known yet*.
- **Odd readings?** Devices disagree on the current's unit (µA or mA) and on which way is
  "charging". The app learns both on its own. If charging shows as draining, or a number is
  1000× off, open Settings → **Current reading**: it shows the raw value and lets you set
  **Direction** and **Units** yourself, or **Learn again**.
- **It won't drain your battery.** Background monitoring holds no wake lock: while the device
  sleeps, readings pause, and the gap is bridged with the battery's own charge counter.
- **What's kept.** Samples for 1, 7 or 30 days (Settings → **Keep samples for**); sessions and
  screen-off stretches at least 90 days. Everything stays on the device; **Clear history**
  deletes it.

## Building it yourself

With JDK 21 and the Android SDK (platform 37):

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew assembleRelease` gives a small unsigned APK; sign it with your own key to install it.

Power Meter is open source under the [MIT License](LICENSE).
