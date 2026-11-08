"""Gallery cards for the README: the app's name, a one-line subtitle, an edited screenshot.

    py -3.13 -P -E tools/gallery_cards.py <raw screenshots dir> docs/gallery [app ...]

The raw shots are full 1080-wide phone screenshots, named as in `cards` below; they stay out of
the repo (the status bar shows the phone's notifications). How they were taken: docs/architecture.md
*Gallery*. Needs Pillow.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

RAW = Path(sys.argv[1])
OUT = Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)
FONTS = Path(__file__).resolve().parent.parent / "stack" / "src" / "main" / "res" / "font"
bold = lambda s: ImageFont.truetype(str(FONTS / "instrument_sans_bold.ttf"), s)
reg = lambda s: ImageFont.truetype(str(FONTS / "instrument_sans_regular.ttf"), s)
mono = lambda s: ImageFont.truetype(str(FONTS / "jetbrains_mono_medium.ttf"), s)

# Stack light tokens (stack/.../Colors.kt)
PANEL, INK, MUTED, LINE = (250, 251, 251), (26, 32, 38), (91, 102, 112), (210, 216, 221)
MAT, HANDLE = (38, 43, 48), (242, 169, 59)

CW, CH, PAD, R = 900, 1400, 48, 40          # card size, side gutter, corner radius
PW = CW - 2 * PAD                            # pane width
PANE_TOP = 268
PH = CH - PANE_TOP - PAD                     # pane height


def rounded_mask(size, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), r, fill=255)
    return m


def wrap(draw, text, font, width):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if draw.textlength(t, font=font) <= width:
            cur = t
        else:
            lines.append(cur); cur = w
    return lines + [cur]


def screen_pane(src, y0):
    """The phone screen from y0 down, scaled to the pane width, cut to the pane height."""
    im = Image.open(RAW / src).convert("RGB")
    h = round(im.width * PH / PW)
    return im.crop((0, y0, im.width, y0 + h)).resize((PW, PH), Image.LANCZOS)


def scan_pane():
    """Before → after: the photo with the edges Scan found, and the finished page."""
    pane = Image.new("RGB", (PW, PH), MAT)
    d = ImageDraw.Draw(pane)
    for x in range(0, PW, 46):
        d.line((x, 0, x, PH), fill=(53, 60, 67))
    for y in range(0, PH, 46):
        d.line((0, y, PW, y), fill=(53, 60, 67))
    before = Image.open(RAW / "scan-crop.png").convert("RGB").crop((330, 1310, 770, 1942))
    after = Image.open(RAW / "scan-filter.png").convert("RGB")
    ImageDraw.Draw(after).rectangle((624, 1970, 716, 1997), fill=after.getpixel((690, 1960)))  # the zoom pill's corner
    after = after.crop((345, 1420, 735, 1999))
    tw = 352
    tiles = [(before, "Your photo"), (after, "The PDF page")]
    x = 38
    ty = 260
    for i, (t, label) in enumerate(tiles):
        th = round(t.height * tw / t.width)
        t = t.resize((tw, th), Image.LANCZOS)
        pane.paste(t, (x, ty), rounded_mask(t.size, 18))
        lf = mono(30)
        lw = d.textlength(label, font=lf)
        d.text((x + (tw - lw) / 2, ty - 62), label, font=lf, fill=(201, 209, 216))
        if i == 0:
            ax, ay = x + tw + 7, ty + th // 2
            d.polygon([(ax, ay - 26), (ax + 36, ay), (ax, ay + 26)], fill=HANDLE)
            x += tw + 52
    return pane


def card(name, subtitle, accent, pane, out):
    c = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    body = Image.new("RGBA", (CW, CH), PANEL + (255,))
    d = ImageDraw.Draw(body)
    d.rectangle((0, 0, CW, 14), fill=accent)
    d.text((PAD, 54), name, font=bold(76), fill=INK)
    sf = reg(40)
    for i, line in enumerate(wrap(d, subtitle, sf, PW)[:2]):
        d.text((PAD, 156 + i * 50), line, font=sf, fill=MUTED)
    # The pane: rounded, with a thin line, fading into the card at the bottom.
    pane = pane.convert("RGBA")
    fade = 170
    alpha = Image.new("L", (1, PH), 255)
    for y in range(PH - fade, PH):
        alpha.putpixel((0, y), round(255 * (PH - y) / fade))
    pane.putalpha(Image.composite(alpha.resize((PW, PH)), Image.new("L", (PW, PH), 0), rounded_mask((PW, PH), 26)))
    border = Image.new("RGBA", (PW, PH), (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle((0, 0, PW - 1, PH - 1), 26, outline=LINE + (255,), width=3)
    border.putalpha(Image.composite(border.getchannel("A"), Image.new("L", (PW, PH), 0), alpha.resize((PW, PH))))
    body.alpha_composite(pane, (PAD, PANE_TOP))
    body.alpha_composite(border, (PAD, PANE_TOP))
    # Card outline and rounded corners.
    ImageDraw.Draw(body).rounded_rectangle((0, 0, CW - 1, CH - 1), R, outline=LINE + (255,), width=3)
    c.paste(body, (0, 0), rounded_mask((CW, CH), R))
    c = c.resize((CW * 2 // 3, CH * 2 // 3), Image.LANCZOS)   # 600 × 933: sharp at 300 px wide
    c.save(OUT / out, optimize=True)
    print(out, c.size, (OUT / out).stat().st_size // 1024, "KB")


S0, S1, S2, S3, S4 = (0, 145, 127), (200, 105, 26), (46, 111, 208), (181, 49, 122), (122, 69, 192)
only = sys.argv[3:] or None
cards = [
    ("scan.png", "Scan", "Photos of receipts and documents into a clean PDF.", S2, lambda: scan_pane()),
    ("calculator.png", "Calculator", "Type whole formulas, keep a history, make your own functions.", S0,
     lambda: screen_pane("calc3.png", 290)),
    ("powermeter.png", "Power Meter", "See the watts going into or out of your battery.", S1,
     lambda: screen_pane("pm1.png", 150)),
    ("displaycompare.png", "Display Compare", "Compare monitors, TVs, laptops and phones to scale.", S4,
     lambda: screen_pane("dc1.png", 150)),
    ("dailynote.png", "Daily Note", "One page of paper to jot the day on, and a widget.", HANDLE,
     lambda: screen_pane("dn-page.png", 108)),
]
for out, name, sub, accent, pane in cards:
    if only and out.split(".")[0] not in only:
        continue
    try:
        card(name, sub, accent, pane(), out)
    except FileNotFoundError as e:
        print("skip", out, e)
