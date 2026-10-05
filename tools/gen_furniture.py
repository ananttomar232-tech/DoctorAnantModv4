# Modern HD furniture: generates block models (JSON), 64x64 HD material textures, picture textures for screens/fronts,
# item models, blockstates, and the Java FurnitureType enum (collision shapes + seat heights + light) from ONE description.
# Authored facing NORTH (the user of the furniture looks toward -Z / the front of the piece is at z = 0).
import json, math, os, random
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

A = 'src/main/resources/assets/dranant'
T = f'{A}/textures/block/furniture'
JAVA = 'src/main/java/com/dranant/block/FurnitureType.java'
FB = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
N = 64
rng = np.random.default_rng(42)
os.makedirs(T, exist_ok=True)

# ------------------------------------------------------------------ HD materials (tileable 64x64)
def save(img, name):
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA' if img.shape[2] == 4 else 'RGB').save(f'{T}/{name}.png')

def base(col):
    a = np.zeros((N, N, 4), np.float32); a[..., :3] = col; a[..., 3] = 255; return a

def noise(a, amt):
    a[..., :3] += rng.normal(0, amt, (N, N, 1)); return a

def fabric(col, name):
    a = base(col)
    yy, xx = np.mgrid[0:N, 0:N]
    weave = ((xx % 2) ^ (yy % 2)) * 6 - 3
    a[..., :3] += weave[..., None]
    a[..., :3] += (np.sin(xx * 0.9) * np.sin(yy * 0.7))[..., None] * 3
    noise(a, 4); save(a, name)

def wood(col, name, dark=0.75):
    a = base(col)
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    grain = np.sin(xx * 0.35 + np.sin(yy * 0.08) * 4 + np.sin(yy * 0.021 + xx * 0.05) * 2)
    a[..., :3] *= (0.88 + 0.12 * grain)[..., None]
    for _ in range(3):
        x0 = rng.integers(0, N)
        a[:, max(0, x0 - 1):x0 + 1, :3] *= dark
    noise(a, 3); save(a, name)

def lacquer(col, name):
    a = base(col)
    t = np.linspace(1.04, 0.94, N)
    a[..., :3] *= t[:, None, None]
    noise(a, 1.2); save(a, name)

def marble(name, col=(238, 238, 240), vein=(150, 150, 160)):
    a = base(col)
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    for k in range(4):
        ph = rng.uniform(0, 6.28); fr = rng.uniform(0.04, 0.09)
        v = np.abs(np.sin(xx * fr + yy * fr * 0.7 + np.sin(yy * 0.11 + ph) * 2.2 + ph))
        m = np.exp(-(v / 0.04) ** 2)
        a[..., :3] = a[..., :3] * (1 - m[..., None] * 0.55) + np.array(vein) * m[..., None] * 0.55
    noise(a, 1.5); save(a, name)

def metal(col, name, brushed=True):
    a = base(col)
    if brushed:
        a[..., :3] += rng.normal(0, 7, (N, 1, 1))
    t = np.linspace(1.08, 0.9, N)
    a[..., :3] *= t[:, None, None]
    noise(a, 2); save(a, name)

def glassy(col, name, alpha=255):
    a = base(col); a[..., 3] = alpha
    yy, xx = np.mgrid[0:N, 0:N]
    s = np.exp(-(((xx + yy) - 40) / 6.0) ** 2) * 60 + np.exp(-(((xx + yy) - 70) / 3.0) ** 2) * 40
    a[..., :3] += s[..., None]
    save(a, name)

fabric((128, 132, 140), 'fabric_gray')
fabric((70, 74, 82), 'fabric_charcoal')
fabric((196, 92, 52), 'fabric_accent')
fabric((232, 226, 210), 'linen')
wood((112, 72, 44), 'walnut')
wood((214, 184, 140), 'oak_light', 0.85)
lacquer((244, 244, 246), 'white_lacquer')
lacquer((38, 40, 46), 'graphite_lacquer')
marble('marble')
marble('marble_black', (36, 36, 40), (150, 140, 120))
metal((188, 192, 200), 'steel')
metal((228, 232, 238), 'chrome', brushed=False)
metal((26, 26, 30), 'black_steel')
glassy((18, 20, 24), 'black_glass')
lacquer((250, 250, 252), 'ceramic')
glassy((170, 195, 215), 'mirror')
a = base((24, 24, 28)); noise(a, 2)
for i in range(4):  # leather seat
    a[:, i * 16:i * 16 + 1, :3] *= 0.7
