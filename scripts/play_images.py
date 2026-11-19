"""
Renders the Play Store graphics into fastlane/metadata/android/en-US/images: the icon, the feature graphic, and
captioned phone and tablet screenshots from raw captures (scripts/screenshots.sh, demo mode).

    python3 scripts/play_images.py <dir with raw_*.png>

Raw files: raw_trackpad, raw_keyboard, raw_remote, raw_connect, raw_aod, raw_settings, raw_gamepad (phone) and
raw_tab_deck, raw_tab_connect, raw_tab_remote (tablet, landscape). Needs Pillow and the Ubuntu font family.
"""
import os, sys, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "fastlane/metadata/android/en-US/images")
os.chdir(sys.argv[1] if len(sys.argv) > 1 else ".")
FB = "/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf"
FR = "/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf" if os.path.exists("/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf") else "/usr/share/fonts/truetype/ubuntu/Ubuntu-L.ttf"
C_IN, C_OUT = (0x36, 0x67, 0xC2), (0x10, 0x23, 0x49)
LIGHT = (0xDC, 0xE6, 0xFF)

def radial(w, h, cx, cy, r):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = min(1.0, math.hypot(x - cx, y - cy) / r)
            px[x, y] = tuple(int(C_IN[i] + (C_OUT[i] - C_IN[i]) * t) for i in range(3))
    return img

def radial_fast(w, h, cx, cy, r):
    s = 8
    small = radial(max(1, w // s), max(1, h // s), cx / s, cy / s, r / s)
    return small.resize((w, h), Image.BICUBIC)

def glyph(size, color=LIGHT):
    """The icon foreground (108-unit viewport) drawn at `size` px: pad with split buttons, two touch points, arc."""
    k = size / 108.0
    S = 4  # supersample
    im = Image.new("L", (size * S, size * S), 0)
    d = ImageDraw.Draw(im)
    f = lambda v: v * k * S
    d.rounded_rectangle([f(30), f(34), f(78), f(74)], radius=f(9), fill=255)
    d.rectangle([f(30), f(62.6), f(78), f(64.4)], fill=0)
    d.rectangle([f(53.1), f(64.4), f(54.9), f(74)], fill=0)
    for cx, cy in [(47, 49), (60, 46)]:
        d.ellipse([f(cx - 4.5), f(cy - 4.5), f(cx + 4.5), f(cy + 4.5)], fill=0)
    # wireless arc: quadratic M44,28.5 Q54,22.5 64,28.5, stroke 2.6
    pts = []
    for i in range(41):
        t = i / 40
        x = (1 - t) ** 2 * 44 + 2 * (1 - t) * t * 54 + t * t * 64
        y = (1 - t) ** 2 * 28.5 + 2 * (1 - t) * t * 22.5 + t * t * 28.5
        pts.append((f(x), f(y)))
    d.line(pts, fill=255, width=int(f(2.6)), joint="curve")
    for p in (pts[0], pts[-1]):
        r = f(1.3); d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=255)
    mask = im.resize((size, size), Image.LANCZOS)
    layer = Image.new("RGBA", (size, size), color + (0,))
    layer.putalpha(mask)
    return layer

# ---- 512 x 512 icon (Play applies its own rounded mask; full-bleed square)
icon = radial_fast(512, 512, 256, 208, 360).convert("RGBA")
icon.alpha_composite(glyph(512))
icon.convert("RGB").save(f"{OUT}/icon.png")

# ---- 1024 x 500 feature graphic
fg = radial_fast(1024, 500, 300, 230, 820).convert("RGBA")
g = glyph(360)
fg.alpha_composite(g, (30, 70))
d = ImageDraw.Draw(fg)
d.text((400, 150), "PadBridge", font=ImageFont.truetype(FB, 92), fill=(255, 255, 255))
d.text((404, 268), "Your phone as a Bluetooth trackpad,", font=ImageFont.truetype(FR, 32), fill=LIGHT)
d.text((404, 310), "keyboard, TV remote and gamepad", font=ImageFont.truetype(FR, 32), fill=LIGHT)
fg.convert("RGB").save(f"{OUT}/featureGraphic.png")

# ---- Framed screenshots, exactly 2:1 (Play's max aspect ratio)
def rounded(img, r):
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius=r, fill=255)
    out = img.convert("RGBA"); out.putalpha(m); return out

