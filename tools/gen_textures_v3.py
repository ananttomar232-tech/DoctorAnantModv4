# v3 textures: city founder block, bike entity + item, sidekick blaster, whistle
import numpy as np
from PIL import Image, ImageDraw, ImageFont
R='src/main/resources/assets/dranant/textures'
FB='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def crisp(im,size):
    a=np.array(im.resize((size,size),Image.LANCZOS)).astype(float); a[...,3]=np.where(a[...,3]>100,255,0); return Image.fromarray(a.astype(np.uint8))
# --- city founder block: golden frame with a skyline
im=Image.new('RGBA',(64,64),(255,215,80,255)); d=ImageDraw.Draw(im)
d.rectangle([0,0,63,63],outline=(160,110,10),width=4)
d.rectangle([6,6,57,57],fill=(40,80,160))
for x,h,c in [(8,30,(230,230,240)),(16,40,(200,220,255)),(25,22,(240,200,200)),(32,46,(220,240,220)),(42,34,(250,250,250)),(50,26,(230,210,250))]:
    d.rectangle([x,57-h,x+6,57],fill=c)
    for wy in range(57-h+3,56,5): d.rectangle([x+2,wy,x+3,wy+1],fill=(255,240,120))
d.rectangle([6,52,57,57],fill=(80,170,70)); d.text((32,13),'CITY',font=ImageFont.truetype(FB,12),fill=(255,230,90),anchor='mm',stroke_width=1,stroke_fill=(60,40,0))
im.save(f'{R}/block/city_founder.png')
# --- bike entity texture (128x128): regions must match BikeModel texOffs
tex=Image.new('RGBA',(128,128),(0,0,0,0)); d=ImageDraw.Draw(tex)
def box_uv(u,v,w,h,dd,col,front=None,top=None):
    d.rectangle([u,v,u+2*(w+dd)-1,v+dd+h-1],fill=col)
    if top: d.rectangle([u+dd,v,u+dd+w-1,v+dd-1],fill=top)
    if front: d.rectangle([u+dd,v+dd,u+dd+w-1,v+dd+h-1],fill=front)
TIRE=(30,30,34,255); RIM=(170,175,185,255); RED=(200,25,35,255); DRED=(140,15,20,255); CHROME=(205,210,220,255); BLACK=(25,25,28,255)
box_uv(0,0,2,12,8,TIRE); d.rectangle([2,10,15,17],fill=RIM)          # wheel A
box_uv(24,0,2,8,12,TIRE); d.rectangle([28,14,47,17],fill=RIM)        # wheel B
box_uv(0,24,4,3,18,DRED,top=RED)                                     # frame
box_uv(48,24,6,5,8,CHROME); d.line([(48,34),(75,34)],fill=(120,120,130,255))   # engine
box_uv(0,48,6,4,7,RED,top=(235,60,60,255)); d.rectangle([7,48,9,58],fill=(255,255,255,255))  # tank stripe
box_uv(32,48,6,2,9,BLACK,top=(55,40,30,255))                          # seat
box_uv(64,48,2,11,2,CHROME)                                           # fork
box_uv(56,0,12,1,1,BLACK)                                             # handlebar
box_uv(80,48,3,3,2,(255,240,150,255),front=(255,255,200,255))        # headlight
box_uv(0,64,4,1,6,RED)                                                # fender
box_uv(24,64,1,1,9,CHROME)                                            # exhaust
d.text((14,52),'',fill=(255,255,255,255))
tex.save(f'{R}/entity/dr_anant_bike.png')
# --- bike item icon (side view)
S=512; im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
for cx in (120,392):
    d.ellipse([cx-95,300,cx+95,490],fill=(30,30,34,255)); d.ellipse([cx-50,345,cx+50,445],fill=(180,185,195,255)); d.ellipse([cx-15,380,cx+15,410],fill=(60,60,70,255))
d.polygon([(120,395),(230,300),(380,300),(392,395),(300,395)],fill=(200,25,35,255),outline=(90,10,15,255),width=10)
d.rounded_rectangle([200,230,330,300],radius=30,fill=(220,40,50,255),outline=(90,10,15,255),width=8)
d.rounded_rectangle([300,250,450,285],radius=14,fill=(25,25,28,255))
d.line([(150,180),(120,395)],fill=(205,210,220,255),width=22); d.line([(110,175),(200,175)],fill=(25,25,28,255),width=18)
d.ellipse([60,190,110,240],fill=(255,240,150,255),outline=(120,100,20,255),width=6)
d.rectangle([240,330,330,380],fill=(205,210,220,255)); d.line([(320,380),(470,380)],fill=(205,210,220,255),width=16)
im=crisp(im,64); im.save(f'{R}/item/dr_anant_bike.png')
# --- sidekick blaster (pistol)
im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
d.rounded_rectangle([60,150,450,250],radius=20,fill=(60,65,75,255),outline=(20,20,25,255),width=12)
d.rectangle([400,170,480,230],fill=(40,45,55,255),outline=(20,20,25,255),width=10)
d.polygon([(150,240),(260,240),(220,440),(110,440)],fill=(90,60,40,255),outline=(30,20,15,255),width=12)
d.arc([230,230,320,330],0,180,fill=(20,20,25,255),width=14)
d.rectangle([90,165,380,185],fill=(0,200,200,255)); d.ellipse([440,185,470,215],fill=(255,120,0,255))
crisp(im.rotate(-20,resample=Image.BICUBIC),64).save(f'{R}/item/sidekick_blaster.png')
# --- whistle
im=Image.new('RGBA',(S,S)); d=ImageDraw.Draw(im)
d.ellipse([170,200,400,430],fill=(255,200,40,255),outline=(120,80,0,255),width=14)
d.rounded_rectangle([60,240,260,320],radius=20,fill=(255,210,60,255),outline=(120,80,0,255),width=14)
d.ellipse([250,270,320,340],fill=(40,30,10,255)); d.line([(400,250),(470,120)],fill=(200,30,30,255),width=18)
d.ellipse([440,80,500,140],outline=(200,30,30,255),width=14)
crisp(im,64).save(f'{R}/item/sidekick_whistle.png')
print('v3 textures done')
