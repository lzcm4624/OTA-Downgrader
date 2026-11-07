#!/usr/bin/env python3
"""Generate a liquid-glass style launcher icon for 降级助手 (pure Pillow)."""
import math, os
from PIL import Image, ImageDraw, ImageFilter, ImageOps

OUT = "app/src/main/res"


def squircle_mask(size, radius, n=4.6):
    ss = 3
    big = Image.new("L", (size * ss, size * ss), 0)
    d = ImageDraw.Draw(big)
    cx = cy = size * ss / 2
    r = radius * ss
    pts = []
    steps = 480
    for i in range(steps):
        t = 2 * math.pi * i / steps
        c, s = math.cos(t), math.sin(t)
        x = r * math.copysign(abs(c) ** (2.0 / n), c)
        y = r * math.copysign(abs(s) ** (2.0 / n), s)
        pts.append((cx + x, cy + y))
    d.polygon(pts, fill=255)
    return big.resize((size, size), Image.LANCZOS)


def vgrad(size, top, bot, w=None):
    w = w or size
    strip = Image.new("RGB", (1, size))
    px = strip.load()
    for y in range(size):
        t = y / max(1, size - 1)
        px[0, y] = tuple(int(top[i] * (1 - t) + bot[i] * t) for i in range(3))
    return strip.resize((w, size), Image.BILINEAR).convert("RGBA")


def add_blob(base, cx, cy, r, color, alpha):
    d = max(2, int(r * 2))
    g = ImageOps.invert(Image.radial_gradient("L").resize((d, d), Image.BILINEAR))
    g = g.point(lambda v: int((v / 255.0) ** 2.0 * alpha * 255))
    blob = Image.new("RGBA", (d, d), color + (0,))
    blob.putalpha(g)
    x0, y0 = int(cx - r), int(cy - r)
    sx0, sy0 = max(0, -x0), max(0, -y0)
    dx0, dy0 = max(0, x0), max(0, y0)
    w = min(d - sx0, base.width - dx0)
    h = min(d - sy0, base.height - dy0)
    if w <= 0 or h <= 0:
        return base
    part = blob.crop((sx0, sy0, sx0 + w, sy0 + h))
    base.alpha_composite(part, (dx0, dy0))
    return base


def backdrop(size):
    img = vgrad(size, (10, 16, 34), (30, 14, 54))
    add_blob(img, size * 0.16, size * 0.10, size * 0.98, (46, 108, 255), 0.95)
    add_blob(img, size * 0.94, size * 0.30, size * 0.86, (150, 70, 255), 0.80)
    add_blob(img, size * 0.10, size * 0.94, size * 0.82, (0, 176, 214), 0.42)
    add_blob(img, size * 0.90, size * 0.98, size * 0.70, (255, 92, 170), 0.24)
    return img


def glass_pane(size, pane_radius, n=4.4):
    bg = backdrop(size)
    pane = squircle_mask(size, pane_radius, n=n)

    # refracted backdrop: zoomed slightly, heavily blurred, lifted
    zoom = 1.12
    inner = bg.resize((int(size * zoom), int(size * zoom)), Image.LANCZOS)
    off = (inner.size[0] - size) // 2
    refr = inner.crop((off, off, off + size, off + size))
    refr = refr.filter(ImageFilter.GaussianBlur(size * 0.028))
    refr = Image.blend(refr, Image.new("RGBA", (size, size), (255, 255, 255, 255)), 0.22)

    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    layer.paste(refr, (0, 0), pane)

    # vertical satin sheen
    sheen_strip = Image.new("RGBA", (1, size))
    sp = sheen_strip.load()
    for y in range(size):
        t = y / (size - 1)
        a = int(max(0.0, 0.34 * (1 - t) ** 1.4) * 255)
        sp[0, y] = (255, 255, 255, a)
    sheen = sheen_strip.resize((size, size), Image.BILINEAR)
    sheen = Image.composite(sheen, Image.new("RGBA", (size, size), (0, 0, 0, 0)), pane)
    layer.alpha_composite(sheen)

    # specular rim: blurred edge, modulated top -> bottom
    edge_soft = pane.filter(ImageFilter.GaussianBlur(max(1.5, size * 0.005)))
    rim_grad = Image.new("L", (1, size))
    rp = rim_grad.load()
    for y in range(size):
        t = y / (size - 1)
        rp[0, y] = int(max(0.10, 1.0 - t * 0.85) * 255)
    rim_grad = rim_grad.resize((size, size), Image.BILINEAR)
    rim_alpha = Image.composite(edge_soft, Image.new("L", (size, size), 0), rim_grad)
    rim_alpha = rim_alpha.point(lambda v: int(v * 0.78))
    # chromatic tint: cool at the top, warm at the bottom (light dispersion)
    col_strip = Image.new("RGB", (1, size))
    cp = col_strip.load()
    cool, warm = (188, 232, 255), (255, 196, 234)
    for y in range(size):
        t = y / (size - 1)
        cp[0, y] = (int(cool[0] * (1 - t) + warm[0] * t),
                    int(cool[1] * (1 - t) + warm[1] * t),
                    int(cool[2] * (1 - t) + warm[2] * t))
    rim = col_strip.resize((size, size), Image.BILINEAR).convert("RGBA")
    rim.putalpha(rim_alpha)
    layer.alpha_composite(rim)

    # crisp outer hairline
    hair = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    hair.putalpha(pane.point(lambda v: int(v * 0.35)))
    edge = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    edge_d = ImageDraw.Draw(edge)
    layer.alpha_composite(hair.filter(ImageFilter.GaussianBlur(size * 0.0016)))
    return layer


