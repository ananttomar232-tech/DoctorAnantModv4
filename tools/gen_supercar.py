# Generates the supercar model (Java, with auto-packed UVs) AND its 4x HD textures from one geometry description,
# so the UV layout and the paint always match.  Run from the project root:  python3 tools/gen_supercar.py
#
# Model space: 1 unit = 1/16 block, y points DOWN, ground at y = 24, car front = -Z, model +X = the car's LEFT side.
import math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = 'src/main/resources/assets/dranant/textures/entity/supercar'
JAVA = 'src/main/java/com/dranant/client/model/SupercarModel.java'
S = 4            # texture scale (4x HD: 1 model pixel = 4x4 texels)
TEXW = 256       # layout width in model units
FB = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

# ------------------------------------------------------------------ geometry
class Cube:
    def __init__(self, key, x, y, z, w, h, d, style, side='C'):
        self.key, self.x, self.y, self.z, self.w, self.h, self.d, self.style, self.side = key, x, y, z, w, h, d, style, side

class Part:
    def __init__(self, name, parent, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=(), field=None):
        self.name, self.parent, self.pivot, self.rot, self.cubes, self.field = name, parent, pivot, rot, list(cubes), field

parts = []
def part(*a, **k):
    p = Part(*a, **k); parts.append(p); return p

def mirror_cube(c):
    return Cube(c.key + '_r', -(c.x + c.w), c.y, c.z, c.w, c.h, c.d, c.style, 'R')

def body_lr(key, x, y, z, w, h, d, style):
    """A cube on the car's left side (+X) plus its mirrored twin."""
    c = Cube(key, x, y, z, w, h, d, style, 'L')
    return [c, mirror_cube(c)]

body = []
C = lambda *a: body.append(Cube(*a))
WZ = 21   # wheel centre distance from the middle (wheelbase 42 px = 2.6 blocks)
C('floor', -14, 18, -30, 28, 2, 60, 'under')
C('core_rear', -11, 12, -30, 22, 6, 16, 'nose')
C('core_front', -11, 14, -35, 22, 4, 5, 'nose_front')
body += body_lr('fender_f', 11, 10, -28, 5, 2, 14, 'fender')
body += body_lr('corner_f', 11, 13, -33, 5, 5, 5, 'corner_f')
C('splitter', -14, 18, -36, 28, 1, 3, 'carbon')
C('grille', -8, 15, -36, 16, 3, 1, 'grille')
body += body_lr('skirt', 14, 18, -15, 2, 1, 30, 'carbon')
C('cabin_low', -15, 12, -14, 30, 6, 28, 'cabin_low')
C('roof', -13, 4, -3, 26, 1, 11, 'roof')
C('dash', -13, 10, -12, 26, 3, 4, 'dash')
C('console', -2, 13, -10, 4, 4, 16, 'console')
body += body_lr('seat', 2, 14, -3, 9, 3, 8, 'seat')
body += body_lr('seat_back', 2, 6, 4, 9, 8, 2, 'seat_back')
C('bulkhead', -13, 5, 7, 26, 7, 1, 'interior')
body += body_lr('quarter', 12, 5, 6, 2, 7, 8, 'quarter')
C('rear', -11, 12, 14, 22, 6, 18, 'rear')
body += body_lr('fender_r', 11, 10, 14, 5, 2, 18, 'fender')
body += body_lr('corner_r', 11, 12, 28, 5, 6, 4, 'corner_r')
C('diffuser', -14, 18, 30, 28, 2, 4, 'diffuser')
C('tailbar', -15, 12, 32, 30, 2, 1, 'taillight')
C('plate', -4, 14, 32, 8, 2, 1, 'plate')
body += body_lr('exhaust', 3, 16, 32, 3, 2, 2, 'exhaust')
part('body', 'root', (0, 0, 0), cubes=body)

part('hood', 'body', (0, 12, -14), cubes=[Cube('hood', -11, -1, -16, 22, 1, 16, 'hood')], field='hood')
part('nose_slope', 'hood', (0, -1, -16), (0.42, 0, 0), cubes=[Cube('nose_slope', -11, 0, -7, 22, 1, 7, 'nose_slope')])
part('trunk', 'body', (0, 12, 20), cubes=[Cube('trunk', -11, -1, 0, 22, 1, 12, 'trunk')], field='trunk')
part('windshield', 'body', (0, 12, -14), (-0.98, 0, 0), cubes=[Cube('windshield', -12, -13, -0.5, 24, 13, 1, 'glass_front')])
part('rear_glass', 'body', (0, 4.5, 8), (1.08, 0, 0), cubes=[Cube('rear_glass', -12, 0, -0.5, 24, 13, 1, 'glass_rear')])
for s, sx in (('l', 1), ('r', -1)):
    def lr(c):
        return c if sx > 0 else mirror_cube(c)
    part(f'a_pillar_{s}', 'body', (13 * sx, 12, -14), (-0.98, 0, 0), cubes=[lr(Cube('a_pillar', -1, -13, -0.5, 2, 13, 1, 'pillar', 'L'))])
    part(f'c_pillar_{s}', 'body', (13 * sx, 4.5, 8), (1.08, 0, 0), cubes=[lr(Cube('c_pillar', -1, 0, -0.5, 2, 13, 1, 'fender', 'L'))])
    part(f'fender_slope_{s}', 'body', (11 * sx if sx > 0 else -16, 10, -28), (0.45, 0, 0), cubes=[Cube('fender_slope' + ('' if sx > 0 else '_r'), 0, 0, -6, 5, 2, 6, 'fender', 'L' if sx > 0 else 'R')])
    part(f'headlight_{s}', f'fender_slope_{s}', (0, 0, 0), cubes=[Cube('headlight' + ('' if sx > 0 else '_r'), 0.5, -0.5, -5.5, 4, 1, 4, 'headlight', 'L' if sx > 0 else 'R')])
    part(f'door_{s}', 'body', (15.5 * sx, 12, -12), cubes=[lr(Cube('door', -0.5, 0, 0, 1, 6, 18, 'door', 'L')),
                                                         lr(Cube('mirror', 0.5, -2, 1, 3, 2, 1, 'mirror', 'L'))], field=f'door_{s}')
    part(f'door_{s}_glass', f'door_{s}', (0, 0, 0), (0, 0, -0.3 * sx), cubes=[lr(Cube('door_glass', -0.5, -8, 2, 1, 8, 14, 'glass_side', 'L'))])
