Cahier des charges — SKOFCRAFT

Nom complet : SKOFCRAFT
Signification : Snus Kingdom Of Freedom Craft
Package Java : ch.rere232.skofcraft
Version cible : Minecraft Java 1.20.1
Type de mod : Agriculture, production industrielle, consommation, économie
Énergie : Forge Energy / FE

1. Objectif du mod

SKOFCRAFT ajoute une chaîne complète de production de snus et de nicotine pouches dans Minecraft.

Le joueur doit pouvoir :

Cultiver du tabac
Sécher les feuilles
Broyer les matières
Créer des mélanges
Produire des sachets
Remplir des boîtes de 20
Utiliser les snus via une interface de gencives
Automatiser la production avec des machines FE
Acheter/vendre certains items avec un villageois buraliste
2. Progression

La progression se fait uniquement par les crafts.

Chaque machine avancée demande une ancienne machine dans sa recette.

Exemple :

Broyeur FE = Broyeur manuel + redstone + fer + composant énergétique
Mélangeur FE = Mélangeur manuel + cuivre + redstone + moteur
Machine industrielle = Machine FE + acier + circuit avancé

Pas besoin de système de recherche, niveau ou quête obligatoire.

3. Ressources ajoutées
Agriculture
Graine de tabac
Plant de tabac
Feuille de tabac fraîche
Feuille de tabac séchée
Tabac moulu
Nicotine pouches
Fibre végétale
Base neutre
Extrait de nicotine
Cristaux de nicotine fictifs
Poudre aromatisée
Mélange pour pouch
Arômes
Menthe
Ice mint
Citrus
Berry
Réglisse
Cola
Café
Nature
Emballage
Sachet vide
Sachet de snus
Nicotine pouch
Boîte vide
Boîte de snus
Boîte de nicotine pouches
4. Machines
Machines manuelles
Séchoir manuel
Broyeur manuel
Mélangeur manuel
Presse à sachets manuelle
Machines FE
Séchoir FE
Broyeur FE
Mélangeur FE
Extracteur de nicotine FE
Presse à sachets FE
Emballeuse FE
Machines industrielles
Ligne industrielle de snus
Ligne industrielle de nicotine pouches
Emballeuse industrielle
Extracteur avancé

Chaque machine FE doit avoir :

Stockage interne en FE
Consommation par tick
Temps de traitement
Slots input/output
Compatibilité hopper
Compatibilité pipes si possible
Interface graphique
5. Forge Energy

Les machines utilisent Forge Energy.

Exemple d’équilibrage :

Séchoir FE : 20 FE/tick
Broyeur FE : 40 FE/tick
Mélangeur FE : 60 FE/tick
Presse FE : 60 FE/tick
Emballeuse FE : 80 FE/tick
Extracteur FE : 100 FE/tick
Machine industrielle : 200 FE/tick
6. Production du snus

Étapes :

Tabac frais
→ Séchage
→ Feuille de tabac séchée
→ Broyage
→ Tabac moulu
→ Mélange avec sel + arôme
→ Mélange de snus
→ Presse à sachets
→ Sachets de snus
→ Emballeuse
→ Boîte de snus remplie

Types de snus :

Snus nature
Snus menthe
Snus ice mint
Snus berry
Snus réglisse
Snus strong
Snus extra strong
Snus premium
7. Production des nicotine pouches

Étapes :

Fibre végétale
→ Base neutre
→ Ajout extrait de nicotine
→ Ajout arôme
→ Mélange pour pouch
→ Presse à sachets
→ Nicotine pouch
→ Emballeuse
→ Boîte de nicotine pouches remplie

Types de pouches :

Mint pouch
Ice mint pouch
Citrus pouch
Berry pouch
Cola pouch
Coffee pouch
Strong pouch
Extra strong pouch
8. Boîtes de 20

Les boîtes fonctionnent comme un item avec durabilité.

Une boîte pleine contient 20 snus ou 20 pouches.

Comportement :

Clic droit avec une boîte dans la main
Donne 1 snus/pouch dans l’inventaire
Retire 1 durabilité à la boîte
Quand la boîte arrive à 0, elle devient une boîte vide ou disparaît selon config

