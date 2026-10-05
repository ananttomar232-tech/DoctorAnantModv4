# v2: HD textures. Generates every PNG for the mod.
import colorsys, os, random, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
SRC='/home/claude/assets_src'; RND='/home/claude/render/out'
R='src/main/resources/assets/dranant/textures'
for d in ['item','block','entity','models/armor']: os.makedirs(f'{R}/{d}',exist_ok=True)
FB='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def save(im,p): im.save(f'{R}/{p}.png')
def crisp(im,size):
    a=np.array(im.resize((size,size),Image.LANCZOS)).astype(float)
    a[...,3]=np.where(a[...,3]>100,255,0); return Image.fromarray(a.astype(np.uint8))
def noise(im,amt=8,seed=3):
    a=np.array(im.convert('RGBA')).astype(int); n=np.random.RandomState(seed).randint(-amt,amt+1,a.shape[:2])
    for c in range(3): a[...,c]=np.clip(a[...,c]+n,0,255)
    return Image.fromarray(a.astype(np.uint8))
def vgrad(w,h,top,bot):
    g=Image.new('RGBA',(w,h)); d=ImageDraw.Draw(g)
    for y in range(h):
        t=y/max(1,h-1); d.line([(0,y),(w,y)],fill=tuple(int(top[i]*(1-t)+bot[i]*t) for i in range(3))+(255,))
    return g
def paste_masked(base,fill,mask): base.paste(fill,(0,0),mask)

