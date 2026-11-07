# M1 — First light

### D1.1 · 2026-11-07 · Package, name and toolchain
`io.github.whatwhatwhoa.scan`, app name *Scan* (the web tool's *Scan Stack* without the
series' word; the owner called it "scan app"), made from the series template: the series'
toolchain, `:stack` and `:update`, no other library; JUnit for the core's tests only. Its own
release key, made before the first install. The launcher name can change without the package.
Status: as built 2026-11-07, owner to confirm the name.

### D1.2 · 2026-11-07 · A port: the web tool's code, line for line where it matters
The detection, the filters, the page sizes and qualities and the wording are the web tool's,
ported to Kotlin with the same constants, so a receipt crops and cleans up the same in both.
The pure parts (`Detect.kt`, `Filters.kt`, `Pdf.kt`) have no Android imports, so they are
tested on the PC. Rejected: Android's document scanner in Google Play services (a library,
and it sends nothing anywhere but is not the owner's tool); OpenCV (a large library).
Status: as built 2026-11-07, owner to confirm.

### D1.3 · 2026-11-07 · The PDF is written by the app, not by Android's PdfDocument
jsPDF stays on the web; on Android a 150-line writer embeds each page's JPEG as it is, so the
quality steps mean what they say and the file stays small. Android's `PdfDocument` was
rejected: it draws bitmaps through Skia and doesn't let an app choose how they are encoded,
so the quality steps couldn't promise a size. Status: as built 2026-11-07, owner to
confirm.

### D1.4 · 2026-11-07 · The straightening uses Android's perspective matrix
The web tool solves the homography and samples each pixel itself; Scan gives the same four
corners to `Matrix.setPolyToPoly` and lets Skia draw (bilinear, the same sampling), which is
faster and less code. Status: as built 2026-11-07.

### D1.5 · 2026-11-07 · What the phone adds: camera, picker, Share in and out
*Take photo* hands off to the camera app (no camera permission), *Add photos* opens the
photo picker (no storage permission), photos shared to Scan from the gallery become pages (the
web tool's drop and paste), and the export sheet has *Share* beside *Save PDF*, for a receipt
going straight to mail. Status: as built 2026-11-07, owner to confirm.

### D1.6 · 2026-11-07 · Pages wait in the app until removed
The web tool forgets its pages when the tab closes; on a phone the camera app often takes the
app's place in memory, so Scan keeps each photo (at most 2400 px, JPEG 95) and the crops in
its own files until a page is removed, and the pages stay after an export. *Start a new
scan* after an export and *Remove all pages* in Settings clear them, each after a question;
removing one page asks too (a camera shot exists only in Scan). Status: as built 2026-11-07,
owner to confirm.

### D1.7 · 2026-11-07 · Touch: crosshair snap stays on by default
As in the web tool, snap is on: a tap anywhere moves the nearest corner there, which is quick
with a finger; two fingers zoom and move. With snap off one finger pans. The corner a finger
grabs keeps its offset (it doesn't jump under the finger) and the magnifier shows what the
finger hides. Grab radius 40 dp, handles 11 dp (14 dp while dragged). Status: as built
2026-11-07, owner to confirm the default.

### D1.8 · 2026-11-07 · Filters in the series colors; rotate and arrows in words
The web tool's own filter colors become the series slots, each fixed to its filter and always
beside its name: Original s0, Photo s1, Document s2, Grayscale sOther, B&W s4. On a phone the
filters are a segmented control (the web chips are under the 34 dp touch size), and ⟲ ⟳ ↑ ↓
✕ are words (*Turn left*, *Up*, *Remove*): the cut Stack fonts may not hold those signs.
Status: as built 2026-11-07, owner to confirm.