def arrow_layer(size, scale=1.0):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx = size / 2
    cy = size / 2
    span = size * 0.26 * scale
    stroke = max(3, int(size * 0.030 * scale))
    top = cy - span * 1.05
    mid = cy + span * 0.05
    d.line([(cx, top), (cx, mid)], fill=(255, 255, 255, 255), width=stroke)
    chev_y = cy + span * 0.52
    d.line([(cx - span * 0.66, cy - span * 0.12), (cx, chev_y)], fill=(255, 255, 255, 255), width=stroke)
    d.line([(cx, chev_y), (cx + span * 0.66, cy - span * 0.12)], fill=(255, 255, 255, 255), width=stroke)
    base_y = cy + span * 1.16
    d.line([(cx - span * 0.92, base_y), (cx + span * 0.92, base_y)],
           fill=(228, 240, 255, 225), width=stroke)
    # round the joints
    for (x, y) in [(cx, top), (cx, mid), (cx - span * 0.66, cy - span * 0.12),
                   (cx + span * 0.66, cy - span * 0.12), (cx, chev_y),
                   (cx - span * 0.92, base_y), (cx + span * 0.92, base_y)]:
        rr = stroke * 0.62
        d.ellipse((x - rr - 1, y - rr - 1, x + rr + 1, y + rr + 1),
                  fill=(255, 255, 255, 255))
    return layer


def bloom(layer, size, amount=0.55):
    g = layer.filter(ImageFilter.GaussianBlur(size * 0.020))
    g = g.point(lambda v: int(v * amount))
    return g


def build_full(size):
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    body = backdrop(size)
    mask = squircle_mask(size, size / 2, n=4.6)
    canvas.paste(body, (0, 0), mask)

    pane = glass_pane(size, size * 0.286)
    canvas.alpha_composite(pane)

    arr = arrow_layer(size)
    canvas.alpha_composite(bloom(arr, size, 0.5))
    canvas.alpha_composite(arr)
    return canvas


def build_background(size):
    return backdrop(size)


def build_foreground(size):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    inner = int(size * 0.66)
    off = (size - inner) // 2
    pane = glass_pane(inner, inner * 0.30)
    layer.alpha_composite(pane, (off, off))
    arr = arrow_layer(inner)
    layer.alpha_composite(bloom(arr, inner, 0.5), (off, off))
    layer.alpha_composite(arr, (off, off))
    return layer


def save(img, path, px):
    out = img.resize((px, px), Image.LANCZOS)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out.save(path, "PNG", optimize=True)


if __name__ == "__main__":
    W = 2048
    print("building full…")
    master_full = build_full(W)
    print("building background…")
    master_bg = build_background(W)
    print("building foreground…")
    master_fg = build_foreground(W)

    dens = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    fg_dens = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

    for d, px in dens.items():
        save(master_full, f"{OUT}/mipmap-{d}/ic_launcher.png", px)
        save(master_full, f"{OUT}/mipmap-{d}/ic_launcher_round.png", px)
    for d, px in fg_dens.items():
        save(master_bg, f"{OUT}/drawable-{d}/ic_launcher_background.png", px)
        save(master_fg, f"{OUT}/drawable-{d}/ic_launcher_foreground.png", px)

    master_full.resize((512, 512), Image.LANCZOS).save("icon_preview.png")
    # preview how it looks on a home screen
    shot = Image.new("RGB", (720, 480), (14, 16, 24))
    for i, (img, label) in enumerate([(master_full, "full"), (master_fg, "fg")]):
        prev = Image.new("RGBA", (720, 480), (0, 0, 0, 0))
        tile = img.resize((220, 220), Image.LANCZOS)
        prev.alpha_composite(tile, (60 + i * 260, 130))
    print("done")
