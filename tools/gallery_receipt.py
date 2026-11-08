"""A made-up sample receipt 'photographed' on a desk, for Scan's gallery shot.

    py -3.13 -P -E tools/gallery_receipt.py <out.jpg>

Push it to the phone and add it in Scan with Choose photos. Needs Pillow and numpy.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import numpy as np

out = sys.argv[1]
font_dir = Path(__file__).resolve().parent.parent / "stack" / "src" / "main" / "res" / "font"
mono = ImageFont.truetype(str(font_dir / "jetbrains_mono_regular.ttf"), 34)
monob = ImageFont.truetype(str(font_dir / "jetbrains_mono_medium.ttf"), 46)

# The receipt itself, flat.
W, H = 620, 1180
rc = Image.new("RGB", (W, H), (250, 249, 244))
d = ImageDraw.Draw(rc)
y = 60
def center(t, f, y):
    w = d.textlength(t, font=f)
    d.text(((W - w) / 2, y), t, font=f, fill=(40, 40, 44))
center("CORNER MARKET", monob, y); y += 70
center("12 Harbor Street", mono, y); y += 46
center("Sample receipt", mono, y); y += 80
items = [("Coffee beans", "14.50"), ("Oat milk", "3.20"), ("Bananas 1.2 kg", "3.84"),
         ("Sourdough loaf", "6.00"), ("Tomatoes", "4.75"), ("Basil", "2.50"),
         ("Lemons x4", "2.80")]
for name, price in items:
    d.text((50, y), name, font=mono, fill=(40, 40, 44))
    w = d.textlength(price, font=mono)
    d.text((W - 50 - w, y), price, font=mono, fill=(40, 40, 44))
    y += 50
y += 20
d.line((50, y, W - 50, y), fill=(60, 60, 64), width=2); y += 30
for name, price, f in [("Subtotal", "37.59", mono), ("Tax", "3.01", mono), ("TOTAL", "40.60", monob)]:
    d.text((50, y), name, font=f, fill=(30, 30, 34))
    w = d.textlength(price, font=f)
    d.text((W - 50 - w, y), price, font=f, fill=(30, 30, 34))
    y += 56
y += 40
center("Card  **** 0000", mono, y); y += 50
center("07/11/2026  10:42", mono, y); y += 90
center("Thank you!", monob, y)

# The table: a warm wood-ish surface with grain and noise.
TW, TH = 1600, 2133
rng = np.random.default_rng(3)
yy, xx = np.mgrid[0:TH, 0:TW]
blot = Image.fromarray((rng.random((TH // 40, TW // 40)) * 255).astype("uint8")).resize((TW, TH), Image.BICUBIC)
blot = np.asarray(blot.filter(ImageFilter.GaussianBlur(30))).astype(float) / 255.0
base = np.stack([78 + 14 * blot, 84 + 14 * blot, 90 + 14 * blot], -1)
base += rng.normal(0, 6, base.shape)
table = Image.fromarray(np.clip(base, 0, 255).astype("uint8")).filter(ImageFilter.GaussianBlur(0.8))

# Place the receipt with a slight perspective (the corners Scan should find).
quad = [(420, 300), (1190, 360), (1250, 1830), (330, 1790)]  # TL, TR, BR, BL on the table
def coeffs(src, dst):
    m = []
    for (x, y), (u, v) in zip(dst, src):
        m.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); m.append([0, 0, 0, x, y, 1, -v * x, -v * y])
    a = np.array(m, float); b = np.array([p for s in src for p in s], float)
    return np.linalg.solve(a, b).tolist()
src = [(0, 0), (W, 0), (W, H), (0, H)]
c = coeffs(src, quad)
warped = rc.transform((TW, TH), Image.PERSPECTIVE, c, Image.BICUBIC)
mask = Image.new("L", (W, H), 255).transform((TW, TH), Image.PERSPECTIVE, c, Image.BICUBIC)

# Soft drop shadow, then the paper.
shadow = Image.new("L", (TW, TH), 0)
shadow.paste(mask.point(lambda v: int(v * 0.55)), (18, 26))
shadow = shadow.filter(ImageFilter.GaussianBlur(22))
table = Image.composite(Image.new("RGB", (TW, TH), (40, 26, 16)), table, shadow)
table.paste(warped, (0, 0), mask)

# Uneven light: a phone's shadow across one corner, brighter at the top right.
light = np.ones((TH, TW), float)
light *= 0.82 + 0.25 * (xx / TW) * (1 - yy / TH) + 0.08
light -= 0.22 * np.exp(-(((xx - 300) / 520.0) ** 2 + ((yy - 1900) / 480.0) ** 2))
arr = np.asarray(table).astype(float) * light[..., None]
arr += rng.normal(0, 2.5, arr.shape)
Image.fromarray(np.clip(arr, 0, 255).astype("uint8")).save(out, quality=90)
print("saved", out)
