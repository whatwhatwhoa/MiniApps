# Gotchas

### Apps can't read the USB port's power
`/sys/class/power_supply/*` is denied to apps (SELinux), even under `run-as`; on the Yoga Tab
the shell couldn't read the files either. Android gives only the battery's net current. Hence
the cable figure is an estimate (`architecture.md` *Readings*).

### CURRENT_NOW's unit and sign vary by device
Specified as µA, + while charging; many devices send mA, many flip the sign. Auto mode learns
both (`Battery.learn`); Settings → Current reading overrides. If a device reads 1000× off or
backwards, check the raw value shown there first.

### Screenshots with a monitor attached
With an external display `screencap` prints a warning into the PNG and picks a display:
pass `-d <id>` from `dumpsys SurfaceFlinger --display-id`.

### Bash heredocs over ~9 kB
One Bash call writing several notes files at once failed to parse; write files with the Write
tool or one heredoc per call.