save(a, 'leather_black')

# ------------------------------------------------------------------ picture faces (drawn at the face's real aspect, then fitted to 64x64)
def picture(name, w, h, fn, scale=40):
    W, H = int(w * scale), int(h * scale)
    im = Image.new('RGBA', (W, H), (0, 0, 0, 255)); d = ImageDraw.Draw(im)
    fn(d, W, H)
    im.resize((N, N), Image.LANCZOS).save(f'{T}/{name}.png')

def tv(d, W, H):
    d.rectangle([0, 0, W, H], fill=(14, 14, 16))
    m = int(H * 0.05)
    # screen: sunset city skyline + channel bug
    for y in range(m, H - m):
        t = (y - m) / (H - 2 * m)
        c = (int(255 * (1 - t) + 40 * t), int(120 * (1 - t) + 30 * t), int(80 * (1 - t) + 110 * t))
        d.line([m, y, W - m, y], fill=c)
    random.seed(3)
    x = m
    while x < W - m:
        bw = random.randint(W // 22, W // 10); bh = random.randint(H // 6, H // 2)
        d.rectangle([x, H - m - bh, min(W - m, x + bw), H - m], fill=(20, 18, 40))
        for wy in range(H - m - bh + 6, H - m - 4, 10):
            for wx in range(x + 4, min(W - m, x + bw) - 4, 9):
                if random.random() < 0.5: d.rectangle([wx, wy, wx + 3, wy + 4], fill=(255, 210, 120))
        x += bw + 2
    d.ellipse([W * 0.62, H * 0.15, W * 0.74, H * 0.35], fill=(255, 230, 160))
    f = ImageFont.truetype(FB, int(H * 0.09))
    d.text((W - m - 8, m + 6), 'DR. ANANT TV', font=f, fill=(255, 255, 255), anchor='ra')
    d.rectangle([m, m, W - m, H - m], outline=(40, 40, 46), width=3)
picture('tv_screen', 15, 9, tv)

def cabinet(d, W, H, oven=False, drawers=2):
    d.rectangle([0, 0, W, H], fill=(244, 244, 246))
    g = 4
    if oven:
        d.rectangle([g, g, W - g, H * 0.18], fill=(40, 40, 44))
        for i in range(4): d.ellipse([W * (0.15 + i * 0.2) - 10, H * 0.09 - 10, W * (0.15 + i * 0.2) + 10, H * 0.09 + 10], fill=(200, 200, 205))
        d.rounded_rectangle([g * 2, H * 0.22, W - g * 2, H * 0.95], radius=10, fill=(22, 22, 26))
        d.rounded_rectangle([W * 0.12, H * 0.34, W * 0.88, H * 0.85], radius=8, fill=(60, 40, 30))
        d.rectangle([W * 0.15, H * 0.26, W * 0.85, H * 0.29], fill=(210, 212, 220))
        return
    hh = H / drawers
    for i in range(drawers):
        d.rectangle([g, i * hh + g, W - g, (i + 1) * hh - g], fill=(250, 250, 252), outline=(205, 205, 210), width=2)
        d.rounded_rectangle([W * 0.3, i * hh + hh * 0.18, W * 0.7, i * hh + hh * 0.18 + 6], radius=3, fill=(160, 165, 175))
picture('cabinet_front', 16, 14, lambda d, W, H: cabinet(d, W, H, drawers=3))
picture('oven_front', 16, 14, lambda d, W, H: cabinet(d, W, H, oven=True))
picture('tv_stand_front', 16, 5, lambda d, W, H: cabinet(d, W, H, drawers=1))

def cooktop(d, W, H):
    d.rectangle([0, 0, W, H], fill=(14, 14, 16))
    for cx, cy, r in ((0.3, 0.3, 0.17), (0.7, 0.3, 0.13), (0.3, 0.7, 0.13), (0.7, 0.7, 0.17)):
        for k in range(3):
            rr = r * (1 - k * 0.3)
            d.ellipse([W * (cx - rr), H * (cy - rr), W * (cx + rr), H * (cy + rr)], outline=(110, 110, 120), width=3)
    d.rectangle([W * 0.4, H * 0.9, W * 0.6, H * 0.96], fill=(255, 80, 40))
picture('cooktop', 16, 16, cooktop)

def fridge(d, W, H):
    d.rectangle([0, 0, W, H], fill=(200, 204, 212))
    for x in range(0, W, 3): d.line([x, 0, x, H], fill=(190 + (x * 7) % 18, 194 + (x * 7) % 18, 202 + (x * 7) % 18))
    d.line([W * 0.5, 0, W * 0.5, H * 0.62], fill=(90, 92, 100), width=4)
    d.line([0, H * 0.62, W, H * 0.62], fill=(90, 92, 100), width=4)
    d.rounded_rectangle([W * 0.43, H * 0.2, W * 0.46, H * 0.52], radius=4, fill=(60, 62, 70))
    d.rounded_rectangle([W * 0.54, H * 0.2, W * 0.57, H * 0.52], radius=4, fill=(60, 62, 70))
    d.rounded_rectangle([W * 0.3, H * 0.66, W * 0.7, H * 0.69], radius=4, fill=(60, 62, 70))
    d.rounded_rectangle([W * 0.1, H * 0.2, W * 0.36, H * 0.42], radius=8, fill=(30, 32, 38))      # ice/water + display
    d.text((W * 0.23, H * 0.25), '-18C', font=ImageFont.truetype(FB, int(W * 0.05)), fill=(80, 200, 255), anchor='mm')
picture('fridge_front', 16, 30, fridge, scale=24)

def monitor(d, W, H):
    d.rectangle([0, 0, W, H], fill=(10, 10, 12))
    m = int(H * 0.06)
    d.rectangle([m, m, W - m, H - m], fill=(18, 26, 40))
    d.rectangle([m, m, W - m, m + H * 0.08], fill=(40, 50, 70))
    random.seed(7)
    for i in range(10):
        y = m + H * 0.14 + i * H * 0.075
        x = m + 10 + random.randint(0, 3) * 18
        cols = [(120, 200, 255), (255, 170, 80), (160, 255, 160), (230, 230, 230)]
        d.rectangle([x, y, x + random.randint(W // 8, W // 2), y + H * 0.03], fill=cols[i % 4])
    d.rectangle([W * 0.62, H * 0.2, W - m - 10, H * 0.7], fill=(30, 40, 60), outline=(70, 90, 120), width=2)
    for i in range(6):
        d.rectangle([W * 0.65 + i * 18, H * 0.65 - (i * 37 % 60) - 20, W * 0.65 + i * 18 + 10, H * 0.65], fill=(80, 220, 160))
picture('monitor_screen', 12, 7, monitor)

def keyboard(d, W, H):
    d.rectangle([0, 0, W, H], fill=(30, 30, 34))
    for r in range(4):
        for c in range(14):
            d.rectangle([8 + c * (W - 16) / 14, 8 + r * (H - 16) / 4, 8 + (c + 1) * (W - 16) / 14 - 3, 8 + (r + 1) * (H - 16) / 4 - 3], fill=(70, 70, 78))
picture('keyboard', 10, 4, keyboard)

def shade(d, W, H):
    d.rectangle([0, 0, W, H], fill=(246, 236, 210))
    for x in range(0, W, 6): d.line([x, 0, x, H], fill=(236, 224, 196))
    d.rectangle([0, H * 0.9, W, H], fill=(210, 190, 150))
picture('lamp_shade', 8, 5, shade)

# ------------------------------------------------------------------ furniture geometry
def auto_uv(d, fr, to):
    """Vanilla's position-based UV, kept inside the 0..16 sprite (shifted, or squeezed when larger than 16)."""
    x0, y0, z0 = fr; x1, y1, z1 = to
    uv = {'down': [x0, 16 - z1, x1, 16 - z0], 'up': [x0, z0, x1, z1], 'north': [16 - x1, 16 - y1, 16 - x0, 16 - y0],
          'south': [x0, 16 - y1, x1, 16 - y0], 'west': [z0, 16 - y1, z1, 16 - y0], 'east': [16 - z1, 16 - y1, 16 - z0, 16 - y0]}[d]
    for a, b in ((0, 2), (1, 3)):
        lo, hi = uv[a], uv[b]
        if hi - lo > 16:
            uv[a], uv[b] = 0, 16
        elif lo < 0 or hi > 16:
            shift = -lo if lo < 0 else 16 - hi
            uv[a], uv[b] = lo + shift, hi + shift
    return [round(v, 3) for v in uv]

def E(fr, to, tex, faces=None, uvfull=()):
    """Element; `faces` overrides texture per face, `uvfull` faces get the full picture mapped."""
    f = {}
    for d in ('north', 'south', 'east', 'west', 'up', 'down'):
        t = (faces or {}).get(d, tex)
        if t is None: continue
        face = {"texture": '#' + t}
        face["uv"] = [0, 0, 16, 16] if d in uvfull else auto_uv(d, fr, to)
        f[d] = face
    return {"from": fr, "to": to, "faces": f}

def legs4(x0, z0, x1, z1, h, tex, s=1.5):
    return [E([x, 0, z], [x + s, h, z + s], tex) for x in (x0, x1 - s) for z in (z0, z1 - s)]

F = {}  # name -> dict(elements, seat, light, tall, name)
F['modern_sofa'] = dict(title='Modern Sofa', seat=9, elements=[
    E([0, 2, 1], [16, 6, 16], 'fabric_charcoal'),
    E([2, 6, 1.5], [14, 9, 13], 'fabric_gray'),
    E([2, 6, 12], [14, 15, 16], 'fabric_gray'),
    E([0, 6, 2], [2, 11, 16], 'fabric_charcoal'), E([14, 6, 2], [16, 11, 16], 'fabric_charcoal'),
    E([10.5, 9, 9.5], [13.5, 13, 11.5], 'fabric_accent'),
    *legs4(1, 2, 15, 15, 2, 'chrome', 1)])
F['coffee_table'] = dict(title='Marble Coffee Table', elements=[
    E([0, 7, 0], [16, 8.5, 16], 'marble'),
    E([2, 2, 2], [14, 3, 14], 'walnut'),
    *legs4(1.5, 1.5, 14.5, 14.5, 7, 'black_steel', 1)])
F['tv_stand'] = dict(title='Smart TV & Media Unit', light=4, elements=[
    E([0, 0, 5], [16, 5, 15], 'white_lacquer', {'north': 'tv_stand_front'}, ('north',)),
    E([0, 5, 5], [16, 5.5, 15], 'walnut'),
    E([7, 5.5, 9], [9, 7, 11], 'steel'),
    E([-4, 7, 9.5], [20, 21, 10.5], 'black_glass', {'north': 'tv_screen'}, ('north',))])
F['dining_table'] = dict(title='Walnut Dining Table', elements=[
    E([0, 14, 0], [16, 16, 16], 'walnut'),
    E([1.5, 0, 1.5], [3, 14, 3], 'black_steel'), E([13, 0, 1.5], [14.5, 14, 3], 'black_steel'),
    E([1.5, 0, 13], [3, 14, 14.5], 'black_steel'), E([13, 0, 13], [14.5, 14, 14.5], 'black_steel'),
    E([3, 12, 2], [13, 13.5, 3], 'black_steel'), E([3, 12, 13], [13, 13.5, 14], 'black_steel')])
F['dining_chair'] = dict(title='Designer Chair', seat=8, elements=[
    E([2.5, 7, 2.5], [13.5, 9, 13.5], 'leather_black'),
    E([2.5, 9, 12], [13.5, 22, 13.5], 'leather_black'),
    *legs4(3, 3, 13, 13, 7, 'oak_light', 1.2)])
F['kitchen_counter'] = dict(title='Kitchen Counter', elements=[
    E([1, 0, 2], [15, 1, 15], 'graphite_lacquer'),
    E([0, 1, 1], [16, 14, 16], 'white_lacquer', {'north': 'cabinet_front'}, ('north',)),
    E([0, 14, 0], [16, 16, 16], 'marble')])
F['kitchen_sink'] = dict(title='Kitchen Sink', elements=[
    E([1, 0, 2], [15, 1, 15], 'graphite_lacquer'),
    E([0, 1, 1], [16, 12, 16], 'white_lacquer', {'north': 'cabinet_front'}, ('north',)),
    E([0, 14, 0], [16, 16, 3], 'marble'), E([0, 14, 13], [16, 16, 16], 'marble'),
    E([0, 14, 3], [3, 16, 13], 'marble'), E([13, 14, 3], [16, 16, 13], 'marble'),
    E([3, 12, 3], [13, 13, 13], 'steel'),
    E([3, 13, 3], [13, 14, 3.5], 'steel'), E([3, 13, 12.5], [13, 14, 13], 'steel'),
    E([7.5, 16, 13.5], [8.5, 22, 14.5], 'chrome'), E([7.5, 21, 9], [8.5, 22, 14.5], 'chrome'),
    E([10, 16, 13.5], [11, 18, 14.5], 'chrome')])
F['kitchen_stove'] = dict(title='Induction Range', elements=[
    E([1, 0, 2], [15, 1, 15], 'graphite_lacquer'),
    E([0, 1, 1], [16, 14, 16], 'black_steel', {'north': 'oven_front'}, ('north',)),
    E([0, 14, 0], [16, 16, 16], 'black_glass', {'up': 'cooktop'}, ('up',))])
F['fridge'] = dict(title='French-Door Fridge', tall=True, elements=[
    E([0, 0, 1], [16, 31, 16], 'steel', {'north': 'fridge_front'}, ('north',)),
    E([0, 31, 1], [16, 32, 16], 'black_steel')])
F['floor_lamp'] = dict(title='Arc Floor Lamp', light=15, elements=[
    E([4.5, 0, 4.5], [11.5, 1, 11.5], 'black_steel'),
    E([7.5, 1, 7.5], [8.5, 11, 8.5], 'chrome'),
    E([4, 11, 4], [12, 16, 12], 'linen', {'north': 'lamp_shade', 'south': 'lamp_shade', 'east': 'lamp_shade', 'west': 'lamp_shade'},
      ('north', 'south', 'east', 'west'))])
F['office_desk'] = dict(title='Workstation Desk', light=3, elements=[
    E([0, 12, 0], [16, 13, 16], 'oak_light'),
    E([0.5, 0, 1], [1.5, 12, 15], 'black_steel'), E([14.5, 0, 1], [15.5, 12, 15], 'black_steel'),
    E([1.5, 6, 14], [14.5, 12, 15], 'black_steel'),
    E([7, 13, 11], [9, 15, 13], 'black_steel'),
    E([2, 15, 11.5], [14, 22, 12.5], 'black_steel', {'north': 'monitor_screen'}, ('north',)),
    E([3, 13, 3], [13, 13.5, 7], 'black_steel', {'up': 'keyboard'}, ('up',)),
    E([13.5, 13, 4], [15, 13.6, 6.5], 'black_steel')])
F['modern_toilet'] = dict(title='Wall-Hung Toilet', seat=7, elements=[
    E([4, 3, 4], [12, 7, 15], 'ceramic'),
    E([3.5, 7, 3.5], [12.5, 7.6, 14], 'ceramic'),
    E([3, 3, 14], [13, 15, 16], 'ceramic'),
    E([7, 13, 15.5], [9, 14, 16.5], 'chrome')])
F['bathroom_vanity'] = dict(title='Bathroom Vanity & Mirror', light=6, elements=[
    E([0, 3, 4], [16, 11, 16], 'walnut', {'north': 'cabinet_front'}, ('north',)),
    E([0, 11, 3], [16, 12, 16], 'marble_black'),
    E([3, 12, 5], [13, 14.5, 13], 'ceramic'),
    E([7.5, 12, 13.5], [8.5, 17, 14.5], 'chrome'), E([7.5, 16, 11], [8.5, 17, 14.5], 'chrome'),
    E([1, 18, 15], [15, 30, 16], 'black_steel', {'north': 'mirror'}, ('north',)),
    E([1, 30, 14], [15, 30.6, 16], 'chrome')])

TEXSET = sorted({f['texture'][1:] for e in [x for v in F.values() for x in v['elements']] for f in e['faces'].values()})

for name, d in F.items():
    tex = {t: f'dranant:block/furniture/{t}' for t in sorted({f['texture'][1:] for e in d['elements'] for f in e['faces'].values()})}
    tex['particle'] = tex[d['elements'][0]['faces']['north']['texture'][1:]] if 'north' in d['elements'][0]['faces'] else list(tex.values())[0]
    model = {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": tex, "elements": d['elements'],
             "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.5 if d.get('tall') else 0.625] * 3},
                         "fixed": {"scale": [0.5] * 3}, "ground": {"scale": [0.25] * 3, "translation": [0, 3, 0]},
                         "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375] * 3},
                         "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4] * 3}}}
    os.makedirs(f'{A}/models/block/furniture', exist_ok=True)
    json.dump(model, open(f'{A}/models/block/furniture/{name}.json', 'w'), indent=1)
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    if d.get('tall'):
        bs = {"variants": {}}
        for fdir, r in rot.items():
            bs["variants"][f"facing={fdir},half=lower"] = {"model": f"dranant:block/furniture/{name}", "y": r}
            bs["variants"][f"facing={fdir},half=upper"] = {"model": "dranant:block/furniture/empty"}
    else:
        bs = {"variants": {f"facing={fdir}": {"model": f"dranant:block/furniture/{name}", "y": r} for fdir, r in rot.items()}}
    json.dump(bs, open(f'{A}/blockstates/{name}.json', 'w'), indent=1)
    json.dump({"parent": f"dranant:block/furniture/{name}"}, open(f'{A}/models/item/{name}.json', 'w'), indent=1)
json.dump({"textures": {"particle": "dranant:block/furniture/steel"}, "elements": []}, open(f'{A}/models/block/furniture/empty.json', 'w'))
for name in F:   # loot: drop itself
    p = f'src/main/resources/data/dranant/loot_table/blocks/{name}.json'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    cond = [{"condition": "minecraft:block_state_property", "block": f"dranant:{name}", "properties": {"half": "lower"}}] if F[name].get('tall') else []
    json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"dranant:{name}"}],
                                                     "conditions": [{"condition": "minecraft:survives_explosion"}] + cond}]}, open(p, 'w'), indent=1)

