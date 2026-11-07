"""Cut the Stack fonts for the :stack module: static weights, Latin only.

The Stack look uses Instrument Sans (UI, 400/500/600/700) and JetBrains Mono (figures and
codes, 400/500). Google Fonts ships both as variable fonts; Android's Compose font loading is
simplest and smallest with one static file per weight. This pins each weight (and Instrument
Sans's width at 100), keeps the characters below, and keeps every name record, so the
copyright and the OFL notice stay inside each file.

    py -3.13 -m pip install --target <libdir> fonttools
    py -3.13 -I tools/cut_fonts.py --lib <libdir> <InstrumentSans[wdth,wght].ttf> <JetBrainsMono[wght].ttf>

Sources: https://github.com/google/fonts/tree/main/ofl/instrumentsans and .../ofl/jetbrainsmono
(SIL OFL 1.1, no reserved font names, so the cuts keep their names). Output goes to
stack/src/main/res/font/.
"""
import argparse
import os
import sys

KEEP = (
    list(range(0x20, 0x7F))  # ASCII
    + list(range(0xA0, 0x180))  # Latin-1 (× ² ° ± µ ·) and Latin Extended-A
    + list(range(0x2000, 0x2060))  # spaces, dashes, quotes, primes (′ ″), bullets, ellipsis, ‹ ›
    + [0x20AC, 0x2122, 0x2190, 0x2191, 0x2192, 0x2193, 0x2212, 0x2248, 0x2260, 0x2264, 0x2265,
       0x2713, 0x2714, 0x25B2, 0x25B6, 0x25BC, 0x25C0]
)

CUTS = [
    # (source index, file name, axes)
    (0, "instrument_sans_regular.ttf", {"wght": 400, "wdth": 100}),
    (0, "instrument_sans_medium.ttf", {"wght": 500, "wdth": 100}),
    (0, "instrument_sans_semibold.ttf", {"wght": 600, "wdth": 100}),
    (0, "instrument_sans_bold.ttf", {"wght": 700, "wdth": 100}),
    (1, "jetbrains_mono_regular.ttf", {"wght": 400}),
    (1, "jetbrains_mono_medium.ttf", {"wght": 500}),
]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--lib", help="folder fontTools was installed into with pip --target")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(__file__), "..", "stack", "src",
                                                  "main", "res", "font"))
    ap.add_argument("sources", nargs=2, help="Instrument Sans and JetBrains Mono variable TTFs")
    args = ap.parse_args()
    if args.lib:
        sys.path.insert(0, args.lib)
    from fontTools import subset
    from fontTools.ttLib import TTFont
    from fontTools.varLib import instancer

    os.makedirs(args.out, exist_ok=True)
    for index, name, axes in CUTS:
        font = TTFont(args.sources[index])
        known = {a.axisTag for a in font["fvar"].axes} if "fvar" in font else set()
        font = instancer.instantiateVariableFont(font, {k: v for k, v in axes.items() if k in known})
        opts = subset.Options()
        opts.layout_features = ["*"]
        opts.name_IDs = ["*"]
        opts.name_languages = ["*"]
        opts.notdef_outline = True
        opts.hinting = False
        s = subset.Subsetter(opts)
        s.populate(unicodes=KEEP)
        s.subset(font)
        dst = os.path.join(args.out, name)
        font.save(dst)
        print(f"{name}: {os.path.getsize(dst) // 1024} KB")


if __name__ == "__main__":
    main()