# steering wheel (driver = left = +X)
part('steer', 'body', (6.5, 10.5, -7.5), (-0.35, 0, 0), cubes=[Cube('column', -0.5, -0.5, -3.5, 1, 1, 3, 'black')])
part('steer_wheel', 'steer', (0, 0, 0), cubes=[Cube('steering', -3, -3, -0.5, 6, 6, 1, 'steering')], field='steering_wheel')
# wheels: steer pivot -> spin -> 4 rotated slabs (16-gon tyre with cut-out circular rim face)
for name, x, z in (('fl', 13.5, -WZ), ('fr', -13.5, -WZ), ('rl', 13.5, WZ), ('rr', -13.5, WZ)):
    part(f'wheel_{name}', 'body', (x, 17.5, z), field=f'wheel_{name}')
    part(f'wheel_{name}_spin', f'wheel_{name}', (0, 0, 0), field=f'wheel_{name}_spin')
    for k in range(4):
        w = 5 if k == 0 else 4.8
        part(f'wheel_{name}_s{k}', f'wheel_{name}_spin', (0, 0, 0), (k * math.pi / 8, 0, 0),
             cubes=[Cube(f'wheel{k}', -w / 2, -6.5, -6.5, w, 13, 13, 'wheel' if k == 0 else 'tread')])
# aero per variant
# (mounted on the boot lid so they lift with it; trunk pivot = (0, 12, 20))
part('wing_gt', 'trunk', (0, -12, -20), cubes=[Cube('wing_plane', -14, 5, 26, 28, 1, 6, 'carbon'),
                                          *body_lr('wing_end', 14, 3, 26, 1, 4, 6, 'carbon'),
                                          *body_lr('wing_strut', 6, 6, 28, 1, 5, 2, 'black')], field='wing_gt')
part('ducktail', 'trunk', (0, -12, -20), cubes=[Cube('ducktail', -11, 10, 29, 22, 1, 3, 'carbon')], field='ducktail')
part('active_wing', 'trunk', (0, -1, 8), cubes=[Cube('active_wing', -12, -1, -2, 24, 1, 5, 'carbon_gold'),
                                                *body_lr('active_strut', 5, 0, 0, 1, 2, 1, 'black')], field='active_wing')

# ------------------------------------------------------------------ UV packing (shelf packer, shared keys)
uv = {}
sizes = {}
for p in parts:
    for c in p.cubes:
        if c.key not in sizes:
            sizes[c.key] = (math.ceil(2 * (c.d + c.w)), math.ceil(c.d + c.h), c)
order = sorted(sizes, key=lambda k: (-sizes[k][1], -sizes[k][0]))
x = y = shelf = 0
for k in order:
    w, h, _ = sizes[k]
    if x + w > TEXW:
        x, y, shelf = 0, y + shelf, 0
    uv[k] = (x, y)
    x += w
    shelf = max(shelf, h)
TEXH = 1 << math.ceil(math.log2(y + shelf))
print('UV layout', TEXW, 'x', TEXH)

# ------------------------------------------------------------------ Java model
def f(v):
    s = ('%.4f' % v).rstrip('0').rstrip('.')
    return (s if s not in ('-0', '') else '0') + 'F'

def emit_parts():
    out = []
    var = {'root': 'root'}
    for p in parts:
        cubes = ''.join(f"\n                .texOffs({uv[c.key][0]}, {uv[c.key][1]}).addBox({f(c.x)}, {f(c.y)}, {f(c.z)}, {f(c.w)}, {f(c.h)}, {f(c.d)})" for c in p.cubes)
        px, py, pz = p.pivot
        rx, ry, rz = p.rot
        pose = f"PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})" if any(p.rot) else f"PartPose.offset({f(px)}, {f(py)}, {f(pz)})"
        v = 'p_' + p.name
        var[p.name] = v
        out.append(f"        PartDefinition {v} = {var[p.parent]}.addOrReplaceChild(\"{p.name}\", CubeListBuilder.create(){cubes}, {pose});")
    return '\n'.join(out)

def path_to(name):
    chain = []
    p = next(q for q in parts if q.name == name)
    while p.parent != 'root':
        chain.append(p.name)
        p = next(q for q in parts if q.name == p.parent)
    chain.append(p.name)
    return 'root' + ''.join(f'.getChild("{n}")' for n in reversed(chain))

fields = [p for p in parts if p.field]
def camel(s):
    a = s.split('_'); return a[0] + ''.join(t.capitalize() for t in a[1:])
field_decl = '\n'.join(f'    private final ModelPart {camel(p.field)};' for p in fields)
field_init = '\n'.join(f'        this.{camel(p.field)} = {path_to(p.name)};' for p in fields)

