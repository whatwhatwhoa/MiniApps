# Calculator

**A calculator you type formulas into.** The whole formula stays on screen with a cursor you can
move, the answer shows under it as you type, every answer goes into a history, and you can make
your own functions.

### [⬇ Download the latest APK](https://github.com/whatwhatwhoa/MiniApps/releases?q=Calculator&expanded=true)

Free, no ads, no account, no tracking. Android 12 or newer. About 2.5 MB. Part of the
[Mini Apps](../../README.md) series. Its only internet use is checking GitHub for its own updates.

---

## Install

Do this on the phone or tablet itself.

1. **Download.** Open the download link above and tap `Calculator-….apk` under *Assets* (the
   top release is the newest).
2. **Install.** Open the download. If Android says your browser isn't allowed to install apps,
   tap **Settings**, switch on **Allow from this source**, go back, and tap **Install**. If
   Play Protect warns about an unknown app, tap **More details → Install anyway**.
3. **Open Calculator** from your app list.

**Updating:** when a newer release is out, a note at the top says so: tap **Update to …**;
Android asks before it installs, and your history and functions stay. Settings → **Updates**
has **Check for updates** and the once-a-day check (switch it off to stop it).

## Find your way around

Three tabs along the top:
- **Calculator:** your history at the top, the formula under it, a strip of functions, the keys.
- **Functions:** the functions you made, and the editor for new ones.
- **Settings:** degrees or radians, history, updates, and how a formula is read.

The keys that edit the formula:
- **◀ ▶** move the cursor (or tap anywhere in the formula).
- **⌫** deletes what is before the cursor, **Del** what is after it. Hold either to keep going.
- **Clear all** empties the formula. Your history stays.
- **=** works it out and puts the line in history.

## How do I…

- **…work something out?** Type it as you would write it: `2(3+4)`, `√2`, `sin(30)`, `5!`. The
  answer shows under the formula as you type; **=** keeps it.
- **…go on from an answer?** After **=**, type an operator (**+ − × ÷**, **xʸ**, **x²**): it goes
  on from the answer. Type a number to start a new formula instead. **ans** in the strip is the
  last answer anywhere in a formula.
- **…use an old answer?** Tap it in the history: it goes in at the cursor. Hold it to bring its
  whole formula back and change it.
- **…fix something in the middle?** Tap there in the formula (or use **◀ ▶**), then **⌫** or
  **Del**, and type.
- **…make my own function?** Functions → **New function**. Give it a **Name** (`tip`), the
  **Values it takes** (`bill, pct`) and a **Formula** (`bill * pct / 100`); **Try it** with
  `80, 15` shows 12; **Save**. It is first in the strip above the keys: tap it, type
  `80, 15`, and **=**. No values makes a named number (`g` = `9.80665`). **Add four examples**
  shows how.
- **…copy an answer?** Hold the formula: its answer is copied.

## Good to know

- Brackets you leave open close themselves; the faint `)` at the end shows where.
- No × is needed before a bracket, a name or π: `2π`, `3(4 + 1)`, `2sin(30)`.
- `200 + 10%` is 220 (10% of 200 added) and `200 − 10%` is 180; `10%` alone is 0.1.
- `−2²` is −4, as in math; `(−2)²` is 4. `2^3^2` is 2^9.
- Answers keep 12 significant digits, so 0.1 + 0.2 is 0.3. Very large or small answers show
  as `×10^`, and read back in as the same number.
- **Deg / Rad** in the strip (or Settings → Angles) sets the unit for sin, cos, tan and their
  inverses; the header says which is on.
- A keyboard works too: `*` `/` `-` for × ÷ −, Enter for =, Esc for Clear all.
- Your formula, history (the last 200) and functions stay on the phone, even when you leave
  the app.

## Building it yourself

Calculator is built in the [Mini Apps](../../README.md) series repo. With JDK 21 and the
Android SDK (platform 37), from that repo's root:

```
./gradlew :calculator:testDebugUnitTest      # the formula engine's tests
./gradlew :calculator:assembleDebug
adb install -r apps/calculator/build/outputs/apk/debug/calculator-debug.apk
```

`./gradlew :calculator:assembleRelease` gives a small unsigned APK; sign it with your own key to
install it.

Calculator is open source under the [MIT License](../../LICENSE).
