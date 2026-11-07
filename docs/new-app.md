# A new mini app

From an idea to an app on the owner's phone, with its notes, in one session. Each step says
where its result lives.

1. **Settle the idea with the owner** (questions in chat, short): the name; what it does in one
   line; the owner's own words for what it must and must not do (they become its hard rules);
   which devices. The folder is the name in lowercase letters (`displaycompare`).
2. **Scaffold**: `py -3.13 -I tools/new_app.py <folder> "<Name>"` makes `apps/<folder>/` from
   `templates/app/`: the module (found by `settings.gradle.kts` on its own), the series' frame
   (`ui/MainScreen.kt`: `StackTopBar`, the page, Settings behind the gear), `:update` wired in (put `UpdateCard()` in its settings and
   `UpdateBanner()` at the top of its page), the launcher icon on the mat, and the notes skeleton (CLAUDE.md,
   HANDOFF, TASKS with M1 and T1.1 ticked, architecture, decisions D1.1).
3. **Build it** (`./gradlew :<folder>:assembleDebug :<folder>:lintDebug`) before writing any
   code of its own: a red build now is the template's fault, not the app's.
4. **Its release key**, before the first install on a phone (so a test build and a later
   release replace each other; a key change later costs an uninstall and the app's data):
   ```
   keytool -genkeypair -keystore ~/.android/<folder>-release.jks -alias <folder> -keyalg RSA \
     -keysize 4096 -validity 10000 -dname "CN=<Name>"
   ```
   with a generated password, written only to `apps/<folder>/local.properties` (`sdk.dir` and
   the four `release.*` lines, `architecture.md` *Signing*). Copy the key and that file to the
   backup folder and add the key's row (cert SHA-256) there and to `docs/local.md`.
5. **Write the app** against `stack-android.md`: Stack components only, the page order, tiles
   with (i) tips, notes one line per point; phone width and ≥ 600 dp.
6. **Fill its notes** as the work goes: CLAUDE.md's three lines and hard rules,
   `architecture.md` *Goals* and screens, a decision per choice the owner didn't make
   (marked "as built, owner to confirm"), `VERIFY.md` *V-M1* for the owner's hand test.
7. **Series notes**: one line for the app in the series `docs/HANDOFF.md` *Apps*; anything the
   app taught that is true for every app goes to the series' gotchas or `stack-android.md`.
8. **On the phone**: foreground check, install, launch, screenshot; tell the owner it is there.
9. **Public** only when the owner asks: a README and LICENSE in `apps/<folder>/` and the
   publish skill (privacy scan first). The release is tagged `<folder>-v<version>` with
   `<Name>-<version>.apk`, or the app's updater won't see it; the root README gets its row.
