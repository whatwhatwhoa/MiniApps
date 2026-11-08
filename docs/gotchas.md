# Gotchas (series)

Traps that cost a session and are true for every app: the toolchain, adb, the devices, this
PC's shell. One entry per trap; an app's own traps are in its `docs/gotchas.md`.

### Wireless adb on the Pixel
A new PC must pair first: Wireless debugging → *Pair device with pairing code* shows a
`_adb-tls-pairing` service in `adb mdns services`; `adb pair <ip:port> <code>`, then
`adb connect` to the `_adb-tls-connect` port. The phone locks quickly; screenshots of a locked
screen come back black.

### A test launch on the owner's phone leaves the app on top
An app started over the lock screen (`am start`) stays the top task after `am force-stop`
(it is still first in recents), so the owner unlocks into it. `pm clear <package>` (when
its data may go) removed the task; check `dumpsys activity recents` afterwards.

### `MSYS_NO_PATHCONV=1` stops every path conversion
It keeps `/sdcard/...` and `/data/local/tmp/...` intact for adb, but a `/c/Projects/...` APK
path is then passed as it is and `adb install` can't find it: give adb `C:/...` paths.

### Screenshots with a monitor attached
With an external display `screencap` prints a warning into the PNG and picks a display:
pass `-d <id>` from `dumpsys SurfaceFlinger --display-id`.

### Black screenshots on the TCL phone
`adb exec-out screencap -p > shot.png` came back black there; `shell screencap -p /sdcard/x.png`
and `pull` works. A frame taken during a launch, a rotation or a swipe is black too: wait a
second and take it again. A swipe that starts within ~60 px of the left edge is Android's Back.

### Sharing a photo to an app from adb
`am start -a android.intent.action.SEND --eu android.intent.extra.STREAM content://media/...
--grant-read-uri-permission` opened Scan without the photo (the shell can't grant that URI):
push the picture, then pick it in the app's own photo picker.

### Bash: backslashes and long commands
The Bash tool collapses `\\` to `\` (also inside quoted Python), and commands over ~9 kB fail
to parse: edit files with the Edit tool or one heredoc per call.

### Python from a Bash heredoc loses non-ASCII
`py - <<'EOF'` reads the script through the console code page: ″, • and → in its string
literals came out as `?` and an exact-match edit failed. Write the script to a file with the
Write tool and run the file. Python also wants `C:/...` paths, not Git Bash's `/c/...`.

### `--` in an XML comment breaks the resource merge
A comment naming a Stack token (`<!-- Stack --bg -->`) failed `mergeDebugResources`: XML
forbids `--` inside comments. Write "the Stack bg token".

### A property's setter and a `setX()` clash on the JVM
`var units by mutableStateOf(…)` with `private set` next to `fun setUnits(u)` is a
"platform declaration clash". Name the function for what it does (`chooseUnits`).

### `maxWidth` of BoxWithConstraints inside a nested lambda
Reading `maxWidth` inside a composable lambda nested in the scope is a DSL scope violation;
read it once into a `val` at the top of the scope.

### A local named `size` hides the draw scope's
Inside `drawWithContent { }` or `Canvas { }`, `size.width` meant the canvas, but a `val size`
(a font size) in the composable shadowed it: "Function invocation 'width(...)' expected". Name
such locals for what they are (`fontSize`).

### Gradle refuses a module whose folder is missing
`include(":x")` with `projectDir` pointing at a folder that doesn't exist fails the whole
build. `settings.gradle.kts` therefore finds apps by folder instead of listing them.