def frame(src, caption, sub, out, landscape=False):
    shot = Image.open(src).convert("RGB")
    W, H = (2160, 1080) if landscape else (1080, 2160)
    canvas = radial_fast(W, H, W * 0.3, H * 0.18, max(W, H) * 1.1).convert("RGBA")
    d = ImageDraw.Draw(canvas)
    if landscape:
        d.text((90, 70), caption, font=ImageFont.truetype(FB, 64), fill=(255, 255, 255))
        d.text((92, 150), sub, font=ImageFont.truetype(FR, 36), fill=LIGHT)
        th = H - 240 - 50; tw = int(th * shot.width / shot.height)
        x, y = (W - tw) // 2, 240
    else:
        d.text((70, 90), caption, font=ImageFont.truetype(FB, 64), fill=(255, 255, 255))
        d.text((72, 172), sub, font=ImageFont.truetype(FR, 36), fill=LIGHT)
        # Fit the whole screenshot below the caption.
        th = H - 280 - 60; tw = int(th * shot.width / shot.height)
        x, y = (W - tw) // 2, 280
    s = rounded(shot.resize((tw, th), Image.LANCZOS), 44)
    shadow = Image.new("RGBA", (tw + 80, th + 80), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([40, 40, tw + 40, th + 40], radius=48, fill=(0, 0, 0, 120))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(24)), (x - 40, y - 20))
    canvas.alpha_composite(s, (x, y))
    canvas.convert("RGB").save(out)

shots = [
    ("raw_trackpad.png", "A real multi-touch trackpad", "Scroll, pinch, 3- and 4-finger gestures"),
    ("raw_keyboard.png", "Type with any keyboard", "Your phone's, or a full one in 7 layouts"),
    ("raw_remote.png", "TV remote clickpad", "Android TV, Apple TV, Mac, PC and slides"),
    ("raw_connect.png", "Nothing to install", "Pair once over Bluetooth, then tap Connect"),
    ("raw_aod.png", "Dims itself when idle", "Outlined on black, minimum brightness, still works"),
    ("raw_settings.png", "Make it yours", "Remap gestures per Windows, macOS, Linux"),
]
dst = f"{OUT}/phoneScreenshots"
for f in os.listdir(dst): os.remove(os.path.join(dst, f))
for i, (f, c, sub) in enumerate(shots, 1):
    frame(f, c, sub, f"{dst}/{i}.png")
frame("raw_gamepad.png", "Gamepad presets", "Racing, first person, third person, retro", f"{dst}/7.png", landscape=True)

# ---- Tablet: 16:9 canvas, caption on top, the 16:10 screenshot below.
def frame_tablet(src, caption, sub, out):
    shot = Image.open(src).convert("RGB")
    W, H = 2560, 1440
    canvas = radial_fast(W, H, W * 0.3, H * 0.18, max(W, H) * 1.1).convert("RGBA")
    d = ImageDraw.Draw(canvas)
    d.text((110, 70), caption, font=ImageFont.truetype(FB, 72), fill=(255, 255, 255))
    d.text((112, 162), sub, font=ImageFont.truetype(FR, 40), fill=LIGHT)
    th = H - 250 - 50; tw = int(th * shot.width / shot.height)
    x, y = (W - tw) // 2, 250
    s = rounded(shot.resize((tw, th), Image.LANCZOS), 36)
    shadow = Image.new("RGBA", (tw + 80, th + 80), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle([40, 40, tw + 40, th + 40], radius=40, fill=(0, 0, 0, 120))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(24)), (x - 40, y - 20))
    canvas.alpha_composite(s, (x, y))
    canvas.convert("RGB").save(out)

tablet = [
    ("raw_tab_deck.png", "A laptop on your tablet", "Keyboard with a trackpad below, resizable and movable"),
    ("raw_tab_connect.png", "Built for big screens", "Two-pane layouts, navigation rail, S Pen support"),
    ("raw_tab_remote.png", "TV remote clickpad", "Android TV, Apple TV, Mac, PC and slides"),
]
for kind in ("sevenInchScreenshots", "tenInchScreenshots"):
    tdst = f"{OUT}/{kind}"
    os.makedirs(tdst, exist_ok=True)
    for f in os.listdir(tdst): os.remove(os.path.join(tdst, f))
    for i, (f, c, sub) in enumerate(tablet, 1):
        frame_tablet(f, c, sub, f"{tdst}/{i}.png")
print("done")
