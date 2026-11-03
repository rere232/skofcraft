<div align="center">

# SKOFCRAFT

### Snus Kingdom Of Freedom Craft

*Grow it. Dry it. Grind it. Pack it. Then build an aircraft carrier and put satellites in orbit.*

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A)
![Forge](https://img.shields.io/badge/Forge-47.4+-1E2A3A)
![Curios](https://img.shields.io/badge/Requires-Curios%205.14+-8A5CF6)
![Version](https://img.shields.io/badge/version-0.1.0--alpha-orange)

</div>

---

SKOFCRAFT started as a snus and nicotine pouch mod: a full production chain from a tobacco seed to a filled tin, powered
by hand cranks and then by Forge Energy. It did not stop there. Today it also has a coffee industry, a smartphone with a
casino and a delivery service, flyable jets and helicopters, a nuclear aircraft carrier, a space program and an
intercontinental missile that leaves a crater 140 blocks wide.

Everything is explained in game in the **Quest Book**, sold by the Tobacconist or crafted.

## Contents

- [SKOFCRAFT](#skofcraft)
    - [Snus Kingdom Of Freedom Craft](#snus-kingdom-of-freedom-craft)
  - [Contents](#contents)
  - [Installation](#installation)
  - [The tobacco industry](#the-tobacco-industry)
    - [Snus](#snus)
    - [Nicotine pouches](#nicotine-pouches)
    - [More ways to get your fix](#more-ways-to-get-your-fix)
    - [Flavors](#flavors)
  - [Consuming](#consuming)
  - [Cuddling](#cuddling)
  - [SKOFee](#skofee)
  - [The Tobacconist](#the-tobacconist)
  - [The Phone](#the-phone)
  - [Aviation](#aviation)
  - [The Nuclear Submarine](#the-nuclear-submarine)
  - [The Swiss Postbus](#the-swiss-postbus)
  - [The Nuclear Aircraft Carrier](#the-nuclear-aircraft-carrier)
  - [Space Program](#space-program)
  - [Tactical Armor](#tactical-armor)
  - [The Snus Kingdom](#the-snus-kingdom)
  - [Pastel Paradise](#pastel-paradise)
  - [Regrettable Blocks](#regrettable-blocks)
  - [Controls](#controls)
  - [Configuration](#configuration)
  - [Building from source](#building-from-source)
  - [En bref (français)](#en-bref-français)

---

## Installation

| Requirement | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4 or newer |
| [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) | 5.14 or newer (required) |
| [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) | 15.x (optional) |

Drop the SKOFCRAFT jar and Curios into your `mods` folder.

With **JEI** installed, every machine gets its own recipe tab (drying, extraction, grinding, mixing, pressing, packaging,
SKOFee brewing and the 5×4 Aircraft Workbench), with flavored products cycling in step with their flavor, the machines
listed as catalysts, and info pages on how to get each flavor.

---

## The tobacco industry

Progression is pure crafting, no research tree: every advanced machine is built from the one before it.

```text
Manual (hand-cranked)  ──►  FE (Forge Energy)  ──►  Industrial (one block, whole line)
 Manual Dryer               FE Dryer                Industrial Snus Line
 Manual Grinder             FE Grinder              Industrial Pouch Line
 Manual Mixer               FE Mixer                Industrial Packager
 Manual Pouch Press         FE Press                Industrial Extractor
                            FE Nicotine Extractor
                            FE Packager
```

Manual machines are cranked with **Shift + right-click**. FE machines accept power from any Forge Energy source and
work with hoppers and pipes.

### Snus

```text
Tobacco Seeds ─► Fresh Leaf ─► Dried Leaf ─► Ground Tobacco ─► Snus Pouch ─► Snus Box
                (harvest)     (dryer)       (grinder)          (press +       (packager,
                                                                empty pouch)   20 pouches)
```

### Nicotine pouches

```text
Sugar Cane ─► Plant Fiber ─► Neutral Base ─┐
Dried Leaf ─► Nicotine Extract ────────────┼─► Mixer ─► Nicotine Mix ─► Press ─► Nicotine Pouch ─► Pouch Box
Flavor ────────────────────────────────────┘
```

Strength comes from the nicotine you put in: **Extract** for a normal pouch, one **Nicotine Crystal** (smelted extract)
for Strong, two for Extra Strong.

### More ways to get your fix

| Product | How |
| --- | --- |
| Snuff | Ground tobacco + flavor in the mixer |
| Cigarillo, Cigar | Dried leaves and ground tobacco on the crafting table |
| Cigarettes | Plant fiber, ground tobacco and paper; packed 20 to a pack by the packager |
| Chewing Tobacco | Dried leaves and sugar. You have to spit (V) at least every 5 minutes |
| Vape | An empty vape filled with nicotine mix in the mixer, 30 puffs |
| Chicha | A placeable hookah with 4 hoses, so up to 4 players can share it |

### Flavors

| Flavor | Bonus | Flavor | Bonus |
| --- | --- | --- | --- |
| Mint | Speed | Cola | Jump Boost |
| Ice Mint | Speed II, Haste II | Licorice | Resistance |
| Berry | Regeneration | Coffee | Haste II, Speed |
| Citrus | Haste II | Nature | Luck |

Coffee can be crafted from ground SKOFee. The others are sold by the Tobacconist as he levels up.

---

## Consuming

Press **G** to open your **gum**: four slots where pouches, snus and chewing tobacco sit while they are active.

- A pouch lasts **15 minutes** (chewing tobacco 30) and gives **Speed** and **Haste**, more with two or more active.
- Strong, Extra Strong and Premium pouches hit harder but build **tolerance** and **addiction** faster.
- Three pouches at once cause nausea; four slow you down.
- Quit with a heavy addiction and you get **withdrawal**: weakness and mining fatigue.
- **Addictol** (golden apples and milk) wipes tolerance and addiction clean.

Every penalty can be turned off in the config.

---

## Cuddling

Look at another player and press **B**. They get 15 seconds to press it back, and nothing happens if they don't. While
you cuddle, you both regenerate slowly and your phone addiction eases. Press **B** again or sneak to let go. Taking damage
also ends it.

---

## SKOFee

A second crop: plant SKOFee seeds, dry the beans, roast them, grind them and brew them in the **SKOFee Machine** with a cup
and a water bucket. A cup gives three minutes of Speed and Haste. Add milk for a latte.

There is also the **One Million Beer**: one item, a million drinks, nearly fourteen days of non-stop drinking.

---

## The Tobacconist

Place a **Tobacconist Counter** next to an unemployed villager. He sells seeds, flavors, packaging, finished products,
Addictol and the Quest Book, and buys leaves, ground tobacco, extract and filled boxes. Five levels of trades to unlock.
Some plains villages spawn with a tobacco house already built.

---

## The Phone

A working smartphone with a battery, a home screen and five apps:

| App | What it does |
| --- | --- |
| **Scrollr** | An endless feed to doomscroll and like |
| **Amazooon** | Buy finished products for diamonds; a cargo plane airdrops the crate on your head |
| **Flappy Block** | Tap to fly through the pipes |
| **Lucky Creeper** | A casino: slots, blackjack and roulette, all rolled server-side, with real diamonds |
| **Sat Map** | Live satellite imagery, unlocked by a recon satellite in orbit |

Screen time builds a **phone addiction**: notification badges, a feed that scrolls itself, phantom notifications and,
eventually, real withdrawal. Touch grass in daylight to get over it.

---

## Aviation

Aircraft are built on the **Aircraft Workbench** (a 5×4 grid) from parts made on a crafting table: gears, aircraft
plates, turbine and rotor blades, avionics, jet engines and weapon systems. They run on **Jet Fuel Canisters**.

| Aircraft | Role | Armament |
| --- | --- | --- |
| **AH-64 Apache** | Attack helicopter, hovers and strafes, two seats | Chain gun and rockets |
| **F-14 Tomcat** | Swing-wing fighter, carrier capable | Vulcan cannon and heat-seeking Sidewinders |
| **FPV Drone** | Remote-piloted kamikaze quad, crafted by twos on a crafting table, needs no fuel | Itself |

Crash at speed and you get an explosion and some salvage.

An FPV Drone puts your camera on the drone and leaves your body standing around unattended, so pick a safe spot. The signal reaches about as far as your view distance, the battery lasts four minutes, and a direct hit goes through tank armor.

On the ground there is also the **Panzer VIII Maus**: 188 tons, a turret that follows your view, an 8-second main gun, and the ability to drive through a fence without slowing down. Built on the same workbench and run on the same fuel.

Two more share its controls. The **Tiger II** has heavy armor, a long 88 mm gun, a slow turret and a small fuel tank. The **Leopard 1** does 65 km/h and reloads in 5 seconds, but its armor won't stop a machine gun.

---

## The Nuclear Submarine

An Ohio-class ballistic missile submarine, 48 blocks long, built on the Aircraft Workbench around a whole Naval Nuclear
Reactor. It takes no fuel, and everyone aboard breathes for free.

- **W / S** set the throttle, **A / D** the rudder, and **Space / C** surface and dive. The dive alarm is included.
- **Sonar:** always hears ships and torpedoes close by. **Attack** sends an active ping that charts everything in the water further out, and everyone nearby hears it.
- **Torpedoes:** **Use** fires a homing torpedo that blows a flooded hole in what it hits.
- **Missiles:** right-click the boat with a Giga ICBM to load one of its 16 tubes, mark a target, stop, come up shallower than 70 m and press **N**. No pad, no canisters.
- Radar can't see it once the sail is under. If the hull gives out, the reactor melts down.

---

## The Swiss Postbus

A yellow postal bus for nine, built on the Aircraft Workbench and run on jet fuel, because the timetable is not
negotiable. **W / S** throttle and brake, **A / D** steer, and **H** sounds the real three-tone horn (C♯, E, A), audible
from 128 blocks away.

---

## The Nuclear Aircraft Carrier

The biggest thing in the mod. Build a **Naval Nuclear Reactor** (it needs a nether star), a Carrier Helm, catapults and
arresting wires, then assemble the carrier and place it on open water.

- At anchor it is ordinary blocks: walk around, build on it, park your jets.
- Take the helm and it sails as one ship.
- Launch F-14s from the **steam catapults** and land them on the **arresting wires**.
- Defend it with **Radar**, a **CIWS** gun and **SAM launchers**, and fix it with **Damage Control Kits**.
- Mind the reactor. It can melt down.

---

## Space Program

Build a **Launch Pad** under open sky, load a satellite and 8 fuel canisters, and launch.

| Payload | Effect |
| --- | --- |
| **Recon Satellite** | +256 blocks to every radar in the dimension, and unlocks the Sat Map phone app |
| **Strike Satellite** | Mark a target with the **Orbital Designator** and a tungsten rod falls from orbit. One minute per satellite to recharge |
| **Giga ICBM** | The end of the tech tree |

The **Giga ICBM** needs a draconic warhead (a dragon head, a nether star, end crystals, netherite blocks and a whole
nuclear reactor), a guidance unit and boosters, and 32 fuel canisters. Mark a target on the Sat Map or with the
designator, launch, and listen to ten seconds of sirens. It leaves the world and comes back down: a white flash, a crater
140 blocks wide lined with obsidian and lava, a rim of rubble, scorched land and a purple mushroom cloud you can see from
anywhere. The blast is spread over several ticks so the server survives it.

Creative mode also has the **Doomsday Missile**, at five times the yield.

---

## Tactical Armor

A military set beyond netherite. Each piece upgrades its netherite counterpart with composite plates and a **Tactical
Core** built around a nether star.

| Piece | Bonus |
| --- | --- |
| **Helmet** | Gas mask and thermal visor: night vision, water breathing, immune to poison, wither, nausea, blindness, darkness and hunger |
| **Plate Carrier** | +4 hearts; arrows and bullets do 60% less |
| **Leggings** | +15% movement speed |
| **Boots** | Step up full blocks; 80% less fall damage |
| **Gloves** (Curios hands slot) | +3 attack damage, +15% attack speed, +2 armor |

**Full set:** immune to fire and falls, 70% less explosion damage, and an **auto-medkit** that heals you when you drop
low. 28 armor and toughness 5 per piece.

---

## The Snus Kingdom

The homeland. Put **four Extra Strong pouches** in your gum at once (Premium counts) and you pass out and wake up on the floor of its castle.

- **Staying:** you stay as long as there is anything in your gum. When the last pouch runs out, withdrawal throws you back where you collapsed. Addictol wakes you up at once.
- **The throne:** empty for 900 years, because the king has to pull Excalibur and nobody has managed it. The **Interim Regent** is temporarily in charge, and sells **Royal Reserve** flavor (Strength, Resistance), **Regent's Blend** and a very reasonable sword.
- **Kingdom Nobles:** wander the fields selling Regent's Blend (Fire Resistance, Water Breathing) and Addictol. Killing one is a matter for HR.
- **The land:** Tobacco Fields with lit Cigar Trees, Grounds Dunes with wild SKOFee, and Tin Mountains of packed snus under giant tins, capped with pouch fabric. The sea and rivers are liquid SKOFee, which gives Speed and Haste and fits in a bucket.
- **Also:** it is always late afternoon, nothing hostile lives there, and beds explode.

Operators can use `/snuskingdom` to go there and back without the overdose.

---

## Pastel Paradise

Soft floating islands in a pink sky. Wear all six pieces of the fit, plus ears and a tail (cat, bunny or fox), and hug a Blahaj to get there. Sneak and hug one to come home. If you fall off an island, Blahaj catches you.

- **The land:** pink grass, cherry trees, cotton candy clouds, Femboys, rabbits and cats. Nothing hostile spawns and it is always midday.
- **Structures:** cottages, Headpat Cafes, plush towers and striped boutiques, with snacks, SKOFee and clothes in the chests.
- **New clothes:** Pastel Skirt, Striped Thigh Highs, Pastel Crop Top, Heart Choker, Bunny Ears and Fox Tail, found only in those chests.
- **The Rightful Fit:** the rarest drop, in about 1 boutique chest in 50. A Royal Tiara, a Royal Gown and Glass Slippers.

Wear all of The Rightful Fit in the Snus Kingdom and **Excalibur comes out of the stone**. The sword beats netherite and the Interim Regent loses their job. Right-click the empty stone with the sword to put it back.

**It's where you recover from the rest of the mod:**

- **Recovery:** your phone addiction, nicotine tolerance and nicotine addiction fade while you are here, twice as fast in a hot spring. Stay 5 minutes and you go home **Well Rested** (+2 hearts, quicker swing, 20 minutes).
- **Heart Tokens:** headpats earn them, at most one every 30 seconds. Boutique Keepers sell clothes, Cotton Puffs, Blahaj, couches, altars and, for two stacks of tokens, The Rightful Fit.
- **Therapy Couch:** 8 tokens take one incident off your HR record.
- **Getting around:** Cotton Puffs push you between islands. Cotton Candy Clouds and Marshmallow take no fall damage and bounce you.
- **Gloom:** only come for players with a phone addiction of 25 or more, and their hits make it worse.
- **The Algorithm:** offer your phone at a Heart Altar in a Plush Tower to summon this boss. It drops the **Do Not Disturb Charm**, which stops your phone feeding the habit.

Operators can use `/pastelparadise` to go there and back.

---

## Regrettable Blocks

37 blocks that exist because nobody stopped us. All of them are craftable, and the recipes are in the Quest Book.

| Block | What it does |
| --- | --- |
| **Swiss Bureaucracy Block** | Right-click to file a request. Mining one drops two, which is how bureaucracy works. |
| **Block of Swiss Neutrality** | Right-click: monsters within 16 blocks forget who they were chasing, and everyone nearby, monsters and players alike, gets Weakness for 10 seconds. |
| **Health Insurance Block** | Right-click to pay a diamond for 3 seconds of Regeneration. Without a diamond, you are not covered. |
| **Postbus Stop** | Right-click to check the timetable. Exactly 10 seconds later the horn sounds and the bus has already left. |
| **Load-Bearing Snus Tin** | Break it and the ceiling above comes down: 3 columns wide, up to 16 blocks high. Containers, fluids and bedrock stop the collapse. |
| **Strategic Snus Reserve** | Right-click to release the reserve: Speed II and Haste II for 15 seconds, then some nausea. Takes 30 seconds to refill. |
| **HR Complaint Box** | Right-click to file a complaint. One in ten comes back as an incident on your own record, and enough incidents get you training. |
| **Office Microwave** | Right-click with raw food to cook one instantly, with a small chance of charcoal. Ingots and nuggets explode. Five second cooldown. |
| **Swivel Chair** | Right-click to spin 180 degrees. That is all it does. You will do it again. |
| **Thought Leader Block** | Right-click for 10 seconds of Haste and a lesson about B2B sales you did not ask for. |
| **Unpaid Intern In A Box** | Right-click to send the intern on an errand, once a minute. Usually brings back junk. One errand in twenty brings a SKOFee. |
| **Compressed Monday** | Walking on it gives Slowness and Mining Fatigue. On an actual Monday, it gives level III of both. On an actual Friday, it does nothing. |
| **Passive-Aggressive Note** | Stick it somewhere for your colleagues to find. Right-click to read it. |
| **Block of Server Lag** | Walking on it sometimes rubberbands you 3 blocks backwards. |
| **Missing Texture Block** | This is the finished texture. Right-click to report the bug. |
| **Definitely Stone** | Looks exactly like stone. Hisses when you walk on it. Never explodes. You will still flinch every time. |
| **Diamond Ore?** | Hide it in a friend's mine. Drops one piece of coal and a laugh. |
| **Indoor Grass** | Touch it to lower your phone addiction by 2%, every 10 seconds. The real thing still works better. |
| **Certified Fireproof Block** | Burns like nothing else, spreads fire fast and sets you alight when you walk on it. Right-click to test the certification. Do not build your house from it. |
| **Excalibur (From Amazooon)** | Anyone can pull it from the stone. It comes out as a wooden sword with one hit left in it. |
| **Amazooon Package** | Place it and open it. Inside: paper and usually a smaller package. Very rarely a diamond. |
| **Ejection Seat** | Right-click to leave very quickly, straight up, roughly 30 blocks. One ejection in ten has no parachute, and that fall will probably kill you. |
| **Banana Peel** | Walk over it and you slide off in the direction you were looking. |
| **Wet Floor** | More slippery than ice. You were warned, and that is what the sign is legally for. |
| **Whoopee Cushion** | Stand on it. You know what it does. |
| **Bouncy Castle Block** | No fall damage, and every bounce goes higher than the last, up to about 20 blocks. Sneak to stop. |
| **Emergency Exit** | Right-click to go straight up to the highest solid block above it. Works everywhere, including in the Nether, which is how you end up on the roof. |
| **Slightly Radioactive Barrel** | Glows. Now and then it gives everyone within 4 blocks Hunger and Glowing. Right-click to lick it for Haste II and Poison. |
| **Loot Box** | Right-click to pay a diamond. 90% dirt or a stick, 9% two diamonds, 1% netherite scrap. The odds are published here, unlike elsewhere. |
| **Router With One Bar** | Plays your phone's notification sound at random, so you check your phone for nothing. Right-click to reboot it, for all the good that does. |
| **Smoke Detector (Low Battery)** | Chirps every 20 to 80 seconds, forever. There is no battery to change. Hide one in someone's base. |
| **Launch Code Note** | The launch code, on a sticky note. Right-click to read it. It does not launch anything, but the code is correct. |
| **Emotional Support Anvil** | Hug it to lower your phone addiction by 3% and get a little Regeneration. One hug in twenty goes badly. |
| **Debugging Duck** | Right-click to explain your problem. The duck quacks some advice. |
| **Cat Loaf** | Pet it for a little Regeneration. One time in five, it knocks an item out of your hand. |
| **Judgmental Block** | Right-click for an honest review of your health, armor, depth, phone habits and what you are holding. |
| **Reverse Sand** | Falls upwards until something stops it. |

The **Certificate of "I Know What I'm Doing"** isn't a block, but it belongs here. Craft it from your Certificate of Completion, an ink sac and a gold ingot, then right-click to sign it. HR then waits for 30 incidents instead of 3, for good.

---

## Controls

| Key | Action |
| --- | --- |
| **G** | Open the gum |
| **V** | Spit (while chewing tobacco) |
| **B** | Ask for a cuddle, accept one or let go |
| **Shift + right-click** | Crank a manual machine |
| **W / S** | Throttle (F-14) or forward / back (Apache) |
| **A / D** | Rudder (F-14) or strafe (Apache) |
| **Space / C** | Climb / descend (Apache, FPV Drone) |
| **Sneak** | Cut the link (FPV Drone) |
| **H** | Sound the horn (Postbus) |
| **N** | Launch a missile (Submarine) |
| **Attack / Use** | Guns / missiles and rockets; Attack detonates an FPV Drone |

All keys can be rebound in the controls menu.

---

## Configuration

`config/skofcraft-common.toml`

| Option | Default | |
| --- | --- | --- |
| `enableAddiction` | `true` | Addiction penalties |
| `enableTolerance` | `true` | Tolerance system |
| `enableNegativeEffects` | `true` | Nausea, slowness and other side effects |
| `enableVillager` | `true` | The Tobacconist |
| `enableMandatoryHr` | `true` | Consequences |
| `enableCuddling` | `true` | Player cuddles |
| `enableSnusKingdom` | `true` | Passing out into the Snus Kingdom on four Extra Strong pouches |
| `enablePastelParadise` | `true` | Hugging your way into Pastel Paradise |
| `tobaccoGrowthSpeed` | `1.0` | Tobacco growth multiplier (0.1 – 10) |
| `skofeeGrowthSpeed` | `1.0` | SKOFee growth multiplier (0.1 – 10) |
| `machineEnergyMultiplier` | `1.0` | FE/t multiplier for all machines |
| `snusDurationMinutes` | `15` | How long a snus pouch lasts |
| `pouchDurationMinutes` | `15` | How long a nicotine pouch lasts |
| `chewDurationMinutes` | `30` | How long chewing tobacco lasts |
| `boxCapacity` | `20` | Pouches per box, cigarettes per pack |
| `vapeUses` | `30` | Puffs per vape |
| `skofeeDurationSeconds` | `180` | Effect length of a cup of SKOFee |

---

## Building from source

Curios and JEI are downloaded from their mavens automatically.

```bash
./gradlew build          # jar in build/libs
./gradlew runClient      # dev client
```

Requires Java 17.

---

## En bref (français)

SKOFCRAFT est un mod Forge 1.20.1 (Curios requis). Il propose :

- Une chaîne complète de production de **snus** et de **nicotine pouches**, des machines manuelles jusqu'aux lignes
  industrielles en FE, plus cigares, cigarettes, tabac à chiquer, vapes et chicha.
- Le **SKOFee**, un **Buraliste** villageois et un **téléphone** avec casino, livraison par avion et carte satellite.
- **Pastel Paradise** : des iles flottantes roses, de nouveaux vetements, et la tenue qui permet de retirer Excalibur.
- Le **Royaume du Snus** : quatre pouches Extra Fortes en meme temps, et vous vous reveillez sur place.
- L'**Apache**, le **F-14**, un **porte-avions nucléaire**, un **programme spatial**, l'**ICBM géant** et une **armure
  tactique** au-delà de la netherite.

Tout est expliqué en jeu dans le **Livre de quêtes**. Touche **G** pour les gencives, **V** pour cracher.

---

<div align="center">

Made by **Rere232** and **I_Honza_I** · All Rights Reserved

Postbus horn: ["Postauto (CH) Dreiklanghorn"](https://commons.wikimedia.org/wiki/File:Postauto_(CH)_Dreiklanghorn.ogg)
by Mateus2019, [CC BY 2.0 DE](https://creativecommons.org/licenses/by/2.0/de/deed.en). Trimmed, normalized and converted
to mono. This licence covers that recording, not the rest of the mod.

</div>
