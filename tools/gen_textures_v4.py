# v4 HD item textures (128x128): Plasma Rifle, Surgeon's Katana, Defibrillator Hammer. Drawn at 1024 and downsampled.
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
R = 'src/main/resources/assets/dranant/textures/item'
S = 1024
def finish(im, name, glow=None):
    if glow is not None:
        g = glow.filter(ImageFilter.GaussianBlur(18))
        im = Image.alpha_composite(g, im)
    a = np.array(im.resize((128, 128), Image.LANCZOS)).astype(float)
    a[..., 3] = np.where(a[..., 3] > 90, 255, 0)
    Image.fromarray(a.astype(np.uint8)).save(f'{R}/{name}.png')

def rot_poly(pts, ang, cx=S / 2, cy=S / 2):
    c, s = math.cos(ang), math.sin(ang)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]

# ---------------- Plasma Rifle (horizontal, muzzle right, then rotated -30 deg like vanilla tools)
im = Image.new('RGBA', (S, S)); d = ImageDraw.Draw(im)
glow = Image.new('RGBA', (S, S)); gd = ImageDraw.Draw(glow)
A = math.radians(-32)
def P(*pts): return rot_poly(pts, A)
GUN = (52, 58, 70); DARK = (22, 24, 30); EDGE = (10, 10, 14); CY = (60, 230, 255); MAG = (255, 60, 220)
d.polygon(P((120, 470), (330, 440), (360, 520), (300, 640), (170, 650), (140, 560)), fill=(45, 40, 38), outline=EDGE, width=14)     # stock
d.polygon(P((300, 430), (760, 420), (800, 470), (780, 540), (330, 560)), fill=GUN, outline=EDGE, width=14)                         # receiver
d.polygon(P((760, 445), (960, 450), (960, 500), (770, 510)), fill=DARK, outline=EDGE, width=12)                                   # barrel shroud
d.polygon(P((940, 440), (1000, 440), (1000, 512), (940, 512)), fill=(150, 160, 175), outline=EDGE, width=10)                       # muzzle
d.polygon(P((430, 545), (520, 545), (500, 700), (420, 700)), fill=DARK, outline=EDGE, width=14)                                   # grip
d.polygon(P((560, 545), (660, 545), (650, 640), (570, 640)), fill=(30, 34, 44), outline=EDGE, width=12)                            # energy cell
d.polygon(P((575, 560), (645, 560), (638, 625), (582, 625)), fill=CY)
d.polygon(P((360, 470), (740, 462), (740, 490), (360, 498)), fill=CY)                                                             # plasma conduit
gd.polygon(P((360, 470), (740, 462), (740, 490), (360, 498)), fill=CY + (255,))
gd.polygon(P((575, 560), (645, 560), (638, 625), (582, 625)), fill=CY + (255,))
for i in range(5):
    x = 790 + i * 34
    d.polygon(P((x, 452), (x + 14, 452), (x + 14, 500), (x, 500)), fill=MAG)
    gd.polygon(P((x, 452), (x + 14, 452), (x + 14, 500), (x, 500)), fill=MAG + (200,))
d.polygon(P((420, 380), (640, 380), (650, 425), (410, 425)), fill=DARK, outline=EDGE, width=12)                                   # scope
d.polygon(P((620, 390), (650, 390), (650, 415), (620, 415)), fill=(255, 80, 80))
d.line(P((330, 455), (760, 448)), fill=(120, 130, 150), width=8)                                                                  # highlight
finish(im, 'plasma_rifle', glow)

