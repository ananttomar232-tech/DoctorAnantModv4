# HD ninja outfit (256x128 = 4x the 64x32 humanoid layout) worn by the sidekick over his player skin.
import numpy as np
from PIL import Image, ImageDraw
K = 4
img = np.zeros((32 * K, 64 * K, 4), np.float32)
rng = np.random.default_rng(5)
CLOTH = np.array((26, 27, 33)); CLOTH2 = np.array((40, 42, 50)); RED = np.array((170, 20, 30)); WRAP = np.array((200, 196, 186))
def R(u, v, w, h): return (u * K, v * K, w * K, h * K)
def cloth(r, base=CLOTH):
    x, y, w, h = r
    yy, xx = np.mgrid[0:h, 0:w]
    a = np.zeros((h, w, 4), np.float32); a[..., :3] = base; a[..., 3] = 255
    a[..., :3] += (np.sin(xx * 0.9 + yy * 0.25) * 3)[..., None] + rng.normal(0, 3, (h, w, 1))
    a[..., :3] += (np.sin(yy * 0.35 + np.sin(xx * 0.2) * 2) * 4)[..., None]        # folds
    img[y:y + h, x:x + w] = a
def band(r, row0, row1, col):
    x, y, w, h = r
    img[y + row0 * K:y + row1 * K, x:x + w, :3] = col + rng.normal(0, 4, (row1 * K - row0 * K, w, 1))
    img[y + row0 * K:y + row1 * K, x:x + w, 3] = 255
def wraps(r, row0, row1):
    x, y, w, h = r
    for yy in range(y + row0 * K, y + row1 * K):
        t = ((yy - y) // 3) % 2
        img[yy, x:x + w, :3] = WRAP * (0.92 if t else 1.0) + rng.normal(0, 3, (w, 1))
        img[yy, x:x + w, 3] = 255
    for yy in range(y + row0 * K, y + row1 * K, 5):
        img[yy, x:x + w, :3] *= 0.75
def clear(r):
    x, y, w, h = r; img[y:y + h, x:x + w] = 0
def faces(u, v, w, h, d):
    return dict(top=R(u + d, v, w, d), bottom=R(u + d + w, v, w, d), right=R(u, v + d, d, h), front=R(u + d, v + d, w, h),
                left=R(u + d + w, v + d, d, h), back=R(u + 2 * d + w, v + d, w, h))
# ---- head layer (0,0): transparent - the hood lives on the hat layer
for r in faces(0, 0, 8, 8, 8).values(): clear(r)
# ---- hood (hat layer, 32,0)
H = faces(32, 0, 8, 8, 8)
for k, r in H.items():
    if k == 'bottom': clear(r); continue
    cloth(r)
for k in ('front', 'left', 'right', 'back'):
    band(H[k], 2, 3, RED)                                             # red headband all round
x, y, w, h = H['front']
img[y + 3 * K + 1:y + 5 * K - 1, x + 1 * K:x + 7 * K] = 0          # eye slit shows the player's eyes
img[y + 3 * K:y + 3 * K + 1, x + K:x + 7 * K, :3] = (60, 10, 14)
x, y, w, h = H['back']                                               # headband knot + tails
im = Image.fromarray(img.clip(0, 255).astype(np.uint8)); d = ImageDraw.Draw(im)
d.polygon([(x + 3 * K, y + 2 * K), (x + 5 * K, y + 2 * K), (x + 5.5 * K, y + 7.8 * K), (x + 4.4 * K, y + 7.8 * K)], fill=(150, 15, 25, 255))
d.polygon([(x + 3.6 * K, y + 2 * K), (x + 4.6 * K, y + 2 * K), (x + 3.2 * K, y + 7.5 * K), (x + 2.2 * K, y + 7.5 * K)], fill=(185, 25, 35, 255))
d.ellipse([x + 3.2 * K, y + 1.7 * K, x + 4.8 * K, y + 3.3 * K], fill=(200, 30, 40, 255))
img = np.array(im).astype(np.float32)
# ---- body (16,16): gi with red-trimmed crossing lapels, sash, katana on the back
B = faces(16, 16, 8, 12, 4)
for r in B.values(): cloth(r)
for k in ('front', 'left', 'right', 'back'): band(B[k], 7, 9, RED)
im = Image.fromarray(img.clip(0, 255).astype(np.uint8)); d = ImageDraw.Draw(im)
x, y, w, h = B['front']
d.line([(x, y), (x + 5 * K, y + 7 * K)], fill=(150, 18, 28, 255), width=K)
d.line([(x + 8 * K, y), (x + 3 * K, y + 7 * K)], fill=(150, 18, 28, 255), width=K)
d.line([(x + 1 * K, y), (x + 5.5 * K, y + 6.5 * K)], fill=(60, 62, 72, 255), width=2)
d.polygon([(x + 5 * K, y + 7 * K), (x + 7 * K, y + 7 * K), (x + 7.5 * K, y + 11 * K), (x + 6.2 * K, y + 11 * K)], fill=(150, 15, 25, 255))   # sash tail
for i in range(3):                                                 # kunai in the sash
    d.line([(x + (1 + i) * K, y + 7 * K), (x + (1.3 + i) * K, y + 10.5 * K)], fill=(190, 195, 205, 255), width=2)
x, y, w, h = B['back']
d.line([(x + 0.5 * K, y + 11 * K), (x + 7.5 * K, y + 0.5 * K)], fill=(20, 16, 14, 255), width=int(1.6 * K))   # scabbard
d.line([(x + 0.5 * K, y + 11 * K), (x + 7.5 * K, y + 0.5 * K)], fill=(60, 40, 30, 255), width=2)
d.line([(x + 5.6 * K, y + 3 * K), (x + 7.2 * K, y + 4.2 * K)], fill=(212, 175, 55, 255), width=K)               # tsuba
d.line([(x + 6.4 * K, y + 1.6 * K), (x + 7.8 * K, y - 0.2 * K)], fill=(230, 40, 50, 255), width=K)              # red grip
img = np.array(im).astype(np.float32)
x, y, w, h = B['back']
img[y + 7 * K:y + 9 * K, x:x + w, :3] = RED                        # sash over the scabbard
# ---- arms (40,16): sleeves, bandage-wrapped forearms, dark gloves
A = faces(40, 16, 4, 12, 4)
for r in A.values(): cloth(r)
for k in ('front', 'left', 'right', 'back'):
    wraps(A[k], 6, 11); band(A[k], 11, 12, np.array((18, 18, 22)))
band(A['bottom'], 0, 4, np.array((18, 18, 22)))
# ---- legs (0,16): hakama pants, shin wraps, tabi boots
L = faces(0, 16, 4, 12, 4)
for r in L.values(): cloth(r, CLOTH2 * 0.7)
for k in ('front', 'left', 'right', 'back'):
    wraps(L[k], 7, 10); band(L[k], 10, 12, np.array((20, 20, 24)))
band(L['bottom'], 0, 4, np.array((20, 20, 24)))
x, y, w, h = L['front']
img[y + 10 * K:y + 12 * K, x + 2 * K - 1:x + 2 * K + 1, :3] = (60, 60, 66)   # tabi split toe
import os; os.makedirs('src/main/resources/assets/dranant/textures/entity', exist_ok=True)
Image.fromarray(img.clip(0, 255).astype(np.uint8), 'RGBA').save('src/main/resources/assets/dranant/textures/entity/ninja_outfit.png')
print('ninja outfit done')
