# Generates every JSON resource (blockstates, models, loot tables, lang, sounds, tags).
import json, os, shutil
A='src/main/resources/assets/dranant'; D='src/main/resources/data'
def w(path,obj):
    os.makedirs(os.path.dirname(path),exist_ok=True)
    with open(path,'w') as f: json.dump(obj,f,indent=2)
M='dranant:'
ROT={'north':0,'east':90,'south':180,'west':270}

# ---------- simple cube blocks ----------
for t in range(1,7):
    n=f'clinic_summoner_{t}'
    w(f'{A}/blockstates/{n}.json',{"variants":{"":{"model":M+'block/'+n}}})
    w(f'{A}/models/block/{n}.json',{"parent":"minecraft:block/cube_all","textures":{"all":M+'block/'+n}})
    w(f'{A}/models/item/{n}.json',{"parent":M+'block/'+n})
w(f'{A}/blockstates/central_shop_spawner.json',{"variants":{"":{"model":M+'block/central_shop_spawner'}}})
w(f'{A}/models/block/central_shop_spawner.json',{"parent":"minecraft:block/cube_bottom_top","textures":{"side":M+'block/central_shop_spawner_side',"top":M+'block/central_shop_spawner_top',"bottom":M+'block/central_shop_spawner_top'}})
w(f'{A}/models/item/central_shop_spawner.json',{"parent":M+'block/central_shop_spawner'})
w(f'{A}/blockstates/city_founder.json',{"variants":{"":{"model":M+'block/city_founder'}}})
w(f'{A}/models/block/city_founder.json',{"parent":"minecraft:block/cube_all","textures":{"all":M+'block/city_founder'}})
w(f'{A}/models/item/city_founder.json',{"parent":M+'block/city_founder'})

def faces(tex, skip=()):
    return {d:{"texture":tex} for d in ['north','south','east','west','up','down'] if d not in skip}
def el(fr,to,tex,**over):
    f=faces(tex)
    for k,v in over.items(): f[k]={"texture":v}
    return {"from":fr,"to":to,"faces":f}

# ---------- item display pedestal (shape + texture from structural_i_from_fps_creator_classic.zip) ----------
w(f'{A}/models/block/item_display_pedestal.json',{"textures":{"particle":M+'block/pedestal_panel',"panel":M+'block/pedestal_panel',"orn":M+'block/pedestal_ornament',"slab":M+'block/pedestal_slab'},
 "elements":[el([0,0,0],[16,3,16],"#slab"), el([3,3,3],[13,13,13],"#panel",up="#slab",down="#slab"), el([1,13,1],[15,16,15],"#orn",up="#slab",down="#slab")]})
w(f'{A}/blockstates/item_display_pedestal.json',{"variants":{"":{"model":M+'block/item_display_pedestal'}}})
w(f'{A}/models/item/item_display_pedestal.json',{"parent":"minecraft:item/generated","textures":{"layer0":M+'item/item_display_pedestal'}})

# ---------- examination bed (hospital_bed.zip colours), authored for facing=north (head toward -z) ----------
bedtex={"particle":M+'block/bed_frame',"sheet":M+'block/bed_sheet',"frame":M+'block/bed_frame',"metal":M+'block/bed_metal',"wheel":M+'block/bed_wheel',"pillow":M+'block/bed_pillow'}
def legs(z0,z1):
    return [el([1.5,1,z0],[2.5,3,z1],"#metal"),el([13.5,1,z0],[14.5,3,z1],"#metal"),el([1,0,z0-0.5],[3,1.5,z1+0.5],"#wheel"),el([13,0,z0-0.5],[15,1.5,z1+0.5],"#wheel")]
head=[el([1,3,0],[15,5,16],"#metal"),el([1,5,0],[15,8,16],"#sheet"),el([3,8,2],[13,10,7],"#pillow",up="#pillow"),
      el([0,3,0],[16,15,2],"#frame"),el([0,8,6],[1,10,15],"#metal"),el([15,8,6],[16,10,15],"#metal"),
      el([0,5,7],[1,8,8],"#metal"),el([15,5,7],[16,8,8],"#metal")]+legs(1.5,2.5)
