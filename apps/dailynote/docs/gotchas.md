# Gotchas

### Line height in sp drifts with a large system font
Compose resolves an `sp` line height, under Android 14+'s nonlinear font scaling, as the
font size × (line sp / font sp): not the pixels asked for. At 130% the lines came out ~20%
taller than the paper's rules. Give the line height in `em` of the actual font size (D1.3).

### An em line height is of the size Compose draws, not of sp × a factor
Under nonlinear font scaling a larger sp grows less: at 130%, 18 sp draws at 21.6 dp but
20.7 sp (18 × 1.15) at 24.1 dp, not 24.8. Handwriting's line height, in em of the assumed
size, came out 128 px against 135 px rules on Large: the ink crept up 7 px a line. Get the
font size from the pixels (`px.toSp()`) and the em from that size's own `toPx()` (D1.3).

### A widget row keeps what it doesn't set
The launcher lays each update's rows over recycled row views, so a row that only sets the ink
when there is ink stays `GONE` where a blank line was: new lines never showed. Every row sets
everything it shows, visibility included (D1.6).

### Android 16+ inlines a widget's list, and keeps it
`setRemoteAdapter(Intent)` lists are fetched once by the system (binding the service from the
caller's context) and packed into the widget's own bundle, bitmaps and all; later updates and
`notifyAppWidgetViewDataChanged` didn't refetch, so the widget never showed edits. Give the
rows directly (`RemoteCollectionItems`) in every update. Bitmaps count once per object: one
bitmap per line made 12.4 MB, one strip shared by every line is stored once (D1.6).

### A widget can't use the app's fonts
RemoteViews has no typeface setter, and the launcher ignores `android:fontFamily="@font/..."`
in a widget's layout (the text came out in the system sans). Draw the ink in the app (D1.6).

### Force-stop kills the widget's taps
`am force-stop` cancels every PendingIntent the app made, so the widget's lines don't respond
until the app runs again (opening it refreshes the widget). Test cold starts with `am kill`.

### Wireless adb on the Pixel
A new PC must pair first: Wireless debugging → *Pair device with pairing code* shows a
`_adb-tls-pairing` service in `adb mdns services`; `adb pair <ip:port> <code>`, then
`adb connect` to the `_adb-tls-connect` port. The phone locks quickly; screenshots of a locked
screen come back black.

### `cmd appwidget` is not there
`adb shell cmd appwidget …` says "No shell command implementation"; the old
`adb shell appwidget grantbind --package <pkg> --user 0` works.

### Bash: backslashes and long commands
The Bash tool collapses `\\` to `\` (also inside quoted Python), and commands over ~9 kB fail
to parse: edit files with the Edit tool or one heredoc per call.
