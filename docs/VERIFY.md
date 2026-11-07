# Verify (series)

Built but not hand-tested, for changes that touch every app. One section per change; delete
it when the owner confirms. An app's own checks are in its `docs/VERIFY.md`.

## V-updates — updates from GitHub in every app (2026-11-07)
- [ ] Display Compare (the Pixel keeps a 0.1.0 test build for this): on opening, "Display
      Compare 0.1.1 is out" at the top; *Update to 0.1.1* asks once to allow installs from it,
      then downloads, Android's installer asks, and 0.1.1 opens with the displays kept.
- [ ] Daily Note (a 0.2.0 test build): the paper sheet → Updates offers 0.3.0; the note stays.
- [ ] Calculator (the 0.1.0 release): the note offers 0.1.1, which brings the one-row header.
- [ ] *Not now* hides the note; Settings → Updates still offers the version.
- [ ] Settings → Updates in each app: its version, *Check for updates* → "Up to date: … is the
      latest release."
- [ ] Airplane mode: *Check for updates* says "No connection to GitHub."
- [ ] *Check once a day* off: nothing is asked on opening.

## V-stack-look — the Stack look in every app (2026-11-06)
- [ ] Power Meter: header "Power Meter" with "monitoring on/off", tabs Now · History · Settings
      on top (no bottom bar); cards, segmented choices and buttons look like Display Compare's.
- [ ] Power Meter: charging green, draining orange, on Now and in the History charts.
- [ ] Power Meter on a tablet (≥ 600 dp): tabs on top, two columns.
- [ ] Daily Note: the page and widget unchanged; the paper sheet's labels, Size and Font
      choices and its notes in the Stack look; Handwriting still shown in its own letters.
- [ ] Theme: Auto / Light / Dark in Power Meter and Display Compare; the status bar icons stay
      readable in each.
- [ ] The fonts read well at 130%: Instrument Sans for text, JetBrains Mono for figures.