foot=[el([1,3,0],[15,5,16],"#metal"),el([1,5,0],[15,8,14],"#sheet"),el([0,3,14],[16,11,16],"#frame"),
      el([0,8,1],[1,10,10],"#metal"),el([15,8,1],[16,10,10],"#metal")]+legs(13.5,14.5)
w(f'{A}/models/block/examination_bed_head.json',{"textures":bedtex,"elements":head})
w(f'{A}/models/block/examination_bed_foot.json',{"textures":bedtex,"elements":foot})
w(f'{A}/blockstates/examination_bed.json',{"variants":{f"facing={f},part={p}":{"model":M+f'block/examination_bed_{p}',"y":r} for f,r in ROT.items() for p in ['head','foot']}})
w(f'{A}/models/item/examination_bed.json',{"parent":"minecraft:item/generated","textures":{"layer0":M+'item/examination_bed'}})

# ---------- diamond cash register (facing = customer side, authored facing north) ----------
w(f'{A}/models/block/diamond_cash_register.json',{"textures":{"particle":M+'block/register_side',"side":M+'block/register_side',"keys":M+'block/register_keypad',"screen":M+'block/register_screen',"gold":M+'block/register_gold'},
 "elements":[el([1,0,2],[15,5,14],"#side",south="#gold"),
             el([2,5,3],[14,8,11],"#side",up="#keys"),
             el([7,8,9],[9,11,11],"#gold"),
             el([3,10,8],[13,16,10],"#side",north="#screen",south="#screen")]})
w(f'{A}/blockstates/diamond_cash_register.json',{"variants":{f"facing={f}":{"model":M+'block/diamond_cash_register',"y":r} for f,r in ROT.items()}})
w(f'{A}/models/item/diamond_cash_register.json',{"parent":M+'block/diamond_cash_register'})

# ---------- posters (authored facing north: plate at the south edge, extends toward +x / +y) ----------
for n,(wd,ht) in {'poster_landscape':(32,16),'poster_square':(16,16),'poster_billboard':(32,32)}.items():
    w(f'{A}/models/block/{n}.json',{"textures":{"particle":M+'block/'+n,"front":M+'block/'+n,"back":M+'block/poster_back'},
      "elements":[{"from":[0,0,15],"to":[wd,ht,16],"faces":{"north":{"uv":[0,0,16,16],"texture":"#front"},"south":{"uv":[0,0,16,16],"texture":"#back"},
       "east":{"uv":[0,0,1,16],"texture":"#back"},"west":{"uv":[0,0,1,16],"texture":"#back"},"up":{"uv":[0,0,16,1],"texture":"#back"},"down":{"uv":[0,0,16,1],"texture":"#back"}}}],
      "display":{"gui":{"rotation":[0,180,0],"translation":[0,0,0],"scale":[0.5 if wd>16 else 0.9]*3},"fixed":{"rotation":[0,180,0],"scale":[0.5]*3},
                 "thirdperson_righthand":{"rotation":[75,180,0],"translation":[0,2.5,0],"scale":[0.3]*3},"firstperson_righthand":{"rotation":[0,135,0],"scale":[0.3]*3},"ground":{"scale":[0.3]*3}}})
    w(f'{A}/blockstates/{n}.json',{"variants":{f"facing={f}":{"model":M+'block/'+n,"y":r} for f,r in ROT.items()}})
    w(f'{A}/models/item/{n}.json',{"parent":"minecraft:item/generated","textures":{"layer0":M+'block/'+n}})

# ---------- items ----------
gen=['expired_experimental_serum','titanium_handcuffs','universal_cure_syringe','dr_anant_coat','fever_syrup','speed_vitamin_drops','iron_bone_capsule',
     'xray_vision_tonic','vitality_heart_injection','regeneration_serum','titan_strength_serum','fire_shield_serum','omega_super_serum','immortality_elixir',
     'dr_anant_bike','sidekick_whistle','sidekick_blaster',
     'plasma_rifle','surgeon_katana','defibrillator_hammer','supercar_veloce','supercar_gt','supercar_phantom']
