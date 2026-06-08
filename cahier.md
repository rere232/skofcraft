# SKOFCRAFT

## Snus Kingdom Of Freedom Craft

### Informations générales

| Élément                 | Valeur                                         |
| ----------------------- | ---------------------------------------------- |
| Nom du mod              | SKOFCRAFT                                      |
| Signification           | Snus Kingdom Of Freedom Craft                  |
| Package Java            | `ch.rere232.skofcraft`                         |
| Version Minecraft cible | 1.20.1                                         |
| Loader                  | Forge                                          |
| Énergie                 | Forge Energy (FE)                              |
| Type                    | Agriculture, industrie, économie, consommation |

---

# Objectif

SKOFCRAFT ajoute une chaîne de production complète de snus et de nicotine pouches.

Le joueur doit pouvoir :

* Cultiver du tabac
* Produire du snus
* Produire des nicotine pouches
* Utiliser des machines alimentées en FE
* Automatiser sa production
* Remplir des boîtes de snus
* Consommer les produits via une interface de gencives
* Développer une industrie complète autour du tabac

---

# Progression

Aucun système de recherche.

La progression se fait uniquement par les crafts.

Chaque machine avancée doit utiliser les machines précédentes dans sa recette.

Exemple :

```text
Broyeur FE
 └─ nécessite Broyeur Manuel

Mélangeur FE
 └─ nécessite Mélangeur Manuel

Emballeuse FE
 └─ nécessite Presse FE

Machine Industrielle
 └─ nécessite Machine FE
```

Le joueur progresse naturellement grâce aux ressources et aux recettes.

---

# Ressources

## Agriculture

* Graine de tabac
* Plant de tabac
* Feuille de tabac fraîche
* Feuille de tabac séchée
* Tabac moulu

## Nicotine

* Fibre végétale
* Base neutre
* Extrait de nicotine
* Cristaux de nicotine

## Arômes

* Menthe
* Ice Mint
* Berry
* Citrus
* Réglisse
* Cola
* Café
* Nature

## Emballage

* Sachet vide
* Sachet de snus
* Nicotine pouch
* Boîte vide
* Boîte de snus
* Boîte de nicotine pouches

---

# Machines

## Machines manuelles

* Séchoir manuel
* Broyeur manuel
* Mélangeur manuel
* Presse à sachets manuelle

## Machines FE

* Séchoir FE
* Broyeur FE
* Mélangeur FE
* Presse FE
* Extracteur de nicotine FE
* Emballeuse FE

## Machines industrielles

* Ligne de production de snus
* Ligne de production de pouches
* Emballeuse industrielle
* Extracteur industriel

---

# Consommation énergétique

## Exemple d'équilibrage

| Machine       | FE/t |
| ------------- | ---- |
| Séchoir FE    | 20   |
| Broyeur FE    | 40   |
| Mélangeur FE  | 60   |
| Presse FE     | 60   |
| Emballeuse FE | 80   |
| Extracteur FE | 100  |
| Industrielle  | 200  |

Toutes les machines doivent être compatibles :

* Forge Energy
* Hoppers
* Pipes
* Câbles FE

---

# Production du Snus

```text
Tabac frais
↓
Séchage
↓
Feuille séchée
↓
Broyage
↓
Tabac moulu
↓
Ajout sel + arôme
↓
Mélange de snus
↓
Presse à sachets
↓
Snus
↓
Emballeuse
↓
Boîte remplie
```

## Variantes

* Nature
* Mint
* Ice Mint
* Berry
* Réglisse
* Strong
* Extra Strong
* Premium

---

# Production des Nicotine Pouches

```text
Fibre végétale
↓
Base neutre
↓
Ajout nicotine
↓
Ajout arôme
↓
Mélange
↓
Presse
↓
Pouch
↓
Emballeuse
↓
Boîte remplie
```

## Variantes

* Mint
* Ice Mint
* Berry
* Citrus
* Cola
* Coffee
* Strong
* Extra Strong

---

# Boîtes de Snus et Pouches

## Capacité

Chaque boîte contient :

```text
20 unités
```

## Fabrication

Une boîte ne peut être remplie que via une emballeuse.

