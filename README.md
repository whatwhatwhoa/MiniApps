# Mini Apps

Small, plain Android apps for phones and tablets, built together: one Gradle build, one look
(the Stack design, in the shared `:stack` module), one set of notes.

| App | What it does | Folder |
|---|---|---|
| Power Meter | Battery power in and out, history, sessions, screen-off drain, alerts | `apps/powermeter` |
| Daily Note | One page of paper to jot on, and a full-page widget | `apps/dailynote` |
| Display Compare | Display sizes compared to scale: monitors, TVs, laptops, tablets, phones | `apps/displaycompare` |

Each app's own README (where it has one) says how to install and use it.

## Build
JDK 21 and the Android SDK; from this folder:

```
./gradlew :displaycompare:assembleDebug      # one app
./gradlew assembleDebug                      # every app
```

A new app: `py tools/new_app.py <folder> "<Name>"` (see `docs/new-app.md`).

Kotlin, Jetpack Compose and Material 3. The Stack fonts (Instrument Sans, JetBrains Mono) and
Daily Note's Patrick Hand are under the SIL Open Font License (`stack/licenses/`,
`apps/dailynote/licenses/`).
