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
  - [SKOFee](#skofee)
  - [The Tobacconist](#the-tobacconist)
  - [The Phone](#the-phone)
  - [Aviation](#aviation)
  - [The Swiss Postbus](#the-swiss-postbus)
  - [The Nuclear Aircraft Carrier](#the-nuclear-aircraft-carrier)
  - [Space Program](#space-program)
  - [Tactical Armor](#tactical-armor)
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

Crash at speed and you get an explosion and some salvage.

On the ground there is also the **Panzer VIII Maus**: 188 tons, a turret that follows your view, an 8-second main gun, and the ability to drive through a fence without slowing down. Built on the same workbench and run on the same fuel.

Two more share its controls. The **Tiger II** has heavy armor, a long 88 mm gun, a slow turret and a small fuel tank. The **Leopard 1** does 65 km/h and reloads in 5 seconds, but its armor won't stop a machine gun.

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

## Controls

| Key | Action |
| --- | --- |
| **G** | Open the gum |
| **V** | Spit (while chewing tobacco) |
| **Shift + right-click** | Crank a manual machine |
| **W / S** | Throttle (F-14) or forward / back (Apache) |
| **A / D** | Rudder (F-14) or strafe (Apache) |
| **Space / C** | Climb / descend (Apache) |
| **H** | Sound the horn (Postbus) |
| **Attack / Use** | Guns / missiles and rockets |

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