HANDHELD=('titanium_handcuffs','universal_cure_syringe','sidekick_blaster','plasma_rifle','surgeon_katana','defibrillator_hammer')
for n in gen:
    parent="minecraft:item/handheld" if n in HANDHELD else "minecraft:item/generated"
    w(f'{A}/models/item/{n}.json',{"parent":parent,"textures":{"layer0":M+'item/'+n}})
# stretcher (authored facing north = patient's head toward -z)
st={"particle":M+'block/stretcher_canvas',"canvas":M+'block/stretcher_canvas',"metal":M+'block/bed_metal',"pillow":M+'block/bed_pillow'}
stel=[el([2,5,0],[14,6,16],"#canvas"),el([1,4.5,-1],[2,6,17],"#metal"),el([14,4.5,-1],[15,6,17],"#metal"),
      el([3,6,1],[13,7.5,4],"#pillow"),
      el([1.5,0,1],[2.5,4.5,2],"#metal"),el([13.5,0,1],[14.5,4.5,2],"#metal"),el([1.5,0,14],[2.5,4.5,15],"#metal"),el([13.5,0,14],[14.5,4.5,15],"#metal")]
w(f'{A}/models/block/stretcher.json',{"textures":st,"elements":stel})
w(f'{A}/blockstates/stretcher.json',{"variants":{f"facing={f}":{"model":M+'block/stretcher',"y":r} for f,r in ROT.items()}})
w(f'{A}/models/item/stretcher.json',{"parent":M+'block/stretcher'})
for n in ['mutant_blood_beast_spawn_egg','assistant_pharmacist_spawn_egg','assistant_scientist_spawn_egg','shopkeeper_spawn_egg','patient_villager_spawn_egg','sidekick_spawn_egg']:
    w(f'{A}/models/item/{n}.json',{"parent":"minecraft:item/template_spawn_egg"})

# ---------- sounds.json (custom events layered on vanilla events) ----------
def ev(*names,sub): return {"subtitle":sub,"sounds":[{"name":n,"type":"event"} for n in names]}
w(f'{A}/sounds.json',{
 "handcuff_rattle":ev("minecraft:block.chain.place","minecraft:item.armor.equip_chain",sub="subtitles.dranant.handcuff_rattle"),
 "syringe_injection":ev("minecraft:item.bottle.empty",sub="subtitles.dranant.syringe_injection"),
 "ground_slam":ev("minecraft:entity.generic.explode",sub="subtitles.dranant.ground_slam"),
 "cash_register_chime":ev("minecraft:block.note_block.chime",sub="subtitles.dranant.cash_register_chime"),
 "diamond_pop":ev("minecraft:block.amethyst_block.chime",sub="subtitles.dranant.diamond_pop"),
 "bike_engine":ev("minecraft:entity.minecart.inside",sub="subtitles.dranant.bike_engine"),
 "bike_horn":ev("minecraft:block.note_block.didgeridoo",sub="subtitles.dranant.bike_horn"),
 "sidekick_shot":ev("minecraft:entity.firework_rocket.blast",sub="subtitles.dranant.sidekick_shot"),
 "sidekick_whistle":ev("minecraft:block.note_block.flute",sub="subtitles.dranant.sidekick_whistle"),
 "plasma_shot":ev("minecraft:entity.firework_rocket.blast","minecraft:block.beacon.power_select",sub="subtitles.dranant.plasma_shot"),
 "katana_dash":ev("minecraft:entity.player.attack.sweep","minecraft:item.trident.riptide_1",sub="subtitles.dranant.katana_dash"),
 "defib_shock":ev("minecraft:block.respawn_anchor.deplete","minecraft:entity.guardian.attack",sub="subtitles.dranant.defib_shock"),
 "car_engine":ev("minecraft:entity.minecart.riding",sub="subtitles.dranant.car_engine"),
 "car_horn":ev("minecraft:block.note_block.bass","minecraft:block.note_block.didgeridoo",sub="subtitles.dranant.car_horn"),
 "car_door":ev("minecraft:block.iron_trapdoor.close",sub="subtitles.dranant.car_door"),
 "car_crash":ev("minecraft:block.anvil.land","minecraft:entity.iron_golem.damage",sub="subtitles.dranant.car_crash"),
 "car_nitro":ev("minecraft:entity.firework_rocket.launch","minecraft:item.firecharge.use",sub="subtitles.dranant.car_nitro")})

