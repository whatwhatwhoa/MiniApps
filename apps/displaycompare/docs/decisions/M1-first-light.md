# M1 — First light

The owner, 2026-11-06: "I want a new mini app. This one is similar https://www.displaywars.com/
in that it is used to compare display sizes. Your task is to design this app, and push it to my
android phone." Mid-session: "families of preset, so phones, tablets, laptops, and monitors. Of
course you always go custom. Also add iphone ratio in there, as that is pretty common as well."

### D1.1 · 2026-11-06 · Name, package, toolchain, key
*Display Compare* (the owner's words for it), `io.github.whatwhatwhoa.displaycompare`, the
series' toolchain and `:stack`, no other library; release APK 2.26 MB. Its own release key from
the start, so the test build and a release replace each other. Status: shipped 0.1.0
2026-11-06 (owner's /publish); the name still the owner's to confirm.

### D1.2 · 2026-11-06 · The shape comes from the pixels when they are known
Pixels are square on every display worth comparing, so a resolution fixes the shape exactly;
"21:9" ultrawides are really 2.37:1 (2560 × 1080) or 2.39:1 (3440 × 1440). A named shape that
disagrees with the pixels is allowed and flagged, not corrected.
Rejected: a custom W:H field (a resolution says the same, more exactly).
Status: as built 2026-11-06, owner to confirm.

### D1.3 · 2026-11-06 · Diagonal in inches, results in in or cm
Displays are sold by the inch even where cm are the unit, so the diagonal is typed in inches;
the Units choice switches width, height and area (and shows the diagonal's cm). Status: as
built 2026-11-06, owner to confirm.

### D1.4 · 2026-11-06 · Up to four displays, each with a fixed letter and color
Four series colors stay told apart and the table still fits a tablet. A display keeps its slot
(letter, color) until removed. The tiles compare two at a time. Status: as built 2026-11-06,
owner to confirm.

### D1.5 · 2026-11-06 · On a phone the mat stays put
The header and the mat don't scroll, so a typed size shows at once on the drawing; the rest
scrolls under them. A Stack deviation (the guide puts tiles before the chart), for the phone.
Status: as built 2026-11-06, owner to confirm.

### D1.6 · 2026-11-06 · Presets in families, Custom always there (owner)
Phones, Tablets, Laptops, Monitors as the owner listed, plus TVs (the presets were there
already; drop on the owner's word). A sheet with a tab per family, opened on the display's
family; This phone at the top of Phones; phones portrait. Custom drops the preset's name and
keeps the numbers; every field stays editable after any preset. Monitor and TV presets use the
viewable diagonal, phones, tablets and laptops the maker's figure. Product names (iPhone,
Pixel, Galaxy, iPad, MacBook) only where a size is that product's; the rest are generic.
Status: as built 2026-11-06, owner to confirm TVs staying.

### D1.7 · 2026-11-06 · 19.5:9 and 20:9 shapes (owner: the iPhone ratio)
19.5:9 is the iPhones' since the X (2556 × 1179 and kin); 20:9 was added beside it for most
Android phones (the owner's Pixel is 2992 × 1344, 20:9). Status: 19.5:9 the owner's ask; 20:9
as built 2026-11-06, owner to confirm.

### D1.8 · 2026-11-06 · This phone is measured, and says so
Pixels ÷ dpi from Android, for the full panel; rounded to 0.1″. On the Pixel 11 Pro XL it reads
6.7″ against Google's 6.8″ (makers measure the uncut rectangle). The note under the app says
so. Status: as built 2026-11-06.
