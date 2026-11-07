# Mini Apps

**Small, plain Android apps for phones and tablets.** Free, no ads, no accounts, no tracking.
Each does one job; all of them share one look.

| App | What it does | Download | Guide |
|---|---|---|---|
| **Display Compare** | Compare display sizes to scale: monitors, TVs, laptops, tablets, phones | [⬇ APK](https://github.com/whatwhatwhoa/MiniApps/releases?q=Display+Compare&expanded=true) | [README](apps/displaycompare/README.md) |
| **Power Meter** | See the watts going into or out of your battery: charging speed, drain, history, alerts | [⬇ APK](https://github.com/whatwhatwhoa/PowerMeter/releases/latest) | [README](apps/powermeter/README.md) |
| **Daily Note** | One page of paper to jot the day on, and a full-page widget | [⬇ APK](https://github.com/whatwhatwhoa/DailyNote/releases/latest) | [README](apps/dailynote/README.md) |

Android 12 or newer. Power Meter's and Daily Note's releases so far are in their first repos
(linked above); new releases of every app come out here.

## Install

1. On your phone, open the app's download link and tap its **…apk** under *Assets*.
2. Open the downloaded file. If Android asks, allow installs from your browser (*Allow from
   this source*), then go back and tap **Install**.
3. If Play Protect warns that it doesn't know the app, tap **More details → Install anyway**.

To update an app, install a newer APK over the old one; its data stays.

## Building them yourself

With JDK 21 and the Android SDK (platform 37), from this folder:

```
./gradlew :displaycompare:assembleDebug      # one app (powermeter, dailynote, displaycompare)
./gradlew assembleDebug                      # every app
```

The APKs land in `apps/<app>/build/outputs/apk/`. Kotlin and Jetpack Compose; the shared look is
the `stack` module.

## License

[MIT](LICENSE). The fonts are under the SIL Open Font License: Instrument Sans and JetBrains
Mono (`stack/licenses/`), and Daily Note's Patrick Hand (`apps/dailynote/licenses/`).
