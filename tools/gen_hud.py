# HD gauge faces for the vehicle HUD (256x256, drawn 4x supersampled). Needles are drawn live in VehicleHudOverlay.
import math
from PIL import Image, ImageDraw, ImageFont
OUT = 'src/main/resources/assets/dranant/textures/gui'
FB = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
K = 4
def dial(path, maxv, major, minor, red_from, label, unit, accent):
    S = 256 * K
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    c = S / 2
    # bezel: chrome ring + carbon face
    for i in range(24):
        t = i / 23
        col = tuple(int(70 + 170 * (1 - abs(t - 0.35) * 1.6)) for _ in range(3)) + (255,)
        r = c - i * K * 0.35
        d.ellipse([c - r, c - r, c + r, c + r], fill=col)
    r_face = c - 9 * K
    d.ellipse([c - r_face, c - r_face, c + r_face, c + r_face], fill=(12, 13, 16, 245))
    for yy in range(0, S, 6 * K):     # carbon weave
        for xx in range(0, S, 6 * K):
            if ((xx // (6 * K)) + (yy // (6 * K))) % 2 == 0 and (xx + 3 * K - c) ** 2 + (yy + 3 * K - c) ** 2 < (r_face - 4 * K) ** 2:
                d.rectangle([xx, yy, xx + 6 * K - 1, yy + 6 * K - 1], fill=(20, 21, 25, 255))
    def ang(v): return math.radians(-135 + 270 * v / maxv)
    def pt(a, r): return (c + math.sin(a) * r, c - math.cos(a) * r)
    # glowing arc + red zone
    R = r_face - 10 * K
    box = [c - R, c - R, c + R, c + R]
    d.arc(box, -225, 45, fill=accent + (255,), width=3 * K)
    a0 = -225 + 270 * red_from / maxv
    d.arc(box, a0, 45, fill=(255, 40, 40, 255), width=7 * K)
    fsz = ImageFont.truetype(FB, 17 * K)
    v = 0
    while v <= maxv + 1e-6:
        a = ang(v)
        is_major = abs((v / major) - round(v / major)) < 1e-6
        r1 = R - (16 if is_major else 8) * K
        d.line([pt(a, R - 2 * K), pt(a, r1)], fill=(255, 70, 70, 255) if v >= red_from else (240, 240, 245, 255), width=(4 if is_major else 2) * K)
        if is_major:
            txt = str(int(v)) if maxv > 20 else str(int(v))
            d.text(pt(a, R - 34 * K), txt, font=fsz, fill=(255, 90, 90, 255) if v >= red_from else (235, 238, 245, 255), anchor='mm')
        v += minor
    f2 = ImageFont.truetype(FB, 13 * K)
    d.text((c, c + 52 * K), unit, font=f2, fill=accent + (255,), anchor='mm')
    d.text((c, c - 46 * K), label, font=ImageFont.truetype(FB, 10 * K), fill=(150, 155, 165, 255), anchor='mm')
    # hub
    d.ellipse([c - 14 * K, c - 14 * K, c + 14 * K, c + 14 * K], fill=(40, 42, 48, 255), outline=(180, 185, 195, 255), width=2 * K)
    # glass highlight
    hl = Image.new('RGBA', (S, S), (0, 0, 0, 0)); hd = ImageDraw.Draw(hl)
    hd.pieslice([c - r_face, c - r_face, c + r_face, c + r_face], 200, 320, fill=(255, 255, 255, 22))
    im = Image.alpha_composite(im, hl)
    im.resize((256, 256), Image.LANCZOS).save(f'{OUT}/{path}')
dial('speedo_320.png', 320, 40, 10, 280, 'ANANT MOTORS', 'km/h', (60, 200, 255))
dial('speedo_200.png', 200, 20, 5, 170, 'ANANT MOTO', 'km/h', (255, 170, 40))
dial('tach.png', 9, 1, 0.25, 7.5, 'x1000', 'rpm', (255, 120, 40))
print('hud dials done')
