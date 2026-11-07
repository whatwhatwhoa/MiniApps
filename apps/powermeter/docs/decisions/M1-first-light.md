# M1 — First light

### D1.1 Package, toolchain, min SDK
`io.github.whatwhatwhoa.powermeter`, minSdk 31 (Android 12, as the owner's other apps), target
37, PortalLauncher's AGP/Kotlin/Gradle versions. Status: as built 2026-11-01, owner to confirm.

### D1.2 One convention for current, learned per device
Signed µA, + into the battery. Auto learns the unit (a value ≥ 20 000 can only be µA; 30
screen-on readings on battery all under it means mA) and the sign (on battery current can only
flow out; 10 votes decide). Until learned, the sign follows Android's charging status. Rejected:
trusting the spec (wrong on many devices); asking the user up front. Status: as built 2026-11-01.

### D1.3 No wake lock in the background
The service samples every 5 s while awake and lets the device sleep; the charge counter bridges
sleep gaps for session energy, and screen-off stretches are measured from the screen events.
Rejected: a partial wake lock for steady sampling, which would add to the standby drain it
reports. Status: as built 2026-11-01.

### D1.4 The cable's power is an estimate
Owner, 2026-11-01: "is it possible to see it estimate the total power pulling from the usbc
cable?" Apps can't read the USB input (`gotchas.md`), so: (battery power + the device's own
draw, learned on battery by screen state) / 0.9 for charging losses, labeled "≈" with its
working shown, beside the charger's negotiated maximum from the battery broadcast. Weak spots:
the own draw differs with what's on screen, and 90% is a typical, not measured, efficiency.
Status: as built 2026-11-01, owner to confirm.

### D1.5 Plain Material 3, no other libraries
Owner: "doesn't need to be fancy. quick light, and decent font/layout". Stock Material 3 with
the wallpaper's colors (dynamic color), Roboto with tabular figures, own vector icons (no icon
pack), SQLite without Room, tabs without a navigation library. Release APK ~1.9 MB.
Status: as built 2026-11-01, owner to confirm. The look (wallpaper colors, Roboto, bottom bar
and rail) superseded 2026-11-06 by series D1.2: the Stack look from `:stack`.

### D1.6 Release signing: a key of its own, outside the repo
Owner, 2026-11-01: publish with an APK download link up front and a short user guide. An
unsigned APK can't be installed, so 0.1.0 is signed with a new 4096-bit RSA key for this app
alone (as PortalLauncher has its own), kept in the user's `.android` folder; its path and
passwords are in the gitignored `local.properties` (`release.*`), and without them a release
build is left unsigned. Every later release must use the same key, or installs over it fail:
compare `apksigner verify --print-certs` with the last release's asset. Asset name
`PowerMeter-<version>.apk` on the GitHub release; the README links to `releases/latest`.
Status: as built 2026-11-01; the owner backs up the key.
