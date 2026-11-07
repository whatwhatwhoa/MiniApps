# Gotchas (series)

Traps that cost a session and are true for every app: the toolchain, adb, the devices, this
PC's shell. One entry per trap; an app's own traps are in its `docs/gotchas.md`.

### Wireless adb on the Pixel
A new PC must pair first: Wireless debugging → *Pair device with pairing code* shows a
`_adb-tls-pairing` service in `adb mdns services`; `adb pair <ip:port> <code>`, then
`adb connect` to the `_adb-tls-connect` port. The phone locks quickly; screenshots of a locked
screen come back black.

### Screenshots with a monitor attached
With an external display `screencap` prints a warning into the PNG and picks a display:
pass `-d <id>` from `dumpsys SurfaceFlinger --display-id`.

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

### Gradle refuses a module whose folder is missing
`include(":x")` with `projectDir` pointing at a folder that doesn't exist fails the whole
build. `settings.gradle.kts` therefore finds apps by folder instead of listing them.
