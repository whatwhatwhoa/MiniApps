# Verify

Built but not hand-tested. One section per feature; delete a section when the owner confirms it.

## V-M1 — First light (2026-11-07)
Checked without the screen on 2026-11-07 (the Pixel was locked): a test receipt photo shared
to Scan came in upright (its EXIF turn applied) at 1800 × 2400, and the corners were found
within 5 px; the release build starts with no crash. Everything below is by hand.
- [ ] Empty: the mat says how to start; *Take photo* opens the camera, the shot comes back as
      page 1 with its crop found; *Choose photos* takes several at once.
- [ ] Share two photos from the gallery to Scan: they come in as the next pages.
- [ ] Crop: drag a corner: it doesn't jump, the magnifier top right shows the spot under the
      finger, and fades when the finger goes under it; with snap on, the dashed guides follow.
- [ ] Snap on: a tap away from the corners moves the nearest corner there. Snap off: one
      finger pans when zoomed. Two fingers zoom and move; − % + work; % fits again.
- [ ] *Auto-detect edges* finds a receipt on a table; *Use whole photo* takes the whole frame.
- [ ] *Apply crop*: the page straight on white; each filter does what its line says; Document
      takes out a shadow; *Turn left / right* turn it.
- [ ] Thumbnails show the cropped, filtered page; *Move left / right* (phone) and *Up / Down*
      (tablet) reorder; *Remove* asks first.
- [ ] *Export PDF*: *Save PDF* asks where, the file opens in a PDF viewer with the pages in
      order and the chosen page size; *Small* is visibly smaller than *High*.
- [ ] *Share* offers mail and the other apps; the PDF arrives attached.
- [ ] *Start a new scan* asks, then empties the pages; *Remove all pages* in Settings too.
- [ ] Close the app (swipe it away) and open it again: the pages, crops and filters are there.
- [ ] Turn the phone (≥ 600 dp): the page list sits beside the mat; nothing is cut off at 130 %.
- [ ] Dark and light themes: the mat, the sheet and the dialogs read well in both.