# ---------------- 1. item icons from the attached glTF renders (128 px HD) ----------------
def icon(path,size=128,hue_fn=None):
    im=Image.open(path).convert('RGBA'); im=im.crop(im.getbbox()); w,h=im.size; s=max(w,h)
    sq=Image.new('RGBA',(s,s)); sq.alpha_composite(im,((s-w)//2,(s-h)//2)); im=sq
    if hue_fn:
        a=np.array(im).astype(float)
        rgb=a[...,:3]/255.0
        for y in range(a.shape[0]):
            for x in range(a.shape[1]):
                if a[y,x,3]<10: continue
                h_,s_,v_=colorsys.rgb_to_hsv(*rgb[y,x]); h2,s2,v2=hue_fn(h_,s_,v_); a[y,x,:3]=np.array(colorsys.hsv_to_rgb(h2,s2,v2))*255
        im=Image.fromarray(a.astype(np.uint8))
    return crisp(im,size)
def to_crimson(h,s,v): return (0.985,min(1,s*1.6+0.35),min(1,v*1.25)) if 0.15<h<0.55 else (h,s,v)
def to_green(h,s,v): return (0.36,s,min(1,v*1.3)) if ((h<0.06 or h>0.9) and s>0.3) else (h,s,v)
save(icon(f'{RND}/potion.png',hue_fn=to_crimson),'item/expired_experimental_serum')
save(icon(f'{RND}/cuffs.png'),'item/titanium_handcuffs')
save(icon(f'{RND}/syringe.png',hue_fn=to_green),'item/universal_cure_syringe')
save(icon(f'{RND}/coat.png'),'item/dr_anant_coat')
save(icon(f'{RND}/bed.png'),'item/examination_bed')
save(icon(f'{RND}/pedestal.png'),'item/item_display_pedestal')

# ---------------- 2. HD medicines (drawn at 512, downsampled to 64) ----------------
S=512
def shade(c,f): return tuple(max(0,min(255,int(v*f))) for v in c[:3])+(255,)
OUT=(28,20,30,255)
def glass_body(d,box,ellipse=True):
    (d.ellipse if ellipse else d.rounded_rectangle)(box,fill=(205,232,245,255),outline=OUT,width=14,**({} if ellipse else {'radius':40}))
def liquid(base,box,col,ellipse=True):
    m=Image.new('L',(S,S)); dm=ImageDraw.Draw(m)
    (dm.ellipse if ellipse else dm.rounded_rectangle)(box,fill=255,**({} if ellipse else {'radius':30}))
    g=vgrad(S,S,shade(col,1.25),shade(col,0.65)); base.paste(g,(0,0),m)
def highlight(d,box): d.ellipse(box,fill=(255,255,255,200))
def label(d,box):
    d.rounded_rectangle(box,radius=14,fill=(250,250,245,255),outline=(160,160,160,255),width=6)
    cx=(box[0]+box[2])//2; cy=(box[1]+box[3])//2; s=(box[3]-box[1])//4
    d.rectangle([cx-s//2,cy-s*3//2,cx+s//2,cy+s*3//2],fill=(220,30,40,255)); d.rectangle([cx-s*3//2,cy-s//2,cx+s*3//2,cy+s//2],fill=(220,30,40,255))
def bottle(liq,cap):
    im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
    d.rounded_rectangle([200,20,312,110],radius=18,fill=shade(cap,1),outline=OUT,width=12)
    d.rectangle([215,105,297,170],fill=(210,235,248,255),outline=OUT,width=12)
    glass_body(d,[70,150,442,500]); liquid(im,[95,250,417,480],liq); d=ImageDraw.Draw(im)
    label(d,[170,300,342,420]); highlight(d,[130,210,180,300]); return im
def dropper(liq,cap):
    im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
    d.ellipse([196,10,316,130],fill=shade(cap,1),outline=OUT,width=12); d.rectangle([226,120,286,190],fill=(235,235,235,255),outline=OUT,width=12)
    glass_body(d,[120,180,392,500],ellipse=False); liquid(im,[140,280,372,480],liq,ellipse=False); d=ImageDraw.Draw(im)
    label(d,[170,310,342,420]); d.rounded_rectangle([150,210,175,330],radius=10,fill=(255,255,255,200)); return im
def capsule(c1,c2):
    im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
    big=Image.new('RGBA',(S,S)); bd=ImageDraw.Draw(big)
    bd.rounded_rectangle([60,186,452,326],radius=70,fill=shade(c2,1),outline=OUT,width=14)
    bd.rounded_rectangle([60,186,256,326],radius=70,fill=shade(c1,1),outline=OUT,width=14); bd.rectangle([200,200,256,312],fill=shade(c1,1))
    bd.line([(256,192),(256,320)],fill=OUT,width=10); bd.rounded_rectangle([100,210,240,240],radius=14,fill=(255,255,255,170))
    return big.rotate(40,resample=Image.BICUBIC)
def vial(liq,cap):
    im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
    d.rounded_rectangle([170,10,342,110],radius=14,fill=shade(cap,1),outline=OUT,width=12)
    d.rounded_rectangle([170,100,342,500],radius=40,fill=(212,236,248,255),outline=OUT,width=14)
    liquid(im,[188,190,324,484],liq,ellipse=False); d=ImageDraw.Draw(im)
    for y in range(240,460,60): d.line([(300,y),(330,y)],fill=OUT,width=8)
    d.rounded_rectangle([200,140,228,440],radius=12,fill=(255,255,255,190)); return im
def flask(liq,cap):
    im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
    d.rounded_rectangle([206,10,306,80],radius=12,fill=shade(cap,1),outline=OUT,width=12); d.rectangle([216,75,296,190],fill=(215,238,250,255),outline=OUT,width=12)
    d.polygon([(216,185),(296,185),(470,490),(42,490)],fill=(215,238,250,255),outline=OUT,width=14)
    m=Image.new('L',(S,S)); ImageDraw.Draw(m).polygon([(165,300),(347,300),(445,475),(67,475)],fill=255); im.paste(vgrad(S,S,shade(liq,1.3),shade(liq,0.6)),(0,0),m)
    d=ImageDraw.Draw(im); d.polygon([(216,185),(296,185),(470,490),(42,490)],outline=OUT,width=14)
    for (x,y,r) in [(200,380,18),(270,420,12),(310,350,10)]: d.ellipse([x-r,y-r,x+r,y+r],fill=(255,255,255,170))
    d.line([(190,230),(110,420)],fill=(255,255,255,190),width=18); return im
def elixir():
    im=flask((255,205,40),(255,240,120)); d=ImageDraw.Draw(im)
    for (x,y) in [(60,90),(440,120),(420,300),(80,300)]:
        d.polygon([(x,y-34),(x+10,y-10),(x+34,y),(x+10,y+10),(x,y+34),(x-10,y+10),(x-34,y),(x-10,y-10)],fill=(255,255,190,255))
    d.rectangle([216,110,296,140],fill=(205,150,20,255)); return im
med={1:('fever_syrup',bottle,(230,60,60),(150,90,40)),2:('speed_vitamin_drops',dropper,(70,190,255),(235,70,70)),
3:('iron_bone_capsule',capsule,(200,40,40),(235,235,240)),4:('xray_vision_tonic',flask,(70,250,110),(90,90,90)),
5:('vitality_heart_injection',vial,(255,210,40),(220,40,60)),6:('regeneration_serum',vial,(255,105,185),(200,200,210)),
7:('titan_strength_serum',vial,(170,20,20),(60,60,60)),8:('fire_shield_serum',bottle,(255,130,20),(120,60,20)),
9:('omega_super_serum',flask,(140,60,255),(255,215,0))}
for t,(n,fn,a,b) in med.items(): save(crisp(fn(a,b),64),f'item/{n}')
save(crisp(elixir(),64),'item/immortality_elixir')

# ---------------- 3. HD block textures (64 px) ----------------
TIER_NAMES={1:'DISPENSARY',2:'COMMUNITY',3:'HEALTH CTR',4:'HOSPITAL',5:'MEDICAL',6:'MEGA'}
def summoner(t):
    hue=(t-1)/6.0; col=tuple(int(v*255) for v in colorsys.hsv_to_rgb(hue,0.6,0.92))
    im=vgrad(64,64,(250,252,255),(222,230,238)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,63,63],outline=col+(255,),width=4); d.rectangle([4,4,59,59],outline=shade(col,0.7),width=1)
    d.rectangle([8,10,16,30],fill=(220,30,40,255)); d.rectangle([2,16,22,24],fill=(220,30,40,255))
    f=ImageFont.truetype(FB,30); d.text((44,22),str(t),font=f,fill=(30,30,70,255),anchor='mm',stroke_width=1,stroke_fill=(255,255,255))
    f2=ImageFont.truetype(FB,8); d.text((32,48),'LEVEL '+str(t),font=f2,fill=shade(col,0.6),anchor='mm')
    d.text((32,56),TIER_NAMES[t],font=ImageFont.truetype(FB,6),fill=(80,80,90,255),anchor='mm')
    return noise(im,3)
for t in range(1,7): save(summoner(t),f'block/clinic_summoner_{t}')
im=vgrad(64,64,(255,255,255),(225,240,230)); d=ImageDraw.Draw(im); d.rectangle([0,0,63,63],outline=(40,170,90,255),width=4)
d.rectangle([6,6,57,22],fill=(40,170,90,255)); d.text((32,14),'SHOP',font=ImageFont.truetype(FB,12),fill=(255,255,255),anchor='mm')
d.rectangle([10,28,54,58],fill=(120,200,255,255),outline=(60,120,170,255),width=2); d.line([(32,28),(32,58)],fill=(250,250,250),width=3); save(noise(im,3),'block/central_shop_spawner_side')
im=Image.new('RGBA',(64,64),(40,170,90,255)); d=ImageDraw.Draw(im); d.rectangle([22,10,42,54],fill=(255,255,255,255)); d.rectangle([10,22,54,42],fill=(255,255,255,255)); save(noise(im,4),'block/central_shop_spawner_top')
st=Image.open(f'{SRC}/structural_i_from_fps_creator_classic/textures/structural_i_baseColor.png').convert('RGBA')
save(st.crop((16,40,240,240)).resize((64,64),Image.LANCZOS),'block/pedestal_panel')
save(st.crop((0,300,340,470)).resize((64,64),Image.LANCZOS),'block/pedestal_ornament')
save(st.crop((340,300,512,370)).resize((64,64),Image.LANCZOS),'block/pedestal_slab')
def lin(c): return tuple(int(255*((v*12.92) if v<=0.0031308 else (1.055*v**(1/2.4)-0.055))) for v in c)
SHEET=lin((0.185873,0.97583,0.793332))
im=vgrad(64,64,shade(SHEET,1.05),shade(SHEET,0.88)); d=ImageDraw.Draw(im)
for x in range(0,64,8): d.line([(x,0),(x,63)],fill=shade(SHEET,0.93),width=1)
save(noise(im,3),'block/bed_sheet')
save(noise(vgrad(64,64,(245,245,248),(215,215,222)),3),'block/bed_frame')
im=vgrad(64,64,(170,175,182),(95,100,108)); d=ImageDraw.Draw(im); d.line([(0,20),(63,20)],fill=(220,225,230),width=2); save(noise(im,4),'block/bed_metal')
save(noise(Image.new('RGBA',(64,64),(25,25,28,255)),4),'block/bed_wheel')
im=vgrad(64,64,shade(SHEET,1.0),shade(SHEET,0.9)); d=ImageDraw.Draw(im); d.rounded_rectangle([6,10,57,53],radius=12,fill=(252,252,252,255),outline=(220,220,232,255),width=2); d.line([(10,32),(53,32)],fill=(232,232,240),width=2); save(noise(im,2),'block/bed_pillow')
# stretcher canvas
im=vgrad(64,64,(236,236,228),(205,205,196)); d=ImageDraw.Draw(im)
for y in range(0,64,6): d.line([(0,y),(63,y)],fill=(220,220,210),width=1)
d.rectangle([24,20,40,44],fill=(220,30,40,255)); d.rectangle([20,26,44,38],fill=(220,30,40,255)); d.rectangle([24,20,40,44],outline=(250,250,250)); save(noise(im,3),'block/stretcher_canvas')
# register
im=vgrad(64,64,(130,245,245),(60,200,205)); d=ImageDraw.Draw(im); d.rectangle([0,0,63,63],outline=(235,190,40,255),width=4)
for x,y in [(14,16),(40,40),(50,12),(20,46)]: d.polygon([(x,y-5),(x+5,y),(x,y+5),(x-5,y)],fill=(230,255,255,255))
save(noise(im,4),'block/register_side')
im=Image.new('RGBA',(64,64),(50,52,62,255)); d=ImageDraw.Draw(im)
for yy in range(3):
    for xx in range(4): d.rounded_rectangle([6+xx*13,8+yy*13,15+xx*13,17+yy*13],radius=2,fill=(236,236,236,255),outline=(150,150,150,255))
d.rectangle([6,50,57,56],fill=(235,190,40,255)); save(im,'block/register_keypad')
im=Image.new('RGBA',(64,64),(18,26,36,255)); d=ImageDraw.Draw(im); d.rectangle([4,4,59,59],fill=(16,110,58,255))
d.polygon([(32,10),(48,26),(32,52),(16,26)],fill=(120,255,255,255),outline=(20,160,190,255)); d.line([(16,26),(48,26)],fill=(40,200,220),width=2)
d.text((32,57),'PAID',font=ImageFont.truetype(FB,7),fill=(200,255,200),anchor='mm'); save(im,'block/register_screen')
save(noise(vgrad(64,64,(255,215,70),(200,150,20)),5),'block/register_gold')
save(noise(vgrad(64,64,(140,105,72),(100,72,48)),5),'block/poster_back')

# ---------------- 4. HQ posters with the player's skin ----------------
skin=Image.open(f'{SRC}/skin/player_skin_render.png').convert('RGBA')
def fit_font(text,maxw,size):
    while ImageFont.truetype(FB,size).getlength(text)>maxw and size>8: size-=1
    return ImageFont.truetype(FB,size)
def poster(w,h,lines,path,extra=None):
    im=vgrad(w,h,(255,252,240),(255,236,205)); d=ImageDraw.Draw(im)
    # sunburst
    cx,cy=int(w*0.25),int(h*0.6)
    for i in range(0,360,12):
        import math
        a1=math.radians(i); a2=math.radians(i+6); R_=max(w,h)*1.2
        d.polygon([(cx,cy),(cx+R_*math.cos(a1),cy+R_*math.sin(a1)),(cx+R_*math.cos(a2),cy+R_*math.sin(a2))],fill=(255,226,170,255))
    bw=max(6,w//80)
    d.rectangle([0,0,w-1,h-1],outline=(200,25,35),width=bw*2); d.rectangle([bw*3,bw*3,w-1-bw*3,h-1-bw*3],outline=(30,90,170),width=bw)
    hb=int(h*0.17); d.rectangle([bw*3,bw*3,w-1-bw*3,bw*3+hb],fill=(30,90,170))
    title="DR. ANANT'S MEGA CLINIC"; ft=fit_font(title,w*0.86,int(hb*0.62))
    d.text((w//2,bw*3+hb//2),title,font=ft,fill=(255,255,255),anchor='mm',stroke_width=max(1,hb//25),stroke_fill=(10,40,90))
    # player skin portrait
    sh=int(h*0.72); sw=int(skin.width*sh/skin.height); sk=skin.resize((sw,sh),Image.NEAREST)
    shadow=Image.new('RGBA',sk.size,(0,0,0,0)); shadow.paste((0,0,0,110),(0,0),sk.split()[3])
    px=int(w*0.05); py=h-sh-bw*4
    im.alpha_composite(shadow,(px+bw,py+bw)); im.alpha_composite(sk,(px,py))
    d=ImageDraw.Draw(im)
    # red cross badge
    cs=int(h*0.13); bx=w-int(cs*1.3)-bw*5; by=bw*5+hb+int(cs*0.25)
    d.ellipse([bx-cs*0.2,by-cs*0.2,bx+cs*1.2,by+cs*1.2],fill=(255,255,255),outline=(200,25,35),width=bw)
    d.rectangle([bx+cs*0.35,by,bx+cs*0.65,by+cs],fill=(220,30,40)); d.rectangle([bx,by+cs*0.35,bx+cs,by+cs*0.65],fill=(220,30,40))
    # slogan
    tx0=px+sw+int(w*0.03); tx1=(bx-int(cs*0.3)) if w>h else w-bw*6; tw=tx1-tx0
    y=bw*6+hb+int(h*0.04)+ (int(cs*1.4) if w<=h else 0)
    fs=int(h*0.11) if w>h else int(h*0.075)
    for ln in lines:
        f=fit_font(ln,tw,fs)
        d.text(((tx0+tx1)//2,y),ln,font=f,fill=(190,15,30),anchor='mt',stroke_width=max(2,fs//14),stroke_fill=(255,255,255))
        y+=int(f.size*1.25)
    if extra:
        f=fit_font(extra,tw,int(fs*0.7)); d.rounded_rectangle([(tx0+tx1)//2-f.getlength(extra)/2-12,y+6,(tx0+tx1)//2+f.getlength(extra)/2+12,y+12+f.size*1.3],radius=12,fill=(30,140,70))
        d.text(((tx0+tx1)//2,y+10),extra,font=f,fill=(255,255,255),anchor='mt')
    save(im.convert('RGBA'),path)
poster(1024,512,["Anant Doctor karege","aapke sabhi dukho","ka ilaaj!"],'block/poster_landscape',"OPEN 24x7")
poster(512,512,["Anant Doctor","karege aapke","sabhi dukho","ka ilaaj!"],'block/poster_square')
poster(1024,1024,["Anant Doctor","karege aapke","sabhi dukho","ka ilaaj!"],'block/poster_billboard',"Sabse sasti dawai!")

# ---------------- 5. HD Mutant Blood Beast (256 px, from the attached texture) ----------------
z=Image.open(f'{SRC}/minecraft_mutant_zombie_download_free/textures/material_0_baseColor.png').convert('RGBA')
a=np.array(z).astype(float)
for y in range(128):
    for x in range(128):
        r,g,b,al=a[y,x]
        if al==0: continue
        h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
        if 0.40<h<0.75: h=0.99; s=min(1,s*1.1+0.2); v=v*0.62
        elif 0.15<h<=0.40: h=0.04; s=min(1,s*0.6); v=min(1,v*1.0)
        rr,gg,bb=colorsys.hsv_to_rgb(h,s,v); a[y,x,:3]=(rr*255,gg*255,bb*255)
base=Image.fromarray(a.astype(np.uint8)).resize((256,256),Image.NEAREST)
b=np.array(base).astype(int); rs=np.random.RandomState(5)
detail=rs.randint(-14,15,(256,256))
for c in range(3): b[...,c]=np.where(b[...,3]>0,np.clip(b[...,c]+detail,0,255),b[...,c])
beast=Image.fromarray(b.astype(np.uint8)); d=ImageDraw.Draw(beast)
rr=random.Random(11)
for _ in range(220):  # fine blood drips
    x=rr.randint(0,200); y=rr.randint(32,150)
    if beast.getpixel((x,y))[3]>0: d.line([(x,y),(x,y+rr.randint(2,7))],fill=(rr.randint(90,140),0,0,255),width=1)
for _ in range(60):  # veins
    x=rr.randint(0,200); y=rr.randint(32,150)
    if beast.getpixel((x,y))[3]>0: d.line([(x,y),(x+rr.randint(-4,4),y+rr.randint(-4,4))],fill=(60,0,10,255))
EYES=[(18,24),(19,24),(20,24),(21,24),(26,24),(27,24),(28,24),(29,24),(18,25),(19,25),(20,25),(21,25),(26,25),(27,25),(28,25),(29,25)]
for p in EYES: d.point(p,fill=(255,40,30,255))
d.rectangle([0,160,55,179],fill=(185,190,200,255)); d.rectangle([0,160,55,161],fill=(235,238,242,255))
for x in range(0,56,5): d.line([(x,166),(x,174)],fill=(120,125,135,255))
d.rectangle([0,192,23,207],fill=(150,155,165,255)); save(beast,'entity/mutant_blood_beast')
eyes=Image.new('RGBA',(256,256)); de=ImageDraw.Draw(eyes)
for p in EYES: de.point(p,fill=(255,50,30,255))
save(eyes,'entity/mutant_blood_beast_eyes')

# ---------------- 6. HD humanoid skins (128 px, player-model layout at 2x) ----------------
def skin_hd(coat,accent,hair,skin_col=(160,110,80),extra=None,pants=(40,50,90)):
    s=2; im=Image.new('RGBA',(64*s,64*s)); d=ImageDraw.Draw(im)
    SK=skin_col+(255,); dk=shade(skin_col,0.75)
    def R(x0,y0,x1,y1,c): d.rectangle([x0*s,y0*s,(x1+1)*s-1,(y1+1)*s-1],fill=c)
    def box(u,v,w,h,dd,top,side,front=None,bottom=None):
        R(u+dd,v,u+dd+w-1,v+dd-1,top); R(u+dd+w,v,u+dd+2*w-1,v+dd-1,bottom or top); R(u,v+dd,u+2*(w+dd)-1,v+dd+h-1,side)
        if front: R(u+dd,v+dd,u+dd+w-1,v+dd+h-1,front)
    box(0,0,8,8,8,hair+(255,),SK,SK,SK)
    R(0,8,31,9,hair+(255,)); R(24,8,31,14,hair+(255,)); R(8,8,15,9,hair+(255,)); R(0,8,1,12,hair+(255,)); R(22,8,23,12,hair+(255,))
    # eyes (HD: 2x2 with highlight), brows, nose, mouth
    for ex in (9,13):
        R(ex,12,ex+1,12,(250,250,250,255)); d.rectangle([ (ex+1)*s,12*s,(ex+1)*s+1,12*s+1],fill=(40,25,20,255)); d.point(((ex+1)*s,12*s),fill=(255,255,255,255))
    R(9,11,10,11,shade(hair,0.8)); R(13,11,14,11,shade(hair,0.8)); d.rectangle([11*s+1,13*s,12*s+2,13*s+2],fill=dk)
    R(10,14,13,14,(120,60,55,255))
    box(16,16,8,12,4,coat+(255,),coat+(255,),coat+(255,),coat+(255,))
    R(20,20,27,21,(70,120,200,255)); d.line([(24*s-1,20*s),(24*s-1,32*s-1)],fill=accent+(255,),width=2)
    for yy in (23,26,29): d.ellipse([24*s+1,yy*s,24*s+3,yy*s+2],fill=(170,170,180,255))
    R(21,26,22,27,shade(coat,0.88)); R(25,26,26,27,shade(coat,0.88)); d.rectangle([25*s,21*s,25*s+1,23*s],fill=(30,90,210,255))
    d.arc([20*s,19*s,27*s,26*s],0,180,fill=(30,30,30,255),width=2); d.ellipse([23*s,25*s,24*s+1,26*s+1],fill=(190,190,200,255))
    box(40,16,4,12,4,coat+(255,),coat+(255,),coat+(255,),SK); box(32,48,4,12,4,coat+(255,),coat+(255,),coat+(255,),SK)
    R(40,30,55,31,SK); R(32,62,47,63,SK)
    box(0,16,4,12,4,pants+(255,),pants+(255,)); box(16,48,4,12,4,pants+(255,),pants+(255,))
    R(0,30,15,31,(30,30,30,255)); R(16,62,31,63,(30,30,30,255))
    # coat fold shading
    for x in range(16*s,40*s,3*s): d.line([(x,22*s),(x,32*s-1)],fill=shade(coat,0.94),width=1)
    if extra: extra(d,s,R)
    return im
def goggles(d,s,R):
    R(8,11,15,11,(40,40,40,255)); R(9,11,10,12,(120,220,255,255)); R(13,11,14,12,(120,220,255,255)); R(40,11,55,11,(40,40,40,255))
def seller_extra(d,s,R):
    R(10,14,13,14,(30,20,15,255)); R(9,13,14,13,(30,20,15,255))  # mustache
    R(0,0,31,1,(200,40,40,255)); R(8,0,15,7,(200,40,40,255)); R(8,7,15,8,(200,40,40,255))  # topi
    R(20,20,27,31,(250,170,60,255)); d.line([(24*s,20*s),(24*s,32*s)],fill=(200,120,30,255),width=2)
save(skin_hd((248,248,250),(220,40,50),(25,20,20)),'entity/assistant_pharmacist')
save(skin_hd((210,245,215),(40,160,80),(70,45,30),extra=goggles),'entity/assistant_scientist')
save(skin_hd((250,160,50),(200,100,20),(30,25,25),skin_col=(170,120,85),extra=seller_extra,pants=(245,245,240)),'entity/shopkeeper')

# ---------------- 7. HD coat armor layer (128x64) ----------------
canvas=Image.open(f'{SRC}/male_lab_coat_and_pants/textures/Cotton_Heavy_Canvas_FRONT_127117_baseColor.png').convert('L').resize((128,64))
arm=Image.new('RGBA',(128,64)); d=ImageDraw.Draw(arm); s=2
W=(246,246,248,255); E=(212,212,220,255)
def RR(x0,y0,x1,y1,c): d.rectangle([x0*s,y0*s,(x1+1)*s-1,(y1+1)*s-1],fill=c)
RR(16,16,39,31,W); RR(20,16,35,19,W); RR(40,16,55,31,W); RR(44,16,51,19,W)
d.line([(24*s-1,20*s),(24*s-1,32*s)],fill=E,width=2)
d.polygon([(20*s,20*s),(23*s,20*s),(22*s,25*s)],fill=E); d.polygon([(28*s,20*s),(24*s,20*s),(25*s,25*s)],fill=E)
for y in (23,26,29): d.ellipse([24*s,y*s,24*s+2,y*s+2],fill=(160,160,170,255))
d.rectangle([20*s,27*s,22*s,29*s],outline=E); d.rectangle([26*s,27*s,28*s,29*s],outline=E); d.rectangle([25*s,21*s,25*s+1,23*s+1],fill=(30,90,200,255))
d.line([(40*s,29*s),(56*s,29*s)],fill=E,width=2)
a=np.array(arm).astype(int); cn=np.array(canvas).astype(int); m=a[...,3]>0; a[...,0:3][m]-=((255-cn[m])//40)[:,None]
save(Image.fromarray(np.clip(a,0,255).astype(np.uint8)),'models/armor/dr_anant_layer_1')
save(Image.new('RGBA',(128,64)),'models/armor/dr_anant_layer_2')
print('v2 textures done')
