# Doctor Anant's Mega Clinic & The Expired Mutant Outbreak — v4
NeoForge **1.21.1** · Java **21** · Mod ID `dranant`

```bash
./gradlew runClient     # dev client
./gradlew build         # -> build/libs/dranant-4.0.0.jar
```
If Gradle can't find a version, bump `neo_version` (gradle.properties) / `net.neoforged.moddev` (build.gradle) to the newest 21.1.x / 2.0.x.

## What's new in v4
**Mutant Blood Beast = real boss** (500 HP, armor 0, 64-block aggro, agile):
| Ability | Range | Effect |
|---|---|---|
| Boulder Throw | long | rips a blood boulder from the ground and hurls it (12 dmg blast, no block damage) |
| Blood Spit | long | 3 (enraged 5) poison orbs |
| Pounce | mid | leaps onto you, 10 dmg on landing |
| Claw Combo | short | 3 sweeping slashes, 7 dmg each |
| Stun Slam | short | jump + ground shockwave, 8 dmg + **Stunned** (can't move/jump, 2.5 s) |
| Grab & Throw | short | picks you up, squeezes, then hurls you (8 dmg) - **sneak to break free** |
| + Ground-crack slam, charge, roar, blood geysers, minions below 30%, dodges ~20% of hits | | |

* **Fight pacing:** above 50% HP the beast can lose at most half its health per ~1.8 min (`bossSecondsToHalfHealth = 108` in the config). You + the sidekick reach 50% in about 1.8 minutes - on the bike or on foot.
* **Death explosion:** when it dies it bursts for **15 hearts** (30 dmg) at the centre, falling off to 8 blocks. Run!
* Sidekick fires short bursts (~1.5 dmg/s) and still stops at 50%.

**Boss-hunting weapons** (HD 128px, in the Central Shop):
* **Plasma Rifle** (8,000 💎) - hitscan beam 48 blocks, 9 dmg, x2 in the beast's back, works while riding.
* **Surgeon's Katana** (6,000 💎) - 12 dmg; right-click = 8-block Scalpel Dash (10 dmg to everything you pass).
* **Defibrillator Hammer** (10,000 💎) - 17 dmg; right-click = CLEAR! shockwave, stuns the beast 1.5 s.

**ANANT MOTORS supercar showroom** (new cities: south side next to the ring road; existing v3 city: `/city showroom`):
glass hall, 3 lit podiums (centre = turntable) with the cars **already parked**, buy-your-own pedestals.
| Car | HP | 0-100 | Top | Doors | Price |
|---|---|---|---|---|---|
| Anant Veloce V12 | 790 | 2.9 s | 187 km/h | scissor | 450,000 |
| Anant Stradale GT | 650 | 3.2 s | 166 km/h | conventional | 300,000 |
| Anant Phantom Noir | 1020 | 2.5 s | 209 km/h (+active wing) | gullwing | 750,000 |
* 4x HD textures, see-through glass, glowing LED DRLs, dashboard gauges & ambient light, headlights, light-bar tail lamps, quad exhaust, carbon aero.
* Physics: torque curve, 7-speed auto box + RPM, brakes/reverse, **SPACE handbrake drift**, **SPRINT nitro** (+35%, flames), speed-sensitive steering, body roll/dive/squat, step-climb, crashes bounce & shake - **cars never explode**, ramming hurts mobs.
* HUD: analogue speedometer + rev counter, digital km/h, gear (R/N/1-7), nitro bar, drift score, lamps.
* Keys (rebindable, "Dr. Anant Vehicles"): **H** horn, **J** lights, **U** doors, **N** bonnet, **M** boot. Sneak + right-click a door / the front / the back to open it from outside. Sneak + punch = pick up (showroom cars only in creative).
* 2 seats - the sidekick hops in with you.

**Bike** - 112 km/h, SPRINT turbo to 155 km/h, SPACE rear-brake power-slide, wheelies, strong brakes, same HUD.

**Dr. Anant's Villa** (new cities: south side, next to the showroom; existing city: `/city house`) - a 2-storey modern smart home,
fully furnished: open-plan kitchen with island + bar stools, 8-seat dining, living room with smart TV, L-sofa and marble tables,
quartz staircase, master bedroom with TV + wardrobe, home office, bathroom (vanity, toilet, rain shower, tub), roof-terrace lounge,
pool with underwater lights, landscaped garden and a Stradale GT parked in the driveway.

**13 HD furniture blocks** (64x64 textures, multi-part models, Creative tab): Modern Sofa, Marble Coffee Table, Smart TV & Media Unit,
Walnut Dining Table, Designer Chair, Kitchen Counter, Kitchen Sink, Induction Range, French-Door Fridge (2 blocks tall), Arc Floor Lamp
(light), Workstation Desk, Wall-Hung Toilet, Bathroom Vanity & Mirror. **Right-click the sofa, chairs or toilet to sit.**

**Ninja sidekick** - he now wears an HD ninja outfit (hood with red headband and eye slit, red-trimmed gi, sash with kunai,
katana on his back, bandage wraps, tabi boots) over your own player skin (config `sidekickUsesOwnerSkin`, now on by default).

**Other v4 changes**
* Clinic Summoner blocks: **placing the block builds the clinic right on that spot** (entrance facing you). Left-click placement still gives a plain block - right-click it to build there.
* Pedestals: sneak + right-click with any item (or right-click an empty pedestal) to put it on display - shop pedestals too.
* Diamond Counter speed: `/diamondcounter speed <0-100>` or chat `#speed 2` / `#speed 0.5`. HUD shows `[x2]`.
* Patients talk much more: greet you by name, chat with each other in line (question + answer), get impatient, react to rain/night, beg for the medicine you hold, panic and jump away from the Blood Beast, 3-step conversation when you keep right-clicking them.

## What was new in v3
**Dr. Anant City** — `/city build` (or place the **City Founder** block). A 299 × 297 city is built in front of you, gate facing you, rising live:
* City wall with crenellations, 12 towers, 4 gates, "DR. ANANT CITY" hologram + banners.
* Green belt of cherry/oak/birch/azalea trees and hedges, ring road with lane markings and street lamps, 4 avenues, side streets.
* ~48 numbered buildings (cottages with chimneys and gardens, apartment blocks with balconies and rooftop gardens, villas with pools and palm trees, bazaar rows, sakura gardens, office towers) — each has a glowing **"Building No. N"** sign.
* 4 landmark plazas: **Town Hall** with clock tower and bell, **Dr. Anant Statue** (your skin as a 35-block pixel statue), **Fountain Park**, **Chandni Bazaar**.
* Villagers, iron golems and cats move in. Villagers walk to your clinics as patients.

**Clinic District** (211 × 209 empty land in the middle) with 7 reserved plots sized from the real buildings, line by line:
Row 1: PLOT 1 Shop (21×21), PLOT 2 L1 (19×17), PLOT 3 L2 (25×21), PLOT 4 L3 (31×25), PLOT 5 L4 (23×38), PLOT 6 L5 (46×63) · Row 2: PLOT 7 L6 Mega Hospital (127×128).
Each plot has a curb, lamps, a sign and a hologram (EMPTY / BUILT – Clinic #N). Healing gardens with ponds fill the free space.
* Inside the city, clinic summoners, the shop spawner and shop purchases build straight into the right plot.
* `/build clinic <1-6>`, `/build shop` · `/erase clinic <1-6>`, `/erase clinic all`, `/erase shop` (animated top-down erase, lawn restored; also works for clinics built outside the city) · `/city info` (building count, plots, clinic numbers) · `/city tp`.

**Sidekick + Bike** — use the **Sidekick Whistle**: "Sidekick Bunty" (Minecraft player/Steve skin; config `sidekickUsesOwnerSkin` for your skin) follows you and jumps on whatever you ride — the new 2-seat **Dr. Anant Bike** (W/S throttle, A/D steer with lean), boats, camels, horses, minecarts. When he sees the Mutant Blood Beast he opens fire with his blaster, even while riding, and **stops at exactly 50% HP** so you can handcuff and cure it.

## What was new in v2
**Villagers / patients**
* Clinic customers are now `Patient Villager` entities (vanilla villager look, biome + profession skins) driven 100% by the clinic AI → they always walk INTO the clinic and form neat straight lines (1.2 blocks apart). Stuck patients hop to their spot.
* Every patient says its own problem in a speech bubble (fever, golgappe stomach ache, skeleton arrow, Warden...). Mild → Line 1 medicine. Severe → Line 2: they **lie on stretchers**, then on the **Examination Bed**.
* At the counter they argue and haggle with the pharmacist (~9 s of dialogue), pay, get medicine and leave healthy.
* Patients choose the **closest clinic**. Nearby vanilla villagers are converted into patients and turned back into the same villager (trades kept) when they leave.
* Assistants get random names (Raju, Pappu, Dr. Sneha...). The shop has a **Seller** (Seth Dhaniram etc.) who randomly talks to you — sometimes friendly, sometimes rude.

**Clinics**
* Only **6 levels**, ordered small → huge: 1 Village Dispensary, 2 Community Clinic, 3 Health Centre (procedural), 4 City Hospital, 5 Medical College, 6 Mega Hospital (your .nbt files, re-ordered by size).
* **Right-click** with a summoner = build the clinic **7 blocks in front of you**, entrance facing you. **Left-click** = place only the block (right-click that block later to build).
* Buildings rise **layer by layer** with sounds, particles and fireworks.

**Shop**
* 19×7×19 Central Shop: 14 medicine/tool pedestals + **Clinic Upgrades gallery** (Level 1-6: 5K / 20K / 60K / 150K / 350K / 750K — all under 1 million).
* Right-click a clinic upgrade → green **preview outline** in front of you (follows where you look) → right-click again within 6 s to **buy & build**.
* Hologram fix: only the pedestal you look at shows the full name/price card (it lifts and spins faster); others show a small price tag — no more overlapping text.
* **Shift + right-click** any (non-shop) pedestal with an item to display it, empty hand to take it back. `/pedestalprice <amount>` changes a price.

**Diamond Counter = Diamond Bank**
* `#run diamond counter` → picked-up diamonds disappear from your inventory and are added to the counter; the counter also grows non-stop on an accelerating curve and reaches **1,000,000 in 5 minutes** (configurable).
* Purchases are paid from the counter (then inventory). Rolling number HUD, live "+X/sec", pulse + green/red popups.
* `#stop diamond counter`, `#reset diamond counter`, `/diamondcounter run|stop|reset|set <n>`.

**Boss** — Phase 2 enrage below 50% HP (faster, shaking, shorter cooldowns), **Bull Charge** (gets stunned if it hits a wall), **Blood Roar** shockwave, **Blood Geysers** (dodge the red circles), 3 **Infected Patient** minions at 30% HP, speech bubbles, new animation poses.

**HD textures** — 64-128 px medicines/items/blocks, 128 px assistant & seller skins, 256 px boss, 512-1024 px posters with **your skin**.

## Notes
* `/city build`, `/build` and `/erase` need cheats/op (permission level 2). The city needs ~300×300 blocks of space; it levels terrain and takes ~30-60 s to build.
* Original clinic_level_1/2/3.nbt were empty, so tiers 1-3 are generated in code. Drop real files named `clinic_level_1.nbt`.. into `src/main/resources/data/dranant/structure/` to override.
* Structure "front" is assumed to be the template's south (+Z) side when turning it toward the player.
* Config: `config/dranant-common.toml` (customer spawn rate, queue length, passive growth target/time, auto-deposit).
* Model credits (CC-BY-4.0) in `CREDITS.txt`.