# ---------- lang ----------
L={"itemGroup.dranant":"Dr. Anant's Mega Clinic",
 "item.dranant.fever_syrup":"Fever Syrup (Tier 1)","item.dranant.speed_vitamin_drops":"Speed Vitamin Drops (Tier 2)",
 "item.dranant.iron_bone_capsule":"Iron Bone Capsule (Tier 3)","item.dranant.xray_vision_tonic":"X-Ray Vision Tonic (Tier 4)",
 "item.dranant.vitality_heart_injection":"Vitality Heart Injection (Tier 5)","item.dranant.regeneration_serum":"Regeneration Serum (Tier 6)",
 "item.dranant.titan_strength_serum":"Titan Strength Serum (Tier 7)","item.dranant.fire_shield_serum":"Fire Shield Serum (Tier 8)",
 "item.dranant.omega_super_serum":"Omega Super Serum (Tier 9)","item.dranant.immortality_elixir":"Immortality Elixir (Tier 10)",
 "item.dranant.expired_experimental_serum":"Expired Experimental Serum","item.dranant.titanium_handcuffs":"Titanium Handcuffs",
 "item.dranant.universal_cure_syringe":"Universal Cure Syringe","item.dranant.dr_anant_coat":"Dr. Anant's Lab Coat",
 "item.dranant.mutant_blood_beast_spawn_egg":"Mutant Blood Beast Spawn Egg","item.dranant.assistant_pharmacist_spawn_egg":"Assistant Pharmacist Spawn Egg",
 "item.dranant.assistant_scientist_spawn_egg":"Assistant Scientist Spawn Egg",
 "block.dranant.central_shop_spawner":"Central Shop Spawner","block.dranant.item_display_pedestal":"Item Display Pedestal",
 "block.dranant.examination_bed":"Examination Bed","block.dranant.diamond_cash_register":"Diamond Cash Register",
 "block.dranant.poster_landscape":"Dr. Anant Poster (Landscape 2x1)","block.dranant.poster_square":"Dr. Anant Poster (Square 1x1)",
 "block.dranant.poster_billboard":"Dr. Anant Billboard (2x2)",
 "entity.dranant.mutant_blood_beast":"Mutant Blood Beast","entity.dranant.assistant_pharmacist":"Assistant Pharmacist",
 "entity.dranant.assistant_scientist":"Assistant Scientist","effect.dranant.immortality":"Immortality",
 "subtitles.dranant.handcuff_rattle":"Handcuffs rattle","subtitles.dranant.syringe_injection":"Syringe injects",
 "subtitles.dranant.ground_slam":"Ground cracks","subtitles.dranant.cash_register_chime":"Cash register chimes","subtitles.dranant.diamond_pop":"Diamonds pop"}
