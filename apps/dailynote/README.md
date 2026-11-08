# Daily Note

**One page of paper on your phone.** Tap a line and write; swipe across words to scratch them
out. A yellow pad, college ruled, a legal pad, grid or dot grid, and the same page as a widget
on your home screen. That's all it is: one page, no extras.

### [⬇ Download the APK](https://github.com/whatwhatwhoa/MiniApps/releases/tag/dailynote-v0.3.0)

Free, no ads, no account, no tracking. Android 12 or newer. About 2 MB. Part of the
[Mini Apps](../../README.md) series. Its only internet use is checking GitHub for its own updates.

---

## Install in two minutes

Do this on the phone itself.

1. **Download.** Open this page in the phone's browser, tap **Download the APK** above,
   then tap `DailyNote-….apk` under *Assets*.
2. **Install.** Open the download. If Android says your browser isn't allowed to install apps,
   tap **Settings**, switch on **Allow from this source**, go back, and tap **Install**.
   If Play Protect warns about an unknown app, tap **Install anyway**.
3. **Open Daily Note** from your app list.

**Updating:** the settings button → **Updates** → **Check for updates**, then **Update to …**;
Android asks before it installs, and your note stays. It also checks once a day when you open
the settings (switch **Check once a day** off to stop that). Installing a newer APK over the
old one by hand works too.

## Find your way around

- **The page:** your note, on paper. It scrolls, and always runs a few lines past your text.
- **The settings button** (the little sheet of paper, top right):
  - **Paper:** Yellow pad, College, Legal pad, Grid or Dot grid.
  - **Size:** Small, Medium or Large. The lines get wider with the text.
  - **Font:** Normal or Handwriting.
  - **Swipe to erase:** on or off.
  - **Updates:** this version, **Check for updates**, and the once-a-day check.

## How do I…

- **…write?** Tap a line. On a line with words, the cursor goes where you tapped; on an empty
  line further down, it goes to that line.
- **…erase?** Swipe sideways across words, quickly. Every word you touch is scratched out,
  fades, and the gap closes up. Swipe across a whole line and the lines below move up.
  Changed your mind? Tap **Undo** in the *Erased* message at the bottom.
- **…erase it all?** Swipe across each line. There is no "clear all" button on purpose.
- **…put it on my home screen?** Touch and hold an empty spot on your home screen, tap
  **Widgets**, find **Daily Note**, and drag it onto the screen. Stretch it as big as you like,
  up to a whole screen. Tap a line on the widget to write on it in the app.

## Good to know

- **Swiping vs. scrolling.** Only a quick sideways swipe that starts on a line with words
  erases. Up and down still scroll, and touch-and-hold still selects text. If you'd rather never
  erase by swiping, turn off **Swipe to erase**.
- **Empty lines at the end clear themselves**, so the page doesn't grow by itself.
- **The widget** shows your note when you leave the app. To change it, tap it and write in the
  app (a home screen can't hold a text box).
- **Where your note is kept:** on the phone only, in the app's own storage, saved as you type.
  It goes along with Android's own backup if you use it. Uninstalling the app deletes it.

## Building it yourself

Daily Note is built in the [Mini Apps](../../README.md) series repo. With JDK 21 and the Android SDK
(platform 37), from that repo's root:

```
./gradlew :dailynote:assembleDebug
adb install -r apps/dailynote/build/outputs/apk/debug/dailynote-debug.apk
```

`./gradlew :dailynote:assembleRelease` gives a small unsigned APK; sign it with your own key to install
it. The papers are drawn in code as vector shapes (`Paper.kt`); there are no image files.

Daily Note is open source under the [MIT License](LICENSE). The handwriting font is
[Patrick Hand](https://github.com/google/fonts/tree/main/ofl/patrickhand) by Patrick
Wagesreiter, under the [SIL Open Font License](licenses/PatrickHand-OFL.txt).
