# Generates every PNG texture for the mod from the attached 3D-model ZIPs + procedural pixel art.
import colorsys, os, random, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
SRC='/home/claude/assets_src'; RND='/home/claude/render/out'
R='src/main/resources/assets/dranant/textures'
for d in ['item','block','entity','models/armor','gui']: os.makedirs(f'{R}/{d}',exist_ok=True)
random.seed(7)
def save(im,p): im.save(f'{R}/{p}.png')

# ---------- 1. item icons rendered from the attached glTF models ----------
def icon(path, size=64, hue_fn=None):
    im=Image.open(path).convert('RGBA'); im=im.crop(im.getbbox())
    w,h=im.size; s=max(w,h); sq=Image.new('RGBA',(s,s)); sq.alpha_composite(im,((s-w)//2,(s-h)//2))
    a=np.array(sq.resize((size,size),Image.LANCZOS)).astype(float)
    if hue_fn:
        for y in range(size):
            for x in range(size):
                r,g,b,al=a[y,x]
                if al<10: continue
                hh,ss,vv=colorsys.rgb_to_hsv(r/255,g/255,b/255)
                nh,ns,nv=hue_fn(hh,ss,vv)
                rr,gg,bb=colorsys.hsv_to_rgb(nh,ns,nv); a[y,x,:3]=(rr*255,gg*255,bb*255)
    a[...,3]=np.where(a[...,3]>110,255,0)       # crisp alpha so item/generated extrudes cleanly
    return Image.fromarray(a.astype(np.uint8))
def to_crimson(h,s,v):
    if 0.15<h<0.55: return (0.985, min(1,s*1.6+0.35), min(1,v*1.25))
    return (h,s,v)
def to_green(h,s,v):
    if (h<0.06 or h>0.9) and s>0.3: return (0.36, s, min(1,v*1.3))
    return (h,s,v)
save(icon(f'{RND}/potion.png',hue_fn=to_crimson),'item/expired_experimental_serum')
save(icon(f'{RND}/cuffs.png'),'item/titanium_handcuffs')
save(icon(f'{RND}/syringe.png',hue_fn=to_green),'item/universal_cure_syringe')
save(icon(f'{RND}/coat.png'),'item/dr_anant_coat')

# ---------- 2. medicine tiers 1-10 (16x16 pixel art) ----------
def shade(c,f): return tuple(max(0,min(255,int(v*f))) for v in c[:3])+(255,)
OUT=(30,22,30,255)
def bottle(liq,cap):
    im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
    d.rectangle([6,1,9,3],fill=shade(cap,1),outline=OUT)            # cork
    d.rectangle([6,4,9,5],fill=(200,230,240,255),outline=OUT)       # neck
    d.ellipse([2,5,13,15],fill=(210,235,245,255),outline=OUT)       # body glass
    d.ellipse([3,8,12,14],fill=shade(liq,1)); d.point([(5,9),(5,10),(6,9)],fill=(255,255,255,255))
    return im
def dropper(liq,cap):
    im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
    d.ellipse([6,0,9,4],fill=shade(cap,1),outline=OUT); d.rectangle([7,4,8,6],fill=(230,230,230,255),outline=OUT)
    d.rectangle([4,6,11,15],fill=(210,235,245,255),outline=OUT); d.rectangle([5,9,10,14],fill=shade(liq,1))
    d.line([(6,10),(6,13)],fill=(255,255,255,255)); return im
def capsule(c1,c2):
    im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
    for i in range(10):
        x,y=3+i,12-i; col=shade(c1,1) if i<5 else shade(c2,1)
        d.ellipse([x-2,y-2,x+2,y+2],fill=col)
    d.line([(3,10),(10,3)],fill=(255,255,255,200)); return im
def vial(liq,cap):
    im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
    d.rectangle([5,0,10,3],fill=shade(cap,1),outline=OUT); d.rectangle([5,3,10,15],fill=(215,235,245,255),outline=OUT)
    d.rectangle([6,6,9,14],fill=shade(liq,1)); d.line([(7,7),(7,12)],fill=(255,255,255,255))
    d.line([(6,5),(9,5)],fill=OUT); return im
def flask(liq,cap):
    im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
    d.rectangle([6,0,9,2],fill=shade(cap,1),outline=OUT); d.rectangle([6,3,9,6],fill=(220,240,250,255),outline=OUT)
    d.polygon([(6,6),(9,6),(14,15),(1,15)],fill=(220,240,250,255),outline=OUT)
    d.polygon([(5,10),(10,10),(13,14),(2,14)],fill=shade(liq,1)); d.point([(4,12),(5,11)],fill=(255,255,255,255)); return im
def elixir():
    im=flask((255,210,40),(255,240,120)); d=ImageDraw.Draw(im)
    for p in [(1,2),(13,3),(12,8),(2,8)]: d.point(p,fill=(255,255,180,255))
    d.line([(6,3),(9,3)],fill=(200,150,20,255)); return im
med={1:('fever_syrup',bottle,(230,60,60),(150,90,40)),2:('speed_vitamin_drops',dropper,(80,200,255),(240,70,70)),
3:('iron_bone_capsule',capsule,(200,40,40),(235,235,240)),4:('xray_vision_tonic',flask,(80,255,120),(90,90,90)),
5:('vitality_heart_injection',vial,(255,215,40),(220,40,60)),6:('regeneration_serum',vial,(255,110,190),(200,200,200)),
7:('titan_strength_serum',vial,(160,20,20),(60,60,60)),8:('fire_shield_serum',bottle,(255,130,20),(120,60,20)),
9:('omega_super_serum',flask,(140,60,255),(255,215,0))}
for t,(n,fn,a,b) in med.items(): save(fn(a,b),f'item/{n}')
save(elixir(),'item/immortality_elixir')

# ---------- 3. block textures ----------
DIG={'0':["111","101","101","101","111"],'1':["010","110","010","010","111"],'2':["111","001","111","100","111"],'3':["111","001","111","001","111"],'4':["101","101","111","001","001"],'5':["111","100","111","001","111"],'6':["111","100","111","101","111"],'7':["111","001","010","010","010"],'8':["111","101","111","101","111"],'9':["111","101","111","001","111"]}
def noise(im,amt=10):
    a=np.array(im).astype(int); n=np.random.RandomState(3).randint(-amt,amt+1,a.shape[:2])
    for c in range(3): a[...,c]=np.clip(a[...,c]+n,0,255)
    return Image.fromarray(a.astype(np.uint8))
def summoner(tier):
    hue=(tier-1)/10.0; r,g,b=[int(v*255) for v in colorsys.hsv_to_rgb(hue,0.55,0.95)]
    im=Image.new('RGBA',(16,16),(235,240,245,255)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,15,15],outline=(r,g,b,255)); d.rectangle([1,1,14,14],outline=shade((r,g,b),0.7))
    d.rectangle([3,6,7,8],fill=(220,30,40,255)); d.rectangle([4,5,6,9],fill=(220,30,40,255))  # red cross
    s=str(tier); x=9 if len(s)==1 else 8
    for ch in s:
        for yy,row in enumerate(DIG[ch]):
            for xx,v in enumerate(row):
                if v=='1': d.point((x+xx,5+yy),fill=(30,30,60,255))
        x+=4
    return noise(im,6)
for t in range(1,11): save(summoner(t),f'block/clinic_summoner_{t}')
im=Image.new('RGBA',(16,16),(250,250,250,255)); d=ImageDraw.Draw(im)
d.rectangle([0,0,15,15],outline=(40,170,90,255)); d.rectangle([2,2,13,6],fill=(40,170,90,255))
d.rectangle([3,8,12,14],fill=(120,200,255,255)); d.rectangle([7,8,8,14],fill=(250,250,250,255)); save(noise(im,5),'block/central_shop_spawner_side')
im=Image.new('RGBA',(16,16),(40,170,90,255)); d=ImageDraw.Draw(im); d.rectangle([5,3,10,12],fill=(255,255,255,255)); d.rectangle([3,5,12,10],fill=(255,255,255,255)); save(noise(im,5),'block/central_shop_spawner_top')
# pedestal: crops of the attached stone "structural_i" texture
st=Image.open(f'{SRC}/structural_i_from_fps_creator_classic/textures/structural_i_baseColor.png').convert('RGBA')
save(st.crop((16,40,240,240)).resize((32,32),Image.LANCZOS),'block/pedestal_panel')
save(st.crop((0,300,340,470)).resize((32,32),Image.LANCZOS),'block/pedestal_ornament')
save(st.crop((340,300,512,370)).resize((32,32),Image.LANCZOS),'block/pedestal_slab')
# examination bed: material colours from hospital_bed.zip (linear -> sRGB)
def lin(c): return tuple(int(255*((v*12.92) if v<=0.0031308 else (1.055*v**(1/2.4)-0.055))) for v in c)
for name,c in [('bed_sheet',(0.185873,0.97583,0.793332)),('bed_frame',(0.8,0.8,0.8)),('bed_metal',(0.456404,0.456404,0.456404)),('bed_wheel',(0.02,0.02,0.02))]:
    save(noise(Image.new('RGBA',(16,16),lin(c)+(255,)),5),f'block/{name}')
im=Image.new('RGBA',(16,16),lin((0.185873,0.97583,0.793332))+(255,)); d=ImageDraw.Draw(im); d.rectangle([2,3,13,12],fill=(250,250,250,255),outline=(220,220,230,255)); save(noise(im,4),'block/bed_pillow')
# cash register
im=Image.new('RGBA',(16,16),(90,230,230,255)); d=ImageDraw.Draw(im); d.rectangle([0,0,15,15],outline=(230,190,40,255))
for x,y in [(3,4),(9,9),(12,3),(5,11)]: d.point([(x,y),(x+1,y),(x,y+1)],fill=(220,255,255,255))
save(noise(im,8),'block/register_side')
im=Image.new('RGBA',(16,16),(60,60,70,255)); d=ImageDraw.Draw(im)
for yy in range(3):
    for xx in range(4): d.rectangle([2+xx*3,3+yy*3,3+xx*3,4+yy*3],fill=(230,230,230,255))
d.rectangle([2,12,13,13],fill=(230,190,40,255)); save(im,'block/register_keypad')
im=Image.new('RGBA',(16,16),(20,30,40,255)); d=ImageDraw.Draw(im); d.rectangle([1,1,14,14],fill=(20,120,60,255))
d.polygon([(8,3),(12,7),(8,12),(4,7)],fill=(120,255,255,255)); d.line([(4,7),(12,7)],fill=(40,200,220,255)); save(im,'block/register_screen')
save(noise(Image.new('RGBA',(16,16),(230,190,40,255)),8),'block/register_gold')

# ---------- 4. posters with the slogan ----------
SLOGAN="Anant Doctor karege aapke sabhi dukho ka ilaaj"
FB='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def doctor_figure(d,x,y,s):
    d.ellipse([x+3*s,y,x+7*s,y+4*s],fill=(150,100,70)); d.rectangle([x+3*s,y,x+7*s,y+1*s],fill=(30,25,25))
    d.rectangle([x+2*s,y+4*s,x+8*s,y+11*s],fill=(250,250,250),outline=(180,180,190))
    d.line([(x+5*s,y+4*s),(x+5*s,y+11*s)],fill=(180,180,190),width=max(1,s//2))
    d.arc([x+3*s,y+4*s,x+7*s,y+8*s],0,180,fill=(40,40,40),width=max(1,s//2))
    d.rectangle([x+3*s,y+11*s,x+4*s,y+15*s],fill=(40,60,120)); d.rectangle([x+6*s,y+11*s,x+7*s,y+15*s],fill=(40,60,120))
def cross(d,x,y,s,col=(220,30,40)):
    d.rectangle([x+s,y,x+2*s,y+3*s],fill=col); d.rectangle([x,y+s,x+3*s,y+2*s],fill=col)
def poster(w,h,lines,fs,path):
    im=Image.new('RGBA',(w,h),(250,248,240,255)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,w-1,h-1],outline=(200,30,40),width=max(2,w//64)); d.rectangle([w//40,h//40,w-1-w//40,h-1-h//40],outline=(30,90,170),width=max(1,w//128))
    d.rectangle([w//40,h//40,w-1-w//40,h//5],fill=(30,90,170))
    f=ImageFont.truetype(FB,fs); tsz=int(fs*1.1)
    while ImageFont.truetype(FB,tsz).getlength("DR. ANANT'S MEGA CLINIC")>w*0.9: tsz-=1
    ft=ImageFont.truetype(FB,tsz)
    d.text((w//2,h//10+2),"DR. ANANT'S MEGA CLINIC",font=ft,fill=(255,255,255),anchor='mm')
    s=max(2,h//40); doctor_figure(d,w//24,h//4,s); cross(d,w-w//8,h//4,max(3,h//25))
    y=int(h*0.42)
    for ln in lines:
        d.text((w//2+w//14,y),ln,font=f,fill=(180,20,30),anchor='mm'); y+=int(fs*1.35)
    save(im,path)
poster(256,128,["Anant Doctor karege","aapke sabhi dukho","ka ilaaj"],15,'block/poster_landscape')
poster(128,128,["Anant Doctor","karege aapke","sabhi dukho","ka ilaaj"],10,'block/poster_square')
poster(256,256,["Anant Doctor karege","aapke sabhi dukho","ka ilaaj","","Open 24x7"],18,'block/poster_billboard')
save(noise(Image.new('RGBA',(16,16),(120,90,60,255)),6),'block/poster_back')

# ---------- 5. Mutant Blood Beast (attached mutant zombie texture, blood-tinted) ----------
z=Image.open(f'{SRC}/minecraft_mutant_zombie_download_free/textures/material_0_baseColor.png').convert('RGBA')
a=np.array(z).astype(float)
for y in range(128):
    for x in range(128):
        r,g,b,al=a[y,x]
        if al==0: continue
        h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
        if 0.40<h<0.75: h=0.99; s=min(1,s*1.1+0.2); v=v*0.62        # shirt/trousers -> dark crimson
        elif 0.15<h<=0.40: h=0.06; s=min(1,s*0.55); v=min(1,v*1.05)   # zombie green skin -> raw flesh
        rr,gg,bb=colorsys.hsv_to_rgb(h,s,v); a[y,x,:3]=(rr*255,gg*255,bb*255)
beast=Image.fromarray(a.astype(np.uint8)); d=ImageDraw.Draw(beast)
EYES=[(9,12),(10,12),(13,12),(14,12)]
for p in EYES: d.point(p,fill=(255,30,30,255))
# blood drips on the body
rs=random.Random(11)
for _ in range(60):
    x=rs.randint(0,80); y=rs.randint(16,72)
    if beast.getpixel((x,y))[3]>0: d.line([(x,y),(x,y+rs.randint(1,3))],fill=(110,0,0,255))
# handcuff metal region (used by the cuff/chain cubes at texOffs 0,80 / 0,96)
d.rectangle([0,80,27,89],fill=(185,190,200,255)); d.rectangle([0,80,27,80],fill=(225,230,235,255))
for x in range(0,28,3): d.point((x,85),fill=(110,115,125,255))
d.rectangle([0,96,11,103],fill=(150,155,165,255))
save(beast,'entity/mutant_blood_beast')
eyes=Image.new('RGBA',(128,128)); de=ImageDraw.Draw(eyes)
for p in EYES: de.point(p,fill=(255,40,20,255))
save(eyes,'entity/mutant_blood_beast_eyes')

# ---------- 6. Assistant skins (64x64 player layout, procedural) ----------
def skin(coat,accent,hair,goggles=False):
    im=Image.new('RGBA',(64,64)); d=ImageDraw.Draw(im)
    SK=(160,110,80,255); dk=(120,80,55,255)
    def box(u,v,w,h,dd,top,side,front=None,bottom=None):
        d.rectangle([u+dd,v,u+dd+w-1,v+dd-1],fill=top); d.rectangle([u+dd+w,v,u+dd+2*w-1,v+dd-1],fill=bottom or top)
        d.rectangle([u,v+dd,u+2*(w+dd)-1,v+dd+h-1],fill=side)
        if front: d.rectangle([u+dd,v+dd,u+dd+w-1,v+dd+h-1],fill=front)
    box(0,0,8,8,8,hair,SK,SK,SK)                       # head
    d.rectangle([0,8,31,9],fill=hair); d.rectangle([24,8,31,13],fill=hair)  # hair sides/back
    d.rectangle([8,8,15,9],fill=hair)
    d.point([(10,12),(13,12)],fill=(30,20,20,255)); d.point([(9,12),(14,12)],fill=(250,250,250,255))
    d.line([(10,14),(13,14)],fill=dk); d.line([(10,15),(13,15)],fill=(40,30,30,255) if not goggles else dk)
    if goggles:
        d.rectangle([40,11,55,12],fill=(40,40,40,255)); d.rectangle([41,11,43,12],fill=(120,220,255,255)); d.rectangle([44+0,11,46,12],fill=(40,40,40,255))
        d.rectangle([49,11,51,12],fill=(120,220,255,255)); d.rectangle([52,11,54,12],fill=(120,220,255,255))
    box(16,16,8,12,4,coat,coat,coat,coat)             # body
    d.rectangle([20,20,27,21],fill=(70,120,200,255))  # shirt collar
    d.line([(23,20),(23,31)],fill=accent); d.line([(24,22),(24,31)],fill=shade(coat,0.85))
    d.rectangle([21,26,22,27],fill=shade(coat,0.85)); d.rectangle([25,26,26,27],fill=shade(coat,0.85))
    d.arc([20,19,27,26],0,180,fill=(30,30,30,255))    # stethoscope
    box(40,16,4,12,4,coat,coat,coat,SK); box(32,48,4,12,4,coat,coat,coat,SK)   # arms
    d.rectangle([40,30,55,31],fill=SK); d.rectangle([32,62,47,63],fill=SK)
    box(0,16,4,12,4,(40,50,90,255),(40,50,90,255)); box(16,48,4,12,4,(40,50,90,255),(40,50,90,255))
    d.rectangle([0,30,15,31],fill=(30,30,30,255)); d.rectangle([16,62,31,63],fill=(30,30,30,255))
    return im
save(skin((248,248,250,255),(220,40,50,255),(25,20,20,255)),'entity/assistant_pharmacist')
save(skin((210,245,215,255),(40,160,80,255),(70,45,30,255),goggles=True),'entity/assistant_scientist')

# ---------- 7. Dr. Anant coat armor layer (64x32 humanoid armor layout) ----------
canvas=Image.open(f'{SRC}/male_lab_coat_and_pants/textures/Cotton_Heavy_Canvas_FRONT_127117_baseColor.png').convert('L').resize((64,32))
arm=Image.new('RGBA',(64,32)); d=ImageDraw.Draw(arm)
W=(246,246,248,255); E=(215,215,222,255)
d.rectangle([16,16,39,31],fill=W); d.rectangle([20,16,35,19],fill=W)   # body box
d.rectangle([40,16,55,31],fill=W); d.rectangle([44,16,51,19],fill=W)   # arm box
d.line([(23,20),(23,31)],fill=E)                                       # front opening
d.polygon([(20,20),(23,20),(22,24)],fill=E); d.polygon([(27,20),(24,20),(25,24)],fill=E)  # lapels
for y in (23,26,29): d.point((24,y),fill=(160,160,170,255))            # buttons
d.rectangle([20,27,21,28],outline=E); d.rectangle([26,27,27,28],outline=E); d.rectangle([25,21,26,22],fill=(30,90,200,255))  # pockets+pen
d.line([(40,29),(55,29)],fill=E)                                       # cuffs
a=np.array(arm).astype(int); cn=np.array(canvas).astype(int)
mask=a[...,3]>0; a[...,0:3][mask]-=((255-cn[mask])//40)[:,None]
save(Image.fromarray(np.clip(a,0,255).astype(np.uint8)),'models/armor/dr_anant_layer_1')
save(Image.new('RGBA',(64,32)),'models/armor/dr_anant_layer_2')

# ---------- 8. HUD diamond icon ----------
im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im); d.polygon([(8,2),(14,7),(8,14),(2,7)],fill=(80,235,235,255),outline=(20,120,140,255)); d.line([(2,7),(14,7)],fill=(190,255,255,255)); save(im,'gui/diamond_icon')
print('textures done')