## Fonctionnement

Lorsque le joueur fait clic droit :

* Une unité est retirée de la boîte
* Une snus ou pouch est ajoutée à l'inventaire
* La boîte perd 1 point de durabilité

Exemple :

```text
20/20
19/20
18/20
...
0/20
```

Lorsque la boîte est vide :

* Elle devient une boîte vide

## Informations affichées

Tooltip :

```text
Contenu restant : 13/20
```

---

# Interface des Gencives

## Ouverture

Touche configurable :

```text
G
```

## Nombre de slots

```text
4 slots
```

Disposition :

```text
Haut Gauche
Haut Droite

Bas Gauche
Bas Droite
```

---

# Placement d'une Snus ou d'une Pouch

Le joueur peut glisser une snus ou une pouch dans un slot libre.

Lorsqu'un produit est placé :

* Il devient actif
* Un timer démarre
* Le produit ne peut plus être retiré

---

# Durée d'utilisation

Durée par défaut :

```text
15 minutes
```

Chaque produit possède une barre de progression similaire à une durabilité.

Exemple :

```text
██████████ 100%
██████░░░░ 60%
██░░░░░░░░ 20%
```

Cette barre représente le temps restant dans la gencive.

---

# Verrouillage

Pendant les 15 minutes :

* Impossible de retirer la snus
* Impossible de déplacer la snus
* Impossible de remplacer la snus

Le slot reste verrouillé.

---

# Fin d'utilisation

Lorsque le timer atteint 0 :

* Le produit disparaît
* Le slot se libère automatiquement
* Les effets cessent

---

# Effets

## Positifs

* Speed léger
* Haste léger
* Focus
* Réduction fatigue

## Négatifs

* Nausée
* Fatigue
* Lenteur

---

# Tolérance

Option configurable.

Plus le joueur consomme :

* Plus la tolérance augmente
* Plus les bonus diminuent

---

# Addiction

Option configurable.

Peut provoquer :

* Fatigue
* Lenteur
* Réduction temporaire des bonus

---

# Villageois

## Profession

Buraliste

## Bloc de métier

Comptoir de Buraliste

## Vente

* Graines de tabac
* Arômes
* Sachets vides
* Boîtes vides
* Produits simples

## Achat

* Feuilles séchées
* Tabac moulu
* Boîtes remplies
* Produits premium

---

# Compatibilité

## Forge Tags

```text
forge:crops/tobacco
forge:seeds/tobacco
forge:dusts/tobacco
forge:salt
forge:fibers
forge:flavors
forge:nuggets/nicotine
forge:storage_blocks/tobacco
```

## Tags SKOFCRAFT

```text
skofcraft:snus
skofcraft:nicotine_pouches
skofcraft:snus_boxes
skofcraft:pouch_boxes
skofcraft:machines
skofcraft:tobacco_products
skofcraft:flavors
```

## Mods compatibles visés

* JEI
* EMI
* REI
* Mekanism
* Thermal
* Pipez
* Farmer's Delight
* Create (compatibilité future)

---

# Interfaces Graphiques

Machines :

* Séchoir
* Broyeur
* Mélangeur
* Presse
* Emballeuse
* Extracteur

Interface spéciale :

* Interface des gencives

Toutes les interfaces affichent :

* Progression
* FE stockée
* Input
* Output

---

# Configuration

Fichier :

```text
skofcraft-common.toml
```

Options :

```text
enableAddiction=true
enableTolerance=true
enableNegativeEffects=true

tobaccoGrowthSpeed=1.0

machineEnergyMultiplier=1.0

snusDurationMinutes=15
pouchDurationMinutes=15

boxCapacity=20

enableVillager=true
```

---

# Onglet Créatif

Nom :

```text
SKOFCRAFT
```

Catégories :

* Agriculture
* Ressources
* Machines
* Arômes
* Snus
* Pouches
* Boîtes

---

# Advancements

* Bienvenue dans SKOFCRAFT
* Première récolte
* Premier séchage
* Premier snus
* Première pouch
* Première boîte remplie
* Premier buraliste
* Production automatisée
* Industrie SKOFCRAFT
* Royaume du Snus

```
```
