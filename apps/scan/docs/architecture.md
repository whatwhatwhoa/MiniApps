# Architecture

## Goals
The owner's words: "port over scan app for receipts from NZFielddocs". The web Scan Stack
(NZFieldDocs `library/General/nz-scan-stack.html`, rev 1.0) turns phone photos of receipts
and documents into a PDF on the device; Scan does the same as a phone app, with what a phone
adds: the camera, the photo picker, Share in and out, and pages that wait in the app.

What it keeps from the web tool: the workflow (add, crop, filter and review, export), the edge
detection, the five filters and their wording, the page sizes and quality steps, the crop's
look (dimmed outside, amber corners, crosshair snap, the magnifier), the default file name.
What it leaves out: OCR, cloud anything, editing beyond crop / filter / turn, multi-select.

## Screens
The frame is the series': *Scan* as the header's one tab (larger and bold, series D1.9; its count is the pages), the
Theme icon and the Settings gear (series D1.10): the series' `StackTopBar` (D1.11); the
update banner.

**Scan**, under 600 dp, top to bottom: *Take photo · Add photos · Export PDF*; the pages as a
row of thumbnails (tap to open); the open page's row (*Page 2 of 5*, its filter, *Move left /
Move right / Remove*); the toolbar; the mat; *Previous · Next* and the step on (*Apply crop*,
then *Next page*, on the last page *Export PDF*). From 600 dp the pages are a list beside the
mat (290 dp), each with *Up / Down / Remove*, and the nav row shows *2 / 5*. With no pages the
mat says how to start, with *Take photo* and *Choose photos*.

- **Crop** view: the photo on the mat with the crop over it; the toolbar has *Auto-detect
  edges*, *Use whole photo* and the *Crosshair snap* switch. A finger near a corner (40 dp)
  drags it, keeping its offset; with snap on, a tap elsewhere moves the nearest corner there;
  with snap off one finger pans. Two fingers pinch and move (a corner being dragged goes back).
  While a corner is dragged, dashed guides cross the mat through it (snap on) and the
  magnifier, top right, shows the photo under it at up to 2.5 screen px per photo px, fading
  when the finger or the corner comes under it. The zoom bar (− · % · +) sits bottom right;
  % fits the photo again.
- **Filter & review** view: the processed page on white; the toolbar has *Turn left / Turn
  right* and the five filters as a segmented control, the open one's description under it.
- **Export PDF** sheet: file name (`scan-YYYY-MM-DD`), page size (*Fit to scan* at 200 dpi,
  *US Letter*, *A4*: centered, 24 pt margin, turned for wide scans), image quality (*High* 92,
  *Standard* 80, *Small* 60 JPEG); *Save PDF* (Android asks where), *Share* (the share sheet);
  after an export, *Start a new scan* (asks, then removes every page). The pages stay otherwise.

**Settings**: Crop (the snap switch, explained), Pages (count, the space their photos take,
*Remove all pages*), Updates, Filters (one line each), How it works, About.

## How a page is made
1. **In**: `ImageDecoder` opens the photo upright (EXIF turn applied), in sRGB, at most
   2400 px on its long side (the web tool's limit); it is kept as a JPEG (95) in the app's
   files, `pages/<id>.jpg`. The camera writes into the cache through the app's FileProvider
   and the shot is renamed before it is read, so the next shot can't overwrite it.
2. **Detection** (`Detect.kt`, pure Kotlin, a port of the web tool's): an area-averaged copy at
   most 512 px; four maps (brightness, brightness minus saturation, local contrast, minus the
   color gradient); for each, many thresholds (Otsu and percentiles, or gradient steps); every
   4-connected region above a threshold covering 2–85 % of the photo becomes its convex hull,
   cut down to four corners, scored by fill, hull fit, size and border contact; each candidate
   is then scored by the color step across its sides; the best one's sides are nudged onto the
   strongest step and the corners rebuilt from the lines. Nothing found: the photo less 4 %.
3. **Straighten**: Android's `Matrix.setPolyToPoly` maps the four corners onto an upright
   rectangle (the sides' mean lengths, at most 2200 px, at most 1.25× up) and Skia draws the
   photo through it bilinearly; a twisted quad falls back to its bounding box.
4. **Filters** (`Filters.kt`, pure Kotlin, ported): *Document* divides each pixel by the
   paper's brightness around it (block maxima on a grid of about 1/36 of the short side,
   spread and smoothed) and lifts the result through a gentle curve; *Grayscale* and *B&W*
   build on it (B&W at 150); *Photo* stretches the 1 % / 0.5 % ends and adds 15 % color.
5. **Turn**: quarter turns after the filter.
6. **PDF** (`Pdf.kt`, pure Kotlin): each page's JPEG embedded as it is (DCTDecode) in a PDF
   1.4 written as it goes, so one page is in memory at a time; numbers with a dot in every
   locale. Save writes to the document Android's picker returns; Share writes into the cache
   (`pdf/`, emptied by the next share) and hands a FileProvider link to the share sheet.

Thumbnails (240 px) are made the same way from a small copy. The open page's photo and its
processed result are kept in memory; everything heavy runs one job at a time on its own
thread. The pages, their crops, filters and turns, the open page and view live in
`pages/pages.json` (written whole, through a temporary file); the snap switch, page size and
quality in the app's preferences. A photo with no page is removed at the next start.

## Files
- `MainActivity.kt`: the activity; photos shared to it become pages.
- `Model.kt`: `ScanModel`, one per process: pages, selection, import, processing, export, saving.
- `Detect.kt`, `Filters.kt`, `Pdf.kt`: the core, pure Kotlin, tested in `CoreTest.kt`.
- `Imaging.kt`: bitmaps: decode, straighten + filter + turn, shrink, thumbnails.
- `ui/MainScreen.kt`: the frame; `ui/ScanPage.kt`: the Scan tab (pages, toolbar, nav);
  `ui/Stage.kt`: the mat, its drawing and gestures; `ui/ExportSheet.kt`; `ui/SettingsPage.kt`.