java = f'''package com.dranant.client.model;

import com.dranant.DoctorAnantMod;
import com.dranant.entity.SupercarEntity;
import com.dranant.entity.SupercarVariant;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Supercar model - GENERATED by tools/gen_supercar.py together with its {S}x HD textures (do not edit the layer by hand).
 * Front = -Z, +X = the car's left (driver) side. Doors open per model: scissor (Veloce), conventional (GT), gullwing (Phantom).
 * Bonnet and boot lift, front wheels steer, all wheels spin, the steering wheel turns, the Phantom's active wing rises at speed.
 */
public class SupercarModel extends HierarchicalModel<SupercarEntity> {{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(DoctorAnantMod.id("supercar"), "main");
    public static final int TEX_WIDTH = {TEXW}, TEX_HEIGHT = {TEXH};

    private final ModelPart root;
{field_decl}

    public SupercarModel(ModelPart root) {{
        this.root = root;
{field_init}
    }}

    public static LayerDefinition createLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{emit_parts()}
        return LayerDefinition.create(mesh, TEX_WIDTH, TEX_HEIGHT);
    }}

    @Override
    public ModelPart root() {{
        return root;
    }}

    @Override
    public void setupAnim(SupercarEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {{
    }}

    /** Poses every moving part for this frame. */
    public void pose(SupercarEntity car, float partialTick) {{
        root.getAllParts().forEach(ModelPart::resetPose);
        SupercarVariant variant = car.getVariant();
        float spin = car.getWheelSpin(partialTick);
        float steer = car.getVisualSteer(partialTick);
        for (ModelPart w : new ModelPart[]{{wheelFlSpin, wheelFrSpin, wheelRlSpin, wheelRrSpin}}) w.xRot = -spin;
        wheelFl.yRot = steer * 0.55F;
        wheelFr.yRot = steer * 0.55F;
        steeringWheel.zRot = -steer * 2.4F;

        wingGt.visible = variant == SupercarVariant.GT;
        ducktail.visible = variant == SupercarVariant.VELOCE;
        activeWing.visible = variant == SupercarVariant.PHANTOM;
        activeWing.y -= 3.5F * car.getWingLift(partialTick);

        hood.xRot = -1.05F * ease(car.getPartOpen(SupercarEntity.PART_HOOD, partialTick));
        trunk.xRot = 1.0F * ease(car.getPartOpen(SupercarEntity.PART_TRUNK, partialTick));
        poseDoor(doorL, 1, ease(car.getPartOpen(SupercarEntity.PART_DOOR_LEFT, partialTick)), variant);
        poseDoor(doorR, -1, ease(car.getPartOpen(SupercarEntity.PART_DOOR_RIGHT, partialTick)), variant);
    }}

    private static void poseDoor(ModelPart door, int side, float t, SupercarVariant variant) {{
        if (t <= 0.0F) return;
        switch (variant.doorStyle()) {{
            case SCISSOR -> door.xRot = 1.25F * t;                    // swings straight up about the front hinge
            case CONVENTIONAL -> door.yRot = 1.15F * t * side;       // swings outward
            case GULLWING -> {{
                // hinge along the roof edge: rotate about (13*side, 4.5) instead of the door's own pivot
                float a = -1.45F * t * side;
                float vx = (13.0F - 15.5F) * side, vy = 4.5F - 11.0F;
                float cos = Mth.cos(a), sin = Mth.sin(a);
                door.zRot = a;
                door.x += vx - (vx * cos - vy * sin);
                door.y += vy - (vx * sin + vy * cos);
            }}
        }}
    }}

    private static float ease(float t) {{
        return t * t * (3.0F - 2.0F * t);
    }}
}}
'''
open(JAVA, 'w').write(java)
print('wrote', JAVA)

# ------------------------------------------------------------------ painting
VARIANTS = {
    'veloce':  dict(paint=(206, 14, 26), paint2=(120, 4, 10), accent=(18, 18, 20), stripe=None, metal=0.55,
                    rim=(28, 28, 30), lip=(205, 210, 220), caliper=(255, 200, 0), leather=(24, 24, 26), stitch=(220, 30, 40), spokes=5, badge=(255, 210, 60)),
    'gt':      dict(paint=(18, 92, 225), paint2=(8, 40, 120), accent=(20, 20, 24), stripe=(245, 245, 250), metal=0.6,
                    rim=(200, 205, 214), lip=(235, 238, 245), caliper=(220, 20, 30), leather=(22, 22, 26), stitch=(40, 120, 255), spokes=10, badge=(230, 230, 240)),
    'phantom': dict(paint=(44, 44, 50), paint2=(12, 12, 14), pin=(212, 175, 55), accent=(212, 175, 55), stripe=None, metal=0.15,
                    rim=(212, 175, 55), lip=(240, 215, 120), caliper=(212, 175, 55), leather=(226, 211, 180), stitch=(160, 120, 40), spokes=20, badge=(212, 175, 55)),
}
rng = np.random.default_rng(7)

def faces(c):
    """Face rectangles in texels for a cube: name -> (x0, y0, w, h)."""
    u, v = uv[c.key]
    d, w, h = c.d, c.w, c.h
    R = lambda a, b, ww, hh: (round(a * S), round(b * S), max(1, round(ww * S)), max(1, round(hh * S)))
    return {'top': R(u + d, v, w, d), 'bottom': R(u + d + w, v, w, d), 'minx': R(u, v + d, d, h), 'front': R(u + d, v + d, w, h),
            'maxx': R(u + d + w, v + d, d, h), 'back': R(u + 2 * d + w, v + d, w, h)}

def outer(c, face):
    if c.side == 'L': return face == 'maxx'
    if c.side == 'R': return face == 'minx'
    return face in ('minx', 'maxx')

def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

