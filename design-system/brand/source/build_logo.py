"""Gera os SVGs do logo Central 156 (conceito 6 · Tipográfico) com o texto convertido em contorno.

Fonte: Sora (SIL Open Font License), variável 400–800. Medidas em px, iguais às do conceito aprovado.
Uso:  pip install fonttools brotli && python3 build_logo.py
Saída: ../logo.svg, ../logo-inverse.svg, ../logo-mark.svg, ../app-icon.svg (sobrescreve)."""
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.transformPen import TransformPen
import sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "Sora-latin-variable.woff2")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(HERE)

PAL = dict(ink="#0E1B3A", muted="#5B6B8C", teal="#14C2B3", sky="#2E8FFF", amber="#FFB020", navy="#0E1B3A")

def font(weight):
    f = TTFont(SRC)
    return instantiateVariableFont(f, {"wght": weight})

FONTS = {600: font(600), 800: font(800)}

def text_path(txt, weight, size, tracking_em, x, baseline):
    """Retorna (d, bounds) de um texto como contorno, com letter-spacing em em (como no CSS)."""
    f = FONTS[weight]
    upm = f["head"].unitsPerEm
    gs = f.getGlyphSet()
    cmap = f.getBestCmap()
    s = size / upm
    pen = SVGPathPen(gs)
    bpen = BoundsPen(gs)
    cx = x
    for ch in txt:
        g = cmap[ord(ch)]
        t = (s, 0, 0, -s, cx, baseline)
        gs[g].draw(TransformPen(pen, t))
        gs[g].draw(TransformPen(bpen, t))
        cx += gs[g].width * s + tracking_em * size
    import re
    d = re.sub(r"-?\d+\.\d+", lambda m: f"{float(m.group()):.2f}".rstrip("0").rstrip("."), pen.getCommands())
    return d, bpen.bounds

def circle(cx, cy, r, fill):
    return f'<circle cx="{cx:.2f}" cy="{cy:.2f}" r="{r:.2f}" fill="{fill}"/>'

def svg(elements, bounds, pad, title, bg=None, w=None, h=None):
    x0, y0, x1, y1 = bounds
    x0 -= pad; y0 -= pad; x1 += pad; y1 += pad
    vw, vh = x1 - x0, y1 - y0
    body = "".join(elements)
    bgr = f'<rect x="{x0:.2f}" y="{y0:.2f}" width="{vw:.2f}" height="{vh:.2f}" fill="{bg}"/>' if bg else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x0:.2f} {y0:.2f} {vw:.2f} {vh:.2f}" '
            f'width="{w or round(vw)}" height="{h or round(vh)}" role="img" aria-label="{title}">'
            f'<title>{title}</title>{bgr}{body}</svg>\n')

def union(*bs):
    return (min(b[0] for b in bs), min(b[1] for b in bs), max(b[2] for b in bs), max(b[3] for b in bs))

def dots(x_start, cy, r, gap):
    els, cx = [], x_start + r
    for c in (PAL["teal"], PAL["sky"], PAL["amber"]):
        els.append(circle(cx, cy, r, c)); cx += 2 * r + gap
    return els, (x_start, cy - r, cx - gap - r, cy + r)

def lockup(num_color, label_color, with_label=True):
    els, bounds = [], []
    base156 = 70
    d156, b156 = text_path("156", 800, 62, -0.03, 0, base156)
    if with_label:
        cap_top_156 = b156[1]
        label_base = cap_top_156 - 13  # linha de base do rótulo 13px acima do topo do "156"
        dl, bl = text_path("CENTRAL", 600, 13, 0.42, 1, label_base)
        els.append(f'<path d="{dl}" fill="{label_color}"/>'); bounds.append(bl)
    els.append(f'<path d="{d156}" fill="{num_color}"/>'); bounds.append(b156)
    de, bd = dots(b156[2] + 12, base156 - 9, 4.5, 6)
    els += de; bounds.append(bd)
    return els, union(*bounds)

os.makedirs(OUT, exist_ok=True)

# Logo completo (fundos claros)
els, b = lockup(PAL["ink"], PAL["muted"])
open(f"{OUT}/logo.svg", "w").write(svg(els, b, 0, "Central 156"))
# Logo completo invertido (fundos escuros)
els, b = lockup("#FFFFFF", "#FFFFFF\" fill-opacity=\"0.6")
open(f"{OUT}/logo-inverse.svg", "w").write(svg(els, b, 0, "Central 156"))
# Marca compacta: 156 + pontos (espaços pequenos)
els, b = lockup(PAL["ink"], PAL["muted"], with_label=False)
open(f"{OUT}/logo-mark.svg", "w").write(svg(els, b, 0, "Central 156"))

# Ícone do app / favicon: squircle navy, 156 branco com os pontos abaixo (Sym156 size 90)
S = 132
size = 90
d, bb = text_path("156", 800, size * 0.62, -0.04, 0, 0)
tw, th = bb[2] - bb[0], bb[3] - bb[1]
r, gap_dots, gap_v = size * 0.05, size * 0.08, size * 0.1
dots_w = 6 * r + 2 * gap_dots
total_h = th + gap_v + 2 * r
oy = (S - total_h) / 2 - bb[1]
ox = (S - tw) / 2 - bb[0]
d, bb = text_path("156", 800, size * 0.62, -0.04, ox, oy)
de, _ = dots((S - dots_w) / 2, bb[3] + gap_v + r, r, gap_dots)
icon = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {S} {S}" width="{S}" height="{S}" role="img" aria-label="Central 156">'
        f'<title>Central 156</title><rect width="{S}" height="{S}" rx="30" fill="{PAL["navy"]}"/>'
        f'<path d="{d}" fill="#FFFFFF"/>' + "".join(de) + "</svg>\n")
open(f"{OUT}/app-icon.svg", "w").write(icon)
print("ok", os.listdir(OUT))
