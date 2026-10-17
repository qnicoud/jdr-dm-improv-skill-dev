#!/usr/bin/env python3
"""Genere docs/ux/logo/final/*.svg ; texte "DM Gym" vectorise depuis Alegreya (res/font)."""
import sys
from pathlib import Path
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

root = Path(__file__).resolve().parent.parent
out = root / "docs/ux/logo/final"
out.mkdir(parents=True, exist_ok=True)
BRICK, CREAM, GOLD = "#8A2E1C", "#FBF5EA", "#F2C94C"

font = TTFont(root / "app/src/main/res/font/alegreya.ttf")
if "fvar" in font:
    font = instantiateVariableFont(font, {"wght": 800})
gs, cmap, upm = font.getGlyphSet(), font.getBestCmap(), font["head"].unitsPerEm


def text_path(txt, size):
    s, x, d = size / upm, 0, []
    for ch in txt:
        g = cmap[ord(ch)]
        pen = SVGPathPen(gs, ntos=lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
        gs[g].draw(TransformPen(pen, (s, 0, 0, -s, x, 0)))
        d.append(pen.getCommands())
        x += gs[g].width * s
    return " ".join(d), x


FG = f'''<path d="M54 22L81.7 38V70L54 86L26.3 70V38Z" fill="{CREAM}"/>
<path d="M54 22V37M26.3 38L41 37M81.7 38L67 37M26.3 70L41 71M81.7 70L67 71M54 86V71" stroke="{BRICK}" stroke-width="1.6" fill="none"/>
<path d="M41 36H67V39.5H41ZM41 68.5H67V72H41ZM43 39.5H65L54 54L65 68.5H43L54 54Z" fill="{BRICK}"/>
<path d="M54 61L59.5 68.5H48.5ZM53.2 54H54.8V61H53.2Z" fill="{GOLD}"/>'''
MONO = '''<path fill-rule="evenodd" fill="#112430" d="M54 22L81.7 38V70L54 86L26.3 70V38ZM53.2 23H54.8V37H53.2ZM53.2 71H54.8V85H53.2ZM27 37.2L41 36.2V37.8L27 38.8ZM81 37.2L67 36.2V37.8L81 38.8ZM27 69.2L41 70.2V71.8L27 70.8ZM81 69.2L67 70.2V71.8L81 70.8ZM43 39.5H65L54 54L65 68.5H43L54 54ZM54 61L59.5 68.5H48.5Z"/>'''
NS = 'xmlns="http://www.w3.org/2000/svg"'

(out / "icon.svg").write_text(f'''<svg {NS} viewBox="0 0 108 108" width="512" height="512" role="img" aria-label="DM Gym">
<rect width="108" height="108" rx="24" fill="{BRICK}"/>
{FG}
</svg>
''')
# Monochrome : trous transparents, pas de fond
(out / "icon-monochrome.svg").write_text(f'''<svg {NS} viewBox="0 0 108 108" width="512" height="512" role="img" aria-label="DM Gym monochrome">
{MONO}
</svg>
''')
d, w = text_path("DM Gym", 64)
(out / "logo-horizontal.svg").write_text(f'''<svg {NS} viewBox="0 0 {int(w)+150} 108" width="{int(w)+150}" height="108" role="img" aria-label="DM Gym">
<rect width="108" height="108" rx="24" fill="{BRICK}"/>
{FG}
<path transform="translate(120 74)" fill="{BRICK}" d="{d}"/>
</svg>
''')
print("ok", file=sys.stderr)
