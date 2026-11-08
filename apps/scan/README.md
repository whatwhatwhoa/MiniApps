# Scan

**Turn photos of receipts and documents into a clean PDF, on your phone.** Take or add photos,
let Scan find the paper's edges, fix the crop if you need to, pick a filter, and save or share
the PDF. Nothing is uploaded: the pages stay on your phone.

### [⬇ Download the APK](https://github.com/whatwhatwhoa/MiniApps/releases/tag/scan-v0.1.1)

Free, no ads, no account, no tracking. Android 12 or newer. About 2.5 MB. Part of the
[Mini Apps](../../README.md) series. Its only internet use is checking GitHub for its own updates.

---

## Install

Do this on the phone or tablet itself.

1. **Download.** Open the download link above and tap `Scan-….apk` under *Assets*.
2. **Install.** Open the download. If Android says your browser isn't allowed to install apps,
   tap **Settings**, switch on **Allow from this source**, go back, and tap **Install**. If
   Play Protect warns about an unknown app, tap **More details → Install anyway**.
3. **Open Scan** from your app list.

**Updating:** when a newer release is out, a note at the top says so: tap **Update to …**;
Android asks before it installs, and your pages stay. Settings → **Updates** has **Check for
updates** and the once-a-day check (switch it off to stop it).

## Find your way around

**Scan** at the top left (the number by it is how many pages you have), and two icons at the
right: the theme (Auto, Light or Dark) and the gear for Settings:
- **Scan:** *Take photo*, *Add photos* and *Export PDF* at the top; your pages as a row of
  thumbnails (a list beside the photo on a tablet); the open page, big, with its tools above it
  and *Previous* / *Next* under it.
- **Settings** (the gear; tap it or **Scan** to go back): crosshair snap, how much space your
  pages take, *Remove all pages*, updates, the filters, and how it works.

Each page has two views, chosen above it:
- **Crop:** the photo with the crop over it. *Auto-detect edges* looks again, *Use whole
  photo* takes the whole frame, *Crosshair snap* changes what a tap does.
- **Filter & review:** the finished page. Pick a filter, or *Turn left* / *Turn right* a page
  that lies sideways.

## How do I…

- **…scan a receipt?** **Take photo**, shoot the receipt flat on a table, and accept the shot
  in your camera app. Scan finds the edges; check the crop and tap **Apply crop**. The page
  comes out straight, cleaned up with the *Document* filter. **Export PDF** → **Save PDF**.
- **…scan several pages?** Take or add them one after another, in any order: each becomes the
  next page. **Add photos** takes several from your gallery at once, and photos shared to Scan
  from the gallery come in too. After **Apply crop**, **Next page** goes on to the next one.
- **…fix a crop?** In **Crop**, drag each corner onto the paper's corner: the magnifier at the
  top right shows what is under your finger. Pinch to zoom; the **%** button fits the photo
  again. With *Crosshair snap* on, a tap anywhere moves the nearest corner there.
- **…put the pages in order?** Tap a page, then **Move left** / **Move right** under the
  thumbnails (**Up** / **Down** in a tablet's list). The PDF keeps that order.
- **…send the PDF by email?** **Export PDF**, choose the page size and quality, then **Share**
  and pick your mail app. **Small** quality makes the smallest file.
- **…start over?** After an export, **Start a new scan** removes every page. **Remove** takes
  out one page; Settings → **Remove all pages** takes out all of them.

## Good to know

The five filters (one per page; *Document* is first on each new page):
- **Original:** exactly as photographed, just cropped and straightened.
- **Photo:** auto contrast and slightly richer color, for photos, logos and colored paper.
- **Document:** removes shadows and whitens the paper while keeping ink color; best for most
  receipts.
- **Grayscale:** the same cleanup in gray; smaller files.
- **B&W:** pure black and white; the crispest text and smallest files, but faint print can
  drop out.

And:
- **Page size:** *Fit to scan* (each page the size of its scan), *US Letter* or *A4* (the scan
  centered with a margin; a wide scan turns the page).
- **Quality:** *High* (larger file), *Standard* (sharp text, a moderate size), *Small* (email
  friendly).
- The PDF is named `scan-` and the date; change the *File name* before you save.
- Your pages stay in Scan, even when you close it, until you remove them. Scan keeps its own
  copy of each photo, at most 2400 px on its long side: a photo from your gallery stays in the
  gallery, but a photo taken with *Take photo* lives only in Scan.
- A PDF you saved stays where you saved it when you remove the pages.
- Scan doesn't read the text (no OCR); the PDF holds the pages as pictures.
- Scan needs no camera permission: your camera app takes the photo and hands it back.

## Building it yourself

Scan is built in the [Mini Apps](../../README.md) series repo. With JDK 21 and the Android SDK
(platform 37), from that repo's root:

```
./gradlew :scan:testDebugUnitTest      # the edge detection, filter and PDF tests
./gradlew :scan:assembleDebug
adb install -r apps/scan/build/outputs/apk/debug/scan-debug.apk
```

`./gradlew :scan:assembleRelease` gives a small unsigned APK; sign it with your own key to
install it.

Scan is open source under the [MIT License](../../LICENSE).