class Painter:
    def __init__(self, V):
        self.V = V
        self.img = np.zeros((TEXH * S, TEXW * S, 4), dtype=np.float32)
        self.glow = np.zeros_like(self.img)
        self.lights = np.zeros_like(self.img)
        self.glass = np.zeros_like(self.img)

    # --- primitives
    def rect(self, arr, r, col, a=255):
        x0, y0, w, h = r
        arr[y0:y0 + h, x0:x0 + w, :3] = col
        arr[y0:y0 + h, x0:x0 + w, 3] = a

    def grad(self, arr, r, c1, c2, vertical=True):
        x0, y0, w, h = r
        t = np.linspace(0, 1, h if vertical else w)
        g = np.array(c1)[None, :] * (1 - t[:, None]) + np.array(c2)[None, :] * t[:, None]
        if vertical: arr[y0:y0 + h, x0:x0 + w, :3] = g[:, None, :]
        else: arr[y0:y0 + h, x0:x0 + w, :3] = g[None, :, :]
        arr[y0:y0 + h, x0:x0 + w, 3] = 255

    def noise(self, arr, r, amt):
        x0, y0, w, h = r
        n = rng.normal(0, amt, (h, w, 1))
        arr[y0:y0 + h, x0:x0 + w, :3] = np.clip(arr[y0:y0 + h, x0:x0 + w, :3] + n, 0, 255)

    def edge(self, arr, r, amt=0.55, px=2):
        """Soft ambient-occlusion border so each panel reads crisply."""
        x0, y0, w, h = r
        sub = arr[y0:y0 + h, x0:x0 + w, :3]
        for i in range(min(px, w // 2, h // 2)):
            k = amt * (1 - i / px)
            sub[i, :, :] *= (1 - k); sub[-1 - i, :, :] *= (1 - k)
            sub[:, i, :] *= (1 - k); sub[:, -1 - i, :] *= (1 - k)

    def shine(self, arr, r, strength, pos=0.3, width=0.12, horizontal=True):
        x0, y0, w, h = r
        n = h if horizontal else w
        t = np.linspace(0, 1, n)
        band = np.exp(-((t - pos) / width) ** 2) * strength
        sub = arr[y0:y0 + h, x0:x0 + w, :3]
        if horizontal: sub += (255 - sub) * band[:, None, None]
        else: sub += (255 - sub) * band[None, :, None]

    def draw(self, arr, r, fn):
        """Run a PIL drawing callback on a face (supersampled 4x for smooth edges)."""
        x0, y0, w, h = r
        K = 4
        tile = Image.fromarray(arr[y0:y0 + h, x0:x0 + w].clip(0, 255).astype(np.uint8), 'RGBA').resize((w * K, h * K), Image.NEAREST)
        dd = ImageDraw.Draw(tile)
        fn(dd, w * K, h * K, K)
        arr[y0:y0 + h, x0:x0 + w] = np.array(tile.resize((w, h), Image.LANCZOS)).astype(np.float32)

    # --- materials
    def paint(self, r, face, light=1.0):
        V = self.V
        base = np.array(V['paint'], dtype=np.float32)
        dark = np.array(V['paint2'], dtype=np.float32)
        if face == 'top':
            self.grad(self.img, r, mix(V['paint'], (255, 255, 255), 0.12 * V['metal'] + 0.04), V['paint'], vertical=False)
            self.shine(self.img, r, 0.35 * V['metal'] + 0.05, pos=0.42, width=0.18, horizontal=False)
        elif face in ('minx', 'maxx'):
            self.grad(self.img, r, mix(V['paint'], (255, 255, 255), 0.08), tuple(dark), vertical=True)
            self.shine(self.img, r, 0.45 * V['metal'] + 0.06, pos=0.22, width=0.08)
        else:
            self.grad(self.img, r, tuple(base * 0.95), tuple(dark), vertical=True)
            self.shine(self.img, r, 0.25 * V['metal'], pos=0.25, width=0.1)
        self.noise(self.img, r, 3.0 + 4.0 * V['metal'])
        if V.get('pin') and face in ('minx', 'maxx'):
            x0, y0, w, h = r
            yy = y0 + int(h * 0.3)
            self.img[yy:yy + 2, x0:x0 + w, :3] = V['pin']
        self.edge(self.img, r, 0.35, 2)

    def stripes(self, r, face, cube):
        """GT racing stripes on centre panels (top faces run front-back)."""
        if not self.V['stripe'] or cube.side != 'C': return
        x0, y0, w, h = r
        def model_to_tex_x(mx):
            if face == 'top' or face == 'front': return x0 + (mx - cube.x) / cube.w * w
            if face == 'back': return x0 + (cube.x + cube.w - mx) / cube.w * w
            return None
        for lo, hi in ((-4.5, -1.5), (1.5, 4.5)):
            a, b = model_to_tex_x(lo), model_to_tex_x(hi)
            if a is None: return
            a, b = sorted((int(a), int(b)))
            if b <= x0 or a >= x0 + w: continue
            a, b = max(a, x0), min(b, x0 + w)
            self.img[y0:y0 + h, a:b, :3] = self.img[y0:y0 + h, a:b, :3] * 0.08 + np.array(self.V['stripe']) * 0.92

    def carbon(self, r, gold=False):
        x0, y0, w, h = r
        yy, xx = np.mgrid[0:h, 0:w]
        weave = (((xx // 3) + (yy // 3)) % 2).astype(np.float32)
        tone = 28 + 18 * weave + 10 * np.sin((xx + yy) * 0.6)
        sub = self.img[y0:y0 + h, x0:x0 + w]
        sub[..., 0] = tone; sub[..., 1] = tone; sub[..., 2] = tone + 4; sub[..., 3] = 255
        self.shine(self.img, r, 0.18, pos=0.3, width=0.15)
        if gold:
            self.rect(self.img, (x0, y0 + h - max(1, h // 8), w, max(1, h // 8)), self.V['accent'])
        self.edge(self.img, r, 0.4, 2)

    def black(self, r, tone=22):
        self.grad(self.img, r, (tone + 14,) * 3, (tone,) * 3)
        self.noise(self.img, r, 2)
        self.edge(self.img, r, 0.3, 1)

    def chrome(self, r):
        self.grad(self.img, r, (245, 247, 250), (120, 125, 135))
        self.shine(self.img, r, 0.6, pos=0.35, width=0.08)
        self.edge(self.img, r, 0.3, 1)

    def leather(self, r, quilted=True):
        V = self.V
        self.grad(self.img, r, mix(V['leather'], (255, 255, 255), 0.12), mix(V['leather'], (0, 0, 0), 0.25))
        self.noise(self.img, r, 5)
        x0, y0, w, h = r
        if quilted and w > 8 and h > 8:
            st = V['stitch']
            for yy in range(y0 + 4, y0 + h - 2, 8):
                for xx in range(x0 + 2, x0 + w - 2, 3):
                    self.img[yy, xx, :3] = st
            for xx in range(x0 + 4, x0 + w - 2, 10):
                for yy in range(y0 + 2, y0 + h - 2, 3):
                    self.img[yy, xx, :3] = st
        self.edge(self.img, r, 0.45, 2)

    def glass_face(self, r, tint=(30, 45, 60), alpha=110, frame=2):
        x0, y0, w, h = r
        self.rect(self.img, r, (0, 0, 0), 0)                 # fully clear in the opaque pass
        sub = self.glass[y0:y0 + h, x0:x0 + w]
        sub[..., :3] = tint; sub[..., 3] = alpha
        yy, xx = np.mgrid[0:h, 0:w]
        t = (xx / max(1, w) + yy / max(1, h) * 0.6)
        streak = np.exp(-((t - 0.45) / 0.05) ** 2) + 0.6 * np.exp(-((t - 0.62) / 0.025) ** 2)
        sub[..., :3] += streak[..., None] * 110
        sub[..., 3] += streak * 70
        # black ceramic frit border, opaque
        f = frame
        for arr, a in ((self.img, 255),):
            arr[y0:y0 + f, x0:x0 + w] = (12, 12, 14, a); arr[y0 + h - f:y0 + h, x0:x0 + w] = (12, 12, 14, a)
            arr[y0:y0 + h, x0:x0 + f] = (12, 12, 14, a); arr[y0:y0 + h, x0 + w - f:x0 + w] = (12, 12, 14, a)

    def text(self, arr, r, s, col, size_frac=0.7, bg=None):
        def fn(dd, W, H, K):
            if bg: dd.rectangle([0, 0, W, H], fill=bg)
            font = ImageFont.truetype(FB, max(6, int(H * size_frac)))
            dd.text((W / 2, H / 2), s, font=font, fill=col, anchor='mm')
        self.draw(arr, r, fn)

    # --- per-style faces
    def cube(self, c):
        F = faces(c)
        st = c.style
        V = self.V
        for face, r in F.items():
            o = outer(c, face)
            if st in ('fender', 'corner_r', 'corner_f', 'hood', 'trunk', 'roof', 'cabin_low', 'nose', 'rear', 'quarter', 'door'):
                self.paint(r, face)
                self.stripes(r, face, c)
            if st == 'under': self.black(r, 14)
            elif st == 'nose':
                if face == 'top': self.engine_bay(r, front=True)
                elif face in ('minx', 'maxx'): self.black(r, 16)
            elif st == 'nose_front':
                if face == 'front': self.nose_front(r)
                else: self.black(r, 16)
            elif st == 'nose_slope':
                self.paint(r, face); self.stripes(r, face, c)
                if face == 'top': self.badge(r)
            elif st == 'rear':
                if face == 'top': self.engine_bay(r, front=False)
                elif face in ('minx', 'maxx'): self.black(r, 16)
                elif face == 'back': self.black(r, 20)
            elif st == 'corner_f' and face == 'front':
                self.intake(r)
            elif st == 'corner_r' and face == 'back':
                self.intake(r, rows=2)
            elif st == 'cabin_low':
                if face in ('minx', 'maxx'): self.side_intake(r, face)
            elif st == 'roof':
                if V['stripe'] is None and face == 'top':
                    self.grad(self.img, r, (30, 30, 34), (14, 14, 16), vertical=False); self.shine(self.img, r, 0.2, 0.45, 0.2, False)
                if face == 'bottom': self.headliner(r)
            elif st == 'hood' and face == 'top':
                self.hood_detail(r, c)
            elif st == 'hood' and face == 'bottom': self.black(r, 30)
            elif st == 'trunk' and face == 'top':
                self.louvres(r)
            elif st == 'trunk' and face == 'bottom': self.black(r, 30)
            elif st == 'door':
                if o: self.door_outer(r, face, c)
                elif face in ('minx', 'maxx'): self.door_inner(r)
            elif st == 'quarter' and o:
                self.glass_face(r, alpha=150, frame=2)
            elif st == 'carbon': self.carbon(r)
            elif st == 'carbon_gold': self.carbon(r, gold=True)
            elif st == 'diffuser':
                self.carbon(r)
                if face == 'back': self.diffuser_fins(r)
            elif st == 'grille': self.grille(r) if face == 'front' else self.black(r)
            elif st == 'headlight': self.headlight(r) if face == 'top' else self.black(r, 30)
            elif st == 'taillight': self.taillight(r) if face == 'back' else self.black(r, 25)
            elif st == 'plate':
                if face == 'back': self.text(self.img, r, 'DR ANANT', (20, 20, 30), 0.62, bg=(245, 245, 240))
                else: self.black(r)
            elif st == 'exhaust':
                self.chrome(r)
                if face == 'back': self.exhaust_tip(r)
            elif st == 'black': self.black(r)
            elif st == 'pillar': self.black(r, 18)
            elif st == 'mirror':
                self.paint(r, face)
                if face == 'back': self.mirror_glass(r)
            elif st == 'dash': self.dash(r, face)
            elif st == 'console': self.console(r, face)
            elif st == 'interior': self.leather(r, quilted=False)
            elif st in ('seat', 'seat_back'): self.leather(r)
            elif st == 'steering': self.steering(r, face)
            elif st in ('glass_front', 'glass_rear', 'glass_side'):
                if face in ('front', 'back', 'minx', 'maxx', 'top', 'bottom') and (face in ('front', 'back') if st != 'glass_side' else face in ('minx', 'maxx')):
                    self.glass_face(r, alpha=95 if st == 'glass_front' else 130, frame=3 if st != 'glass_side' else 2)
                else: self.black(r, 12)
            elif st == 'wheel': self.wheel(r, face, rim=True)
            elif st == 'tread': self.wheel(r, face, rim=False)

    # --- detailed decals
    def engine_bay(self, r, front):
        x0, y0, w, h = r
        self.black(r, 26)
        V = self.V
        def fn(dd, W, H, K):
            if front and V['stripe']:   # GT: front-engine V8 with blue intake plenum
                dd.rounded_rectangle([W * 0.18, H * 0.15, W * 0.82, H * 0.85], radius=6 * K, fill=(55, 58, 64))
                for i in range(4):
                    yy = H * (0.22 + i * 0.16)
                    dd.rounded_rectangle([W * 0.24, yy, W * 0.46, yy + H * 0.1], radius=3 * K, fill=V['paint'])
                    dd.rounded_rectangle([W * 0.54, yy, W * 0.76, yy + H * 0.1], radius=3 * K, fill=V['paint'])
                dd.ellipse([W * 0.42, H * 0.4, W * 0.58, H * 0.6], fill=(190, 195, 205))
            elif front:                 # frunk: luggage bay with leather bag
                dd.rectangle([W * 0.08, H * 0.1, W * 0.92, H * 0.9], fill=(32, 32, 36))
                dd.rounded_rectangle([W * 0.25, H * 0.3, W * 0.75, H * 0.75], radius=8 * K, fill=mix(V['leather'], (90, 60, 30), 0.5))
                dd.line([W * 0.25, H * 0.5, W * 0.75, H * 0.5], fill=V['stitch'], width=2 * K)
            else:                       # mid-engine V12: red crinkle cam covers + carbon airbox
                for i in range(6):
                    yy = H * (0.12 + i * 0.13)
                    dd.rounded_rectangle([W * 0.12, yy, W * 0.4, yy + H * 0.09], radius=3 * K, fill=(170, 18, 24))
                    dd.rounded_rectangle([W * 0.6, yy, W * 0.88, yy + H * 0.09], radius=3 * K, fill=(170, 18, 24))
                dd.rounded_rectangle([W * 0.43, H * 0.1, W * 0.57, H * 0.9], radius=4 * K, fill=(45, 45, 50))
                font = ImageFont.truetype(FB, int(min(W, H) * 0.08))
                dd.text((W * 0.5, H * 0.5), 'V12', font=font, fill=(230, 230, 230), anchor='mm')
        self.draw(self.img, r, fn)

    def nose_front(self, r):
        V = self.V
        self.paint(r, 'front')
        def fn(dd, W, H, K):
            dd.rounded_rectangle([W * 0.04, H * 0.45, W * 0.96, H * 0.98], radius=4 * K, fill=(14, 14, 16))
            for i in range(12):
                xx = W * (0.1 + i * 0.07)
                dd.line([xx, H * 0.5, xx, H * 0.95], fill=(40, 40, 46), width=K)
        self.draw(self.img, r, fn)

    def intake(self, r, rows=3):
        self.black(r, 16)
        def fn(dd, W, H, K):
            for i in range(rows * 3):
                yy = H * (0.15 + i * 0.7 / (rows * 3))
                dd.line([W * 0.1, yy, W * 0.9, yy], fill=(55, 55, 62), width=K)
        self.draw(self.img, r, fn)

    def side_intake(self, r, face):
        x0, y0, w, h = r
        # the visible part behind the door (rear 8 px of the 28 px side) gets a big sculpted air intake
        rear_frac = 8 / 28
        if face == 'minx': ix0, ix1 = 0.0, rear_frac          # WEST: left = rear
        else: ix0, ix1 = 1 - rear_frac, 1.0   # EAST: right = rear
        def fn(dd, W, H, K):
            dd.polygon([(W * (ix0 + 0.01), H * 0.25), (W * (ix1 - 0.01), H * 0.12), (W * (ix1 - 0.01), H * 0.9), (W * (ix0 + 0.01), H * 0.75)], fill=(12, 12, 14))
            for i in range(5):
                yy = H * (0.3 + i * 0.1)
                dd.line([W * (ix0 + 0.02), yy, W * (ix1 - 0.02), yy - H * 0.04], fill=(48, 48, 54), width=K)
        self.draw(self.img, r, fn)

    def headliner(self, r):
        self.grad(self.img, r, (60, 58, 56), (40, 38, 36))
        self.noise(self.img, r, 4)
        x0, y0, w, h = r
        # ambient star-light dots (glow layer)
        for _ in range(w * h // 60):
            xx, yy = x0 + rng.integers(1, w - 1), y0 + rng.integers(1, h - 1)
            self.glow[yy, xx] = (200, 210, 255, 255)

    def hood_detail(self, r, c):
        V = self.V
        def fn(dd, W, H, K):
            # top face: left=minX, top=rear, bottom=front. Twin vents + badge near the front edge
            for sx in (0.22, 0.62):
                dd.rounded_rectangle([W * sx, H * 0.18, W * (sx + 0.16), H * 0.45], radius=3 * K, fill=(16, 16, 18))
                for i in range(4):
                    yy = H * (0.22 + i * 0.06)
                    dd.line([W * (sx + 0.01), yy, W * (sx + 0.15), yy], fill=(60, 60, 66), width=K)
        self.draw(self.img, r, fn)

    def badge(self, r):
        V = self.V
        def fn(dd, W, H, K):
            cx, cy, rr = W * 0.5, H * 0.45, min(W, H) * 0.16
            dd.polygon([(cx, cy - rr), (cx + rr * 0.8, cy - rr * 0.3), (cx + rr * 0.55, cy + rr), (cx - rr * 0.55, cy + rr), (cx - rr * 0.8, cy - rr * 0.3)], fill=V['badge'], outline=(40, 30, 0))
            font = ImageFont.truetype(FB, int(rr * 1.2))
            dd.text((cx, cy + rr * 0.15), 'A', font=font, fill=(30, 20, 0), anchor='mm')
        self.draw(self.img, r, fn)

    def louvres(self, r):
        def fn(dd, W, H, K):
            for i in range(7):
                yy = H * (0.1 + i * 0.1)
                dd.rounded_rectangle([W * 0.12, yy, W * 0.88, yy + H * 0.05], radius=2 * K, fill=(14, 14, 16))
        self.draw(self.img, r, fn)

    def door_outer(self, r, face, c):
        V = self.V
        # MC face orientation: maxx (EAST) left=front, minx (WEST) left=rear
        rear_left = face == 'minx'
        def fn(dd, W, H, K):
            # character line, flush handle at the rear top, a script badge
            dd.line([0, H * 0.42, W, H * 0.32], fill=mix(V['paint'], (255, 255, 255), 0.35), width=K)
            hx = W * (0.08 if rear_left else 0.78)
            dd.rounded_rectangle([hx, H * 0.12, hx + W * 0.14, H * 0.24], radius=2 * K, fill=(20, 20, 22))
            dd.rounded_rectangle([hx + K, H * 0.14, hx + W * 0.14 - K, H * 0.22], radius=2 * K, fill=(170, 175, 185))
            font = ImageFont.truetype(FB, int(H * 0.16))
            dd.text((W * 0.5, H * 0.72), 'ANANT', font=font, fill=V['badge'], anchor='mm')
        self.draw(self.img, r, fn)

    def door_inner(self, r):
        self.leather(r)
        def fn(dd, W, H, K):
            dd.rounded_rectangle([W * 0.3, H * 0.2, W * 0.7, H * 0.4], radius=3 * K, fill=(30, 30, 34))
            dd.rectangle([W * 0.05, H * 0.6, W * 0.95, H * 0.66], fill=self.V['accent'])
        self.draw(self.img, r, fn)
        x0, y0, w, h = r
        self.glow[y0 + int(h * 0.6):y0 + int(h * 0.66), x0:x0 + w] = (90, 200, 255, 255)   # ambient strip

    def grille(self, r):
        self.black(r, 10)
        def fn(dd, W, H, K):
            s = max(3, H // 4)
            for yy in range(0, H, s):
                for xx in range((yy // s % 2) * s // 2, W, s):
                    dd.regular_polygon((xx, yy, s * 0.42), 6, fill=(38, 38, 44))
        self.draw(self.img, r, fn)

    def headlight(self, r):
        """Top face of the lamp unit sitting on the sloped fender (image bottom = front edge)."""
        x0, y0, w, h = r
        self.grad(self.img, r, (70, 74, 82), (24, 26, 30))
        lens = [(0.28, 0.42), (0.5, 0.42), (0.72, 0.42)]
        def fn(dd, W, H, K):
            dd.polygon([(W * 0.08, H * 0.25), (W * 0.92, H * 0.3), (W * 0.92, H * 0.58), (W * 0.08, H * 0.55)], fill=(12, 14, 18))
            for cx, cy in lens:
                rr = W * 0.075
                dd.rectangle([W * cx - rr, H * cy - rr, W * cx + rr, H * cy + rr], fill=(210, 220, 235))
            dd.rectangle([W * 0.04, H * 0.8, W * 0.96, H * 0.9], fill=(235, 240, 248))
            dd.rectangle([W * 0.04, H * 0.62, W * 0.1, H * 0.9], fill=(235, 240, 248))
        self.draw(self.img, r, fn)
        # DRL blade always glows; the projectors light up with the headlights
        G = self.glow
        G[y0 + int(h * 0.8):y0 + int(h * 0.9), x0 + int(w * 0.04):x0 + int(w * 0.96)] = (230, 240, 255, 255)
        G[y0 + int(h * 0.62):y0 + int(h * 0.9), x0 + int(w * 0.04):x0 + int(w * 0.1)] = (230, 240, 255, 255)
        self.lights[y0 + int(h * 0.27):y0 + int(h * 0.57), x0 + int(w * 0.1):x0 + int(w * 0.9)] = (255, 250, 235, 255)

    def taillight(self, r):
        x0, y0, w, h = r
        self.grad(self.img, r, (90, 6, 10), (40, 0, 4))
        def fn(dd, W, H, K):
            dd.rectangle([0, H * 0.35, W, H * 0.65], fill=(200, 20, 30))
            for i in range(0, 30):
                xx = W * (0.02 + i * 0.033)
                dd.rectangle([xx, H * 0.15, xx + W * 0.012, H * 0.85], fill=(255, 60, 60))
        self.draw(self.img, r, fn)
        self.glow[y0 + int(h * 0.35):y0 + int(h * 0.65), x0:x0 + w] = (255, 30, 40, 255)
        self.lights[y0:y0 + h, x0:x0 + w] = (255, 40, 50, 255)

    def diffuser_fins(self, r):
        def fn(dd, W, H, K):
            for i in range(7):
                xx = W * (0.1 + i * 0.13)
                dd.rectangle([xx, 0, xx + W * 0.02, H], fill=(70, 70, 78))
            dd.rectangle([W * 0.47, H * 0.1, W * 0.53, H * 0.6], fill=(255, 40, 40))    # F1-style rain light
        self.draw(self.img, r, fn)
        x0, y0, w, h = r
        self.glow[y0 + int(h * 0.1):y0 + int(h * 0.6), x0 + int(w * 0.47):x0 + int(w * 0.53)] = (255, 30, 30, 255)

    def exhaust_tip(self, r):
        def fn(dd, W, H, K):
            dd.ellipse([W * 0.08, H * 0.05, W * 0.92, H * 0.95], fill=(200, 205, 215), outline=(90, 90, 100), width=K)
            dd.ellipse([W * 0.22, H * 0.2, W * 0.78, H * 0.8], fill=(8, 8, 8))
        self.draw(self.img, r, fn)

    def mirror_glass(self, r):
        self.grad(self.img, r, (150, 175, 205), (80, 100, 130))

    def dash(self, r, face):
        V = self.V
        if face != 'back':
            self.leather(r, quilted=False)
            return
        x0, y0, w, h = r
        self.black(r, 20)
        # back (SOUTH) face: left = maxX (driver side), right = minX (passenger side)
        def fn(dd, W, H, K):
            dd.rectangle([0, 0, W, H * 0.22], fill=mix(V['leather'], (0, 0, 0), 0.2))
            dd.line([0, H * 0.22, W, H * 0.22], fill=V['stitch'], width=K)
            # driver binnacle
            for i, cx in enumerate((0.16, 0.30)):
                dd.ellipse([W * cx - H * 0.3, H * 0.3, W * cx + H * 0.3, H * 0.9], fill=(8, 8, 10), outline=(160, 160, 170), width=K)
            dd.rounded_rectangle([W * 0.40, H * 0.35, W * 0.67, H * 0.85], radius=3 * K, fill=(8, 10, 14))   # infotainment
            for i in range(4):
                xx = W * (0.72 + i * 0.06)
                dd.rounded_rectangle([xx, H * 0.45, xx + W * 0.04, H * 0.75], radius=K, fill=(60, 60, 66))    # vents
        self.draw(self.img, r, fn)
        def gfn(dd, W, H, K):
            for cx in (0.16, 0.30):
                c = (W * cx, H * 0.6)
                rr = H * 0.26
                dd.arc([c[0] - rr, c[1] - rr, c[0] + rr, c[1] + rr], 135, 405, fill=(60, 220, 255, 255), width=max(1, K))
                for k in range(9):
                    a = math.radians(135 + k * 270 / 8)
                    dd.line([c[0] + math.cos(a) * rr * 0.75, c[1] + math.sin(a) * rr * 0.75, c[0] + math.cos(a) * rr, c[1] + math.sin(a) * rr],
                            fill=(255, 255, 255, 255), width=max(1, K // 2))
                a = math.radians(200)
                dd.line([c[0], c[1], c[0] + math.cos(a) * rr * 0.9, c[1] + math.sin(a) * rr * 0.9], fill=(255, 60, 40, 255), width=max(1, K))
            dd.rounded_rectangle([W * 0.41, H * 0.38, W * 0.66, H * 0.82], radius=2 * K, fill=(20, 60, 120, 255))
            dd.rectangle([W * 0.43, H * 0.42, W * 0.64, H * 0.52], fill=(60, 200, 255, 255))
            dd.rectangle([W * 0.43, H * 0.58, W * 0.55, H * 0.78], fill=(50, 230, 120, 255))
            dd.rectangle([W * 0.57, H * 0.58, W * 0.64, H * 0.78], fill=(255, 190, 40, 255))
        self.draw(self.glow, r, gfn)

    def console(self, r, face):
        self.carbon(r)
        if face == 'top':
            def fn(dd, W, H, K):
                dd.rounded_rectangle([W * 0.2, H * 0.55, W * 0.8, H * 0.75], radius=2 * K, fill=(200, 30, 30))   # start button / launch
                dd.ellipse([W * 0.25, H * 0.2, W * 0.75, H * 0.35], fill=(190, 190, 200))
            self.draw(self.img, r, fn)

    def steering(self, r, face):
        x0, y0, w, h = r
        self.rect(self.img, r, (0, 0, 0), 0)
        if face not in ('back', 'front'):
            self.black(r, 15)
            return
        V = self.V
        def fn(dd, W, H, K):
            dd.ellipse([W * 0.02, H * 0.02, W * 0.98, H * 0.98], fill=(22, 22, 24, 255))
            dd.ellipse([W * 0.18, H * 0.18, W * 0.82, H * 0.82], fill=(0, 0, 0, 0))
            dd.rectangle([W * 0.1, H * 0.44, W * 0.9, H * 0.58], fill=(30, 30, 34, 255))
            dd.rectangle([W * 0.44, H * 0.5, W * 0.56, H * 0.85], fill=(30, 30, 34, 255))
            dd.ellipse([W * 0.36, H * 0.36, W * 0.64, H * 0.64], fill=(40, 40, 46, 255))
            dd.polygon([(W * 0.5, H * 0.4), (W * 0.56, H * 0.5), (W * 0.5, H * 0.6), (W * 0.44, H * 0.5)], fill=V['badge'] + (255,))
            dd.rectangle([W * 0.47, H * 0.02, W * 0.53, H * 0.12], fill=(255, 200, 0, 255))   # 12 o'clock stripe
            dd.ellipse([W * 0.12, H * 0.5, W * 0.2, H * 0.58], fill=(220, 30, 30, 255))      # drive-mode dial
        self.draw(self.img, r, fn)

    def wheel(self, r, face, rim):
        V = self.V
        x0, y0, w, h = r
        side = face in ('minx', 'maxx')
        self.rect(self.img, r, (0, 0, 0), 0)
        if side:
            if not rim: return
            def fn(dd, W, H, K):
                cx, cy, R0 = W / 2, H / 2, W / 2
                dd.ellipse([0, 0, W - 1, H - 1], fill=(26, 26, 28, 255))                       # tyre wall
                dd.ellipse([W * 0.06, H * 0.06, W * 0.94, H * 0.94], outline=(48, 48, 52, 255), width=K)
                rr = R0 * 0.72
                dd.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=V['lip'] + (255,))
                ri = rr * 0.9
                dd.ellipse([cx - ri, cy - ri, cx + ri, cy + ri], fill=(18, 18, 20, 255))           # barrel shadow
                # brake disc + caliper behind the spokes
                rd = rr * 0.78
                dd.ellipse([cx - rd, cy - rd, cx + rd, cy + rd], fill=(120, 120, 128, 255))
                for k in range(24):
                    a = k * math.pi / 12
                    dd.line([cx + math.cos(a) * rd * 0.55, cy + math.sin(a) * rd * 0.55, cx + math.cos(a) * rd * 0.95, cy + math.sin(a) * rd * 0.95], fill=(90, 90, 98, 255), width=K)
                dd.pieslice([cx - rd * 1.02, cy - rd * 1.02, cx + rd * 1.02, cy + rd * 1.02], 200, 250, fill=V['caliper'] + (255,))
                n = V['spokes']
                for k in range(n):
                    a = k * 2 * math.pi / n
                    wdt = (0.22 if n <= 5 else 0.12 if n <= 10 else 0.07) * rr
                    pts = []
                    for sgn in (-1, 1):
                        pts.append((cx + math.cos(a + sgn * 0.12) * rr * 0.22, cy + math.sin(a + sgn * 0.12) * rr * 0.22))
                    pts = [pts[0], (cx + math.cos(a) * ri + math.cos(a + math.pi / 2) * wdt, cy + math.sin(a) * ri + math.sin(a + math.pi / 2) * wdt),
                           (cx + math.cos(a) * ri - math.cos(a + math.pi / 2) * wdt, cy + math.sin(a) * ri - math.sin(a + math.pi / 2) * wdt), pts[1]]
                    dd.polygon(pts, fill=V['rim'] + (255,))
                    if n == 5:   # Y-spoke split
                        b = a + math.pi / n
                        dd.line([cx + math.cos(b) * rr * 0.35, cy + math.sin(b) * rr * 0.35, cx + math.cos(b) * ri, cy + math.sin(b) * ri], fill=V['rim'] + (255,), width=2 * K)
                hub = rr * 0.24
                dd.ellipse([cx - hub, cy - hub, cx + hub, cy + hub], fill=V['rim'] + (255,), outline=(60, 60, 60, 255), width=K)
                dd.ellipse([cx - hub * 0.45, cy - hub * 0.45, cx + hub * 0.45, cy + hub * 0.45], fill=V['badge'] + (255,))
            self.draw(self.img, r, fn)
            # round silhouette: hard alpha cut outside the tyre circle
            yy, xx = np.mgrid[0:h, 0:w]
            out = ((xx + 0.5 - w / 2) ** 2 + (yy + 0.5 - h / 2) ** 2) > (w / 2) ** 2
            self.img[y0:y0 + h, x0:x0 + w][out] = 0
        else:
            # tread faces: only the central 1/16-gon facet (2.59 px of 13) is solid, with a tread pattern
            if face in ('top', 'bottom', 'front', 'back'):
                long_axis_vertical = True
                L = h
                band = max(2, int(round(L / 2 * math.tan(math.pi / 16))) + 1)  # half-width of the facet in texels
                c0 = L // 2
                sub = self.img[y0:y0 + h, x0:x0 + w]
                if long_axis_vertical:
                    sub[c0 - band:c0 + band, :, :3] = 24; sub[c0 - band:c0 + band, :, 3] = 255
                    for xx in range(1, w - 1, 3): sub[c0 - band:c0 + band, xx, :3] = 40
                else:
                    sub[:, c0 - band:c0 + band, :3] = 24; sub[:, c0 - band:c0 + band, 3] = 255
                    for yy in range(1, h - 1, 3): sub[yy, c0 - band:c0 + band, :3] = 40

    def save(self, name):
        def out(arr, path):
            Image.fromarray(arr.clip(0, 255).astype(np.uint8), 'RGBA').save(path)
        out(self.img, f'{ROOT}/{name}.png')
        return self

import os
os.makedirs(ROOT, exist_ok=True)
all_cubes = {}
for p in parts:
    for c in p.cubes:
        all_cubes.setdefault(c.key, c)
for name, V in VARIANTS.items():
    P = Painter(V)
    for c in all_cubes.values():
        P.cube(c)
    P.save(name)
    if name == 'veloce':   # emissive + glass layers are shared by all models
        Image.fromarray(P.glow.clip(0, 255).astype(np.uint8), 'RGBA').save(f'{ROOT}/glow.png')
        Image.fromarray(P.lights.clip(0, 255).astype(np.uint8), 'RGBA').save(f'{ROOT}/lights.png')
        Image.fromarray(P.glass.clip(0, 255).astype(np.uint8), 'RGBA').save(f'{ROOT}/glass.png')
    print('painted', name)
