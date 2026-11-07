"""Make a new mini app from templates/app: apps/<folder>, building at once, with its notes.

    py -3.13 -I tools/new_app.py <folder> "<Name>"

<folder> is lowercase letters and digits. It is the module (:<folder>), the folder
(apps/<folder>) and the package (io.github.whatwhatwhoa.<folder>), which is the app's identity
on a phone and never changes. <Name> is what the launcher shows. The rest of the recipe (the
release key, the notes, the icon) is docs/new-app.md.
"""
import argparse
import datetime
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE = os.path.join(ROOT, "templates", "app")
TEXT = (".kt", ".kts", ".xml", ".md", ".in", ".gitignore")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder", help="lowercase letters and digits, e.g. displaycompare")
    ap.add_argument("name", help='the app\'s name, e.g. "Display Compare"')
    args = ap.parse_args()

    if not re.fullmatch(r"[a-z][a-z0-9]{1,30}", args.folder):
        sys.exit("folder: lowercase letters and digits, starting with a letter (it becomes the package)")
    if args.folder == "stack":
        sys.exit("folder: 'stack' is the shared module")
    dst = os.path.join(ROOT, "apps", args.folder)
    if os.path.exists(dst):
        sys.exit(f"apps/{args.folder} exists already")

    subs = {
        "__APP__": args.folder,
        "__NAME__": args.name,
        "__ASSET__": re.sub(r"[^A-Za-z0-9]", "", args.name),
        "__DATE__": datetime.date.today().isoformat(),
    }

    def fill(text: str) -> str:
        for k, v in subs.items():
            text = text.replace(k, v)
        return text

    made = []
    for dirpath, _, files in os.walk(TEMPLATE):
        rel = os.path.relpath(dirpath, TEMPLATE)
        out_dir = os.path.normpath(os.path.join(dst, fill(rel)))
        os.makedirs(out_dir, exist_ok=True)
        for f in files:
            src = os.path.join(dirpath, f)
            out = os.path.join(out_dir, fill(f[:-3] if f.endswith(".in") else f))
            if f.endswith(TEXT) or f == ".gitignore":
                with open(src, encoding="utf-8") as fh:
                    text = fill(fh.read())
                with open(out, "w", encoding="utf-8", newline="\n") as fh:
                    fh.write(text)
            else:
                shutil.copyfile(src, out)
            made.append(os.path.relpath(out, ROOT).replace("\\", "/"))

    left = [m for m in made if any(k in open(os.path.join(ROOT, m), encoding="utf-8", errors="ignore").read()
                                   for k in subs)]
    if left:
        sys.exit(f"placeholders left in: {', '.join(left)}")
    print(f"Made apps/{args.folder} ({len(made)} files): module :{args.folder}, "
          f"package io.github.whatwhatwhoa.{args.folder}, name {args.name!r}.")
    print(f"Next: ./gradlew :{args.folder}:assembleDebug, then docs/new-app.md from step 3.")


if __name__ == "__main__":
    main()