Exemple :

Boîte pleine : 20/20
Après clic droit : 19/20
Après 20 utilisations : boîte vide

À prévoir :

Tooltip affichant le contenu restant
Différents modèles/textures selon le type
Stack size faible, par exemple 1 ou 4
Possibilité de remplir uniquement via l’emballeuse
9. Interface de gencives

Ajouter une interface spéciale pour placer les snus/pouches.

Nom possible :

Interface de gencives

Fonctionnement :

4 slots disponibles
Chaque slot peut contenir 1 snus ou 1 nicotine pouch
Les effets durent tant que le produit est dans un slot
Le produit se consomme progressivement
Quand la durée est terminée, le slot se vide

Slots :

Gencive haut gauche
Gencive haut droite
Gencive bas gauche
Gencive bas droite

Règles possibles :

Maximum 4 produits actifs
Plus il y en a, plus les effets sont forts
Trop de produits donne des malus
Interface ouverte avec une touche dédiée, par exemple G
10. Effets en jeu

Effets positifs possibles :

Vitesse légère
Haste léger
Réduction de fatigue
Focus : minage plus rapide
Résistance légère au ralentissement

Effets négatifs possibles :

Nausée si abus
Lenteur si surconsommation
Malus temporaire après usage excessif
Tolérance progressive
11. Addiction / tolérance

Option configurable.

Mécaniques :

Plus le joueur consomme, plus la tolérance augmente
Les effets deviennent moins forts
Si le joueur arrête brutalement, il peut avoir un malus temporaire
Tout doit être désactivable dans la config

Config :

enableAddiction = true/false
enableTolerance = true/false
enableNegativeEffects = true/false
12. Villageois buraliste

Ajouter une profession :

Buraliste

Bloc de métier possible :

Comptoir de buraliste

Il peut vendre :

Graines de tabac
Sachets vides
Boîtes vides
Arômes
Machines basiques
Snus simples
Nicotine pouches simples

Il peut acheter :

Feuilles séchées
Tabac moulu
Boîtes de snus
Boîtes de pouches
Produits premium

Pas de système d’interdiction dans les villages.

13. Compatibilité avec les autres mods

Ajouter des tags pour faciliter la compatibilité.

Tags items
forge:crops/tobacco
forge:seeds/tobacco
forge:dusts/tobacco
forge:nuggets/nicotine
forge:storage_blocks/tobacco
forge:tools/knives
forge:salt
forge:flavors
forge:fibers
Tags SKOFCRAFT
skofcraft:snus
skofcraft:nicotine_pouches
skofcraft:snus_boxes
skofcraft:pouch_boxes
skofcraft:flavors
skofcraft:tobacco_products
skofcraft:machines
Compatibilités visées
JEI / REI / EMI pour les recettes
Forge Energy
Mekanism, Thermal, Pipez ou autres mods de câbles FE
Farmers Delight via tags agricoles si possible
Create via compatibilité mécanique optionnelle plus tard
14. Interfaces graphiques

Interfaces à prévoir :

Séchoir
Broyeur
Mélangeur
Presse
Emballeuse
Extracteur
Interface de gencives
Buraliste

Chaque GUI machine affiche :

Énergie stockée
Progression
Input
Output
Recette active
15. Config

Fichier de configuration :

skofcraft-common.toml

Options :

enableAddiction
enableTolerance
enableNegativeEffects
tobaccoGrowthSpeed
machineEnergyMultiplier
snusEffectDuration
pouchEffectDuration
boxCapacity
enableVillager
enableJEICompat
16. Onglet créatif

Ajouter un onglet créatif :

SKOFCRAFT

Contenu :

Graines
Plantes
Ressources
Arômes
Machines
Snus
Pouches
Boîtes
Blocs de métier
17. Advancements

Succès possibles :

Bienvenue au royaume du snus
Récolter du tabac
Premier séchage
Premier snus
Production automatisée
Boîte pleine
Buraliste officiel
Surconsommation
Usine SKOFCRAFT