# ------------------------------------------------------------------ Java enum (collision boxes clamped per half)
def boxes(d, half):
    out = []
    for e in d['elements']:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        x0, x1 = max(0, x0), min(16, x1)
        lo, hi = (0, 16) if half == 0 else (16, 32)
        a, b = max(lo, y0), min(hi, y1)
        if b - a < 0.5 or x1 - x0 < 0.5: continue
        out.append((x0, a - lo, z0, x1, b - lo, z1))
    return out
def jb(bx):
    return 'new double[][]{' + ', '.join('{' + ', '.join(('%g' % v) for v in b) + '}' for b in bx) + '}'
lines = []
for name, d in F.items():
    up = jb(boxes(d, 1)) if d.get('tall') else 'new double[0][]'
    lines.append(f'    {name.upper()}("{name}", "{d["title"]}", {d.get("seat", 0)}, {d.get("light", 0)}, {str(bool(d.get("tall"))).lower()},\n            {jb(boxes(d, 0))},\n            {up})')
java = '''package com.dranant.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Modern HD furniture pieces - GENERATED by tools/gen_furniture.py together with the block models and 64x64 HD textures.
 * Boxes are authored facing NORTH in pixels; shapes are rotated for every facing.
 */
public enum FurnitureType {
''' + ',\n'.join(lines) + ''';

    private final String id;
    private final String title;
    private final int seatHeight;
    private final int light;
    private final boolean tall;
    private final Map<Direction, VoxelShape> lower = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> upper = new EnumMap<>(Direction.class);

    FurnitureType(String id, String title, int seatHeight, int light, boolean tall, double[][] lowerBoxes, double[][] upperBoxes) {
        this.id = id;
        this.title = title;
        this.seatHeight = seatHeight;
        this.light = light;
        this.tall = tall;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            lower.put(d, shape(lowerBoxes, d));
            upper.put(d, shape(upperBoxes, d));
        }
    }

    private static VoxelShape shape(double[][] boxes, Direction facing) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
            double rx0, rz0, rx1, rz1;
            switch (facing) {
                case EAST -> { rx0 = 16 - z1; rz0 = x0; rx1 = 16 - z0; rz1 = x1; }
                case SOUTH -> { rx0 = 16 - x1; rz0 = 16 - z1; rx1 = 16 - x0; rz1 = 16 - z0; }
                case WEST -> { rx0 = z0; rz0 = 16 - x1; rx1 = z1; rz1 = 16 - x0; }
                default -> { rx0 = x0; rz0 = z0; rx1 = x1; rz1 = z1; }
            }
            s = Shapes.or(s, Block.box(rx0, b[1], rz0, rx1, b[4], rz1));
        }
        return s.isEmpty() ? Block.box(0, 0, 0, 16, 2, 16) : s.optimize();
    }

    public String id() { return id; }
    public String title() { return title; }
    /** Seat height in pixels (0 = not a seat). */
    public int seatHeight() { return seatHeight; }
    public int light() { return light; }
    public boolean tall() { return tall; }

    public VoxelShape shape(Direction facing, boolean upperHalf) {
        return (upperHalf ? upper : lower).get(facing);
    }
}
'''
open(JAVA, 'w').write(java)
json.dump({k: v['title'] for k, v in F.items()}, open('/tmp/claude-0/-home-claude/02e74c8f-3fc4-517a-b61b-3de95b0c3bb1/scratchpad/furniture_titles.json', 'w'))
print('furniture:', len(F), 'pieces;', len(TEXSET), 'textures')