# ---------------- Surgeon's Katana (diagonal, tip top-right)
im = Image.new('RGBA', (S, S)); d = ImageDraw.Draw(im)
glow = Image.new('RGBA', (S, S)); gd = ImageDraw.Draw(glow)
A = math.radians(-45)
def K(*pts): return rot_poly(pts, A)
BL = (215, 225, 235); BL2 = (150, 165, 180)
d.polygon(K((380, 480), (960, 468), (1010, 505), (960, 538), (380, 538)), fill=BL, outline=(60, 70, 85), width=10)    # blade
d.polygon(K((380, 520), (960, 520), (1010, 505), (960, 538), (380, 538)), fill=BL2)                               # edge bevel
d.line(K((390, 500), (950, 488)), fill=(255, 255, 255), width=6)                                                  # shine
gd.polygon(K((380, 495), (960, 480), (1010, 505), (960, 528), (380, 528)), fill=(120, 230, 255, 140))
for i in range(6):   # hamon / laser-etched ECG line
    x = 450 + i * 85
    d.line(K((x, 512), (x + 20, 498), (x + 35, 520), (x + 55, 505), (x + 85, 512)), fill=(60, 200, 255), width=5)
d.ellipse(K((330, 450), (330, 450))[0:1] and [rot_poly([(350, 510)], A)[0][0] - 55, rot_poly([(350, 510)], A)[0][1] - 55,
                                              rot_poly([(350, 510)], A)[0][0] + 55, rot_poly([(350, 510)], A)[0][1] + 55], fill=(212, 175, 55), outline=(90, 70, 10), width=10)   # tsuba
d.polygon(K((120, 485), (320, 485), (320, 535), (120, 535)), fill=(20, 20, 24), outline=(5, 5, 5), width=8)       # handle
for i in range(7):
    x = 135 + i * 26
    d.polygon(K((x, 488), (x + 14, 510), (x, 532), (x - 14, 510)), fill=(230, 40, 50))                            # red wrap diamonds
d.polygon(K((95, 488), (125, 488), (125, 532), (95, 532)), fill=(212, 175, 55), outline=(90, 70, 10), width=6)    # pommel
finish(im, 'surgeon_katana', glow)

# ---------------- Defibrillator Hammer (diagonal handle, head top-right)
im = Image.new('RGBA', (S, S)); d = ImageDraw.Draw(im)
glow = Image.new('RGBA', (S, S)); gd = ImageDraw.Draw(glow)
A = math.radians(-45)
def H(*pts): return rot_poly(pts, A)
d.polygon(H((90, 488), (700, 488), (700, 536), (90, 536)), fill=(70, 74, 82), outline=(15, 15, 18), width=10)      # shaft
for i in range(8):
    x = 110 + i * 34
    d.polygon(H((x, 486), (x + 18, 486), (x + 18, 538), (x, 538)), fill=(250, 200, 30))                           # hazard grip
d.polygon(H((640, 330), (930, 330), (950, 360), (950, 670), (930, 694), (640, 694), (620, 670), (620, 360)), fill=(225, 30, 40), outline=(60, 5, 10), width=14)   # head
d.polygon(H((660, 360), (910, 360), (910, 420), (660, 420)), fill=(245, 245, 250))                               # white band
d.polygon(H((660, 604), (910, 604), (910, 664), (660, 664)), fill=(245, 245, 250))
for (x0, x1) in ((690, 735), (835, 880)):                                                                         # paddles (strike faces)
    d.polygon(H((x0, 300), (x1, 300), (x1, 330), (x0, 330)), fill=(190, 195, 205), outline=(60, 60, 70), width=6)
    d.polygon(H((x0, 694), (x1, 694), (x1, 724), (x0, 724)), fill=(190, 195, 205), outline=(60, 60, 70), width=6)
# heart + lightning on the head
d.polygon(H((785, 450), (735, 480), (785, 575), (835, 480)), fill=(255, 255, 255))
d.polygon(H((800, 455), (765, 520), (790, 520), (770, 575), (815, 505), (790, 505)), fill=(255, 220, 40))
gd.polygon(H((640, 330), (930, 330), (950, 360), (950, 670), (930, 694), (640, 694), (620, 670), (620, 360)), fill=(80, 220, 255, 60))
for k in range(5):   # electric arcs
    pts = [(650 + j * 70, 300 - (k % 2) * 25 + ((j * 37 + k * 13) % 3) * 12) for j in range(5)]
    d.line(H(*pts), fill=(120, 230, 255), width=6)
finish(im, 'defibrillator_hammer', glow)
print('v4 weapon textures done')