TN=["Village Dispensary","Community Clinic","Health Centre","City Hospital","Medical College","Mega Hospital"]
for t in range(1,7): L[f"block.dranant.clinic_summoner_{t}"]=f"Clinic Level {t} - {TN[t-1]}"
L.update({"block.dranant.city_founder":"City Founder (Dr. Anant City)","item.dranant.dr_anant_bike":"Dr. Anant Bike",
 "item.dranant.sidekick_whistle":"Sidekick Whistle","item.dranant.sidekick_blaster":"Sidekick Blaster","item.dranant.sidekick_spawn_egg":"Sidekick Spawn Egg",
 "entity.dranant.sidekick":"Sidekick","entity.dranant.sidekick_bullet":"Blaster Round","entity.dranant.dr_anant_bike":"Dr. Anant Bike",
 "subtitles.dranant.bike_engine":"Bike engine roars","subtitles.dranant.bike_horn":"Bike horn","subtitles.dranant.sidekick_shot":"Sidekick fires","subtitles.dranant.sidekick_whistle":"Whistle",
 "block.dranant.stretcher":"Hospital Stretcher","entity.dranant.shopkeeper":"Shopkeeper","entity.dranant.patient_villager":"Patient Villager",
 "item.dranant.shopkeeper_spawn_egg":"Shopkeeper Spawn Egg","item.dranant.patient_villager_spawn_egg":"Patient Villager Spawn Egg",
 # v4
 "item.dranant.plasma_rifle":"Plasma Rifle","item.dranant.surgeon_katana":"Surgeon's Katana","item.dranant.defibrillator_hammer":"Defibrillator Hammer",
 "item.dranant.supercar_veloce":"Anant Veloce V12","item.dranant.supercar_gt":"Anant Stradale GT","item.dranant.supercar_phantom":"Anant Phantom Noir",
 "entity.dranant.supercar":"Supercar","entity.dranant.blood_boulder":"Blood Boulder","entity.dranant.blood_orb":"Blood Orb",
 "effect.dranant.stunned":"Stunned",
 "key.categories.dranant.vehicles":"Dr. Anant Vehicles","key.dranant.horn":"Horn","key.dranant.lights":"Headlights",
 "key.dranant.doors":"Open / close doors","key.dranant.hood":"Open / close bonnet","key.dranant.trunk":"Open / close boot",
 "subtitles.dranant.plasma_shot":"Plasma rifle fires","subtitles.dranant.katana_dash":"Katana dash","subtitles.dranant.defib_shock":"Defibrillator shock",
 "subtitles.dranant.car_engine":"Supercar engine","subtitles.dranant.car_horn":"Car horn","subtitles.dranant.car_door":"Car door",
 "subtitles.dranant.car_crash":"Car crashes","subtitles.dranant.car_nitro":"Nitro boost"})
# furniture names come from the generated FurnitureType enum (tools/gen_furniture.py)
import re as _re
FURN=_re.findall(r'^\s+[A-Z_]+\("([a-z_]+)", "([^"]+)"',open('src/main/java/com/dranant/block/FurnitureType.java').read(),_re.M)
for fid,title in FURN: L[f"block.dranant.{fid}"]=title
L.update({"entity.dranant.seat":"Seat","block.dranant.house_founder":"Modern House"})
w(f'{A}/lang/en_us.json',L)
w(f'{D}/minecraft/tags/block/mineable/axe.json',{"replace":False,"values":[M+f for f,_ in FURN]})

# ---------- loot tables (1.21: data/<ns>/loot_table/blocks) ----------
blocks=[f'clinic_summoner_{t}' for t in range(1,7)]+['central_shop_spawner','item_display_pedestal','examination_bed','diamond_cash_register','poster_landscape','poster_square','poster_billboard','stretcher','city_founder']
for b in blocks:
    w(f'{D}/dranant/loot_table/blocks/{b}.json',{"type":"minecraft:block","pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":M+b}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]})
w(f'{D}/minecraft/tags/block/mineable/pickaxe.json',{"replace":False,"values":[M+b for b in blocks if not b.startswith('poster') and b not in ('examination_bed','stretcher')]})
w(f'{D}/minecraft/tags/item/chest_armor.json',{"replace":False,"values":[M+'dr_anant_coat']})

# ---------- structures (1.21 folder name is "structure") ----------
# v2 re-order by size: tier 4 = original level 4, tier 5 = original level 6, tier 6 = original level 5.
# Original levels 1-3 were empty files, so tiers 1-3 are procedural.
shutil.rmtree(f'{D}/dranant/structure',ignore_errors=True); os.makedirs(f'{D}/dranant/structure',exist_ok=True)
for tier,orig in {4:4,5:6,6:5}.items(): shutil.copy(f'/home/claude/assets_src/clinic_level_{orig}.nbt',f'{D}/dranant/structure/clinic_level_{tier}.nbt')
print('json done')
