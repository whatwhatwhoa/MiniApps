"""Cut the handwriting font for the app: Latin only (and one weight, if it is a variable font).

The app needs Latin letters and punctuation in one regular weight. This keeps the characters
below, pins a variable font's weight at 400, and keeps every name record, so the copyright and
the OFL notice stay inside the file.

    pip install --target <dir> fonttools
    PYTHONPATH=<dir> python tools/cut_font.py PatrickHand-Regular.ttf app/src/main/res/font/patrick_hand.ttf

PatrickHand-Regular.ttf comes from https://github.com/google/fonts/tree/main/ofl/patrickhand
(SIL OFL 1.1, no reserved font name, so the cut keeps its name).
"""
import sys

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

KEEP = (
    list(range(0x20, 0x7F))  # ASCII
    + list(range(0xA0, 0x180))  # Latin-1 and Latin Extended-A
    + list(range(0x2010, 0x2060))  # dashes, quotes, bullets, ellipsis
    + [0x20AC, 0x2122, 0x2190, 0x2191, 0x2192, 0x2193, 0x2713, 0x2714, 0x2022, 0x00B7]
)


def main(src: str, dst: str) -> None:
    font = TTFont(src)
    if "fvar" in font:
        font = instancer.instantiateVariableFont(font, {"wght": 400})
    opts = subset.Options()
    opts.layout_features = ["*"]
    opts.name_IDs = ["*"]
    opts.name_languages = ["*"]
    opts.notdef_outline = True
    opts.hinting = False
    s = subset.Subsetter(opts)
    s.populate(unicodes=KEEP)
    s.subset(font)
    font.save(dst)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
