# DM Gym : propositions de logo et d'icône

Chaque SVG est une planche autonome (900 x 320) : icône adaptative (masque rond), version monochrome Android 13, rendus à 48 px, et logo complet « DM Gym » avec la signature « la salle de sport du MJ ».

Conventions communes :
- Grille adaptative 108 x 108 ; le premier plan reste dans le cercle sûr de 66 (rayon 33 autour de (54,54)). Les groupes `bg`, `fg` et `mono` des SVG sont directement exportables en `ic_launcher_background`, `ic_launcher_foreground` et `ic_launcher_monochrome` (VectorDrawable).
- Palette du design system uniquement : brique `#8A2E1C`, ardoise `#3F5565`, laiton `#F2C94C` / `#6B5000`, parchemin `#FBF5EA`, cuir sombre `#1B1612`.
- Texte en `<text>` Alegreya (repli Georgia, serif) avec `textLength` pour garder la largeur ; à vectoriser (Inkscape, « Objet en chemin ») avant livraison.
- Dans la planche, la monochrome simule le « trou » par la couleur de fond `#D9E6F0` ; pour l'export réel, ces zones doivent être transparentes (retirer ces formes ou utiliser un masque).
- Limite : je n'ai pas pu rendre les SVG dans un navigateur ; ils sont relus à la main (géométrie et balisage). À ouvrir une fois pour valider l'aspect.

---

## 01 Tour de donjon (`01-tour-donjon.svg`)
**Concept.** Une tour crénelée parchemin sur fond brique, fenêtre allumée au laiton, pieds dans les nuages. Suit l'idée de l'utilisateur.
**Ce qu'il dit.** Le donjon, donc le JdR ; la fenêtre éclairée évoque la veille du MJ ; les nuages donnent un côté « on s'élève ».
**À 48 px.** Très bon : silhouette verticale unique, crénelage encore visible, fenêtre dorée lisible comme point focal.
**Variantes.** Fond sombre `#1B1612` avec tour laiton ; tour qui gagne un étage selon la série de séances (animation de progression) ; fenêtre en forme de flamme.

## 02 Caverne et nuages (`02-caverne-nuages.svg`)
**Concept.** Un rocher brun percé d'une entrée sombre où brûle une petite torche, des nuages parchemin à la base, sur ciel ardoise. Suit l'idée de l'utilisateur.
**Ce qu'il dit.** L'inconnu qu'on explore sans carte : très proche de l'improvisation. La torche rejoint l'identité « lueur de torche ».
**À 48 px.** Correct : l'arche sombre et la flamme se lisent, mais le brun du rocher sur ardoise a un contraste modeste ; les nuages se fondent un peu.
**Variantes.** Ciel brique pour plus de chaleur ; rocher plus clair ; flamme qui vacille (animation simple).

## 03 d20 sablier (`03-d20-sablier.svg`)
**Concept.** Un d20 parchemin dont la face centrale est un sablier brique, le sable en laiton.
**Ce qu'il dit.** Le hasard (dé) et la contrainte de temps (sablier) : le cœur des séances. Le plus fidèle à la mécanique de l'application.
**À 48 px.** Excellent : hexagone net, sablier à fort contraste brique sur parchemin.
**Variantes.** Fond cuir sombre ; sable qui s'écoule en animation du minuteur ; sablier seul, sans arêtes, pour les très petites tailles (notification).

## 04 Haltère de dés (`04-halteres-des.svg`)
**Concept.** Un haltère en diagonale dont les poids sont un d6 et un d20, barre en laiton, sur cuir sombre.
**Ce qu'il dit.** L'entraînement physique et le jeu : la traduction littérale de « Gym ». Ton plus humoristique.
**À 48 px.** Moyen : les dés mesurent environ 9 px ; la diagonale aide mais les pips et l'intérieur du d20 disparaissent.
**Variantes.** Dés agrandis et barre raccourcie ; deux d20 ; haltère à plat avec un seul d20 comme poids.

## 05 Écran de MJ et flamme (`05-ecran-flamme.svg`)
**Concept.** Un écran de MJ à trois volets brique, losange laiton au centre, surmonté d'une flamme.
**Ce qu'il dit.** Le métier de MJ, immédiatement reconnaissable des joueurs ; la flamme, l'inspiration et la torche.
**À 48 px.** Bon : trois panneaux et flamme restent distincts ; les volets latéraux sombres perdent un peu de contraste sur le fond.
**Variantes.** Fond brique avec écran parchemin ; flamme remplacée par un petit d20 ; volets en perspective.

## 06 Plume et sentier (`06-plume-sentier.svg`)
**Concept.** Une plume brique dont la pointe trace un chemin pointillé ardoise jusqu'à un point laiton, sur fond parchemin.
**Ce qu'il dit.** Écrire vite pour avancer dans l'histoire : lien direct avec la saisie, la trace et l'historique. Le plus doux et littéraire.
**À 48 px.** Moyen : la plume est lisible, mais le pointillé fin s'estompe ; fond clair peu typé « application de jeu ».
**Variantes.** Trait plein plus épais à petite taille ; fond sombre avec plume parchemin ; point laiton devenant une flamme.

---

## Comparatif

| | 01 Tour | 02 Caverne | 03 d20 sablier | 04 Haltère | 05 Écran | 06 Plume |
|---|---|---|---|---|---|---|
| Lisibilité en petit | Très bonne | Correcte | Excellente | Moyenne | Bonne | Moyenne |
| Originalité | Faible (tours très courantes) | Moyenne | Moyenne | Forte | Moyenne | Faible |
| Rapport au produit (entraînement + impro de MJ) | JdR seulement | JdR + inconnu | Chrono + hasard : fort | « Gym » + dés : fort | MJ seulement | Écriture : moyen |
| Facilité d'animation | Moyenne (fenêtre qui s'allume) | Bonne (flamme) | Très bonne (sable) | Faible | Bonne (flamme) | Bonne (tracé du chemin) |

## Recommandation

**03 d20 sablier**, avec 01 comme choix de repli si l'utilisateur tient à l'univers donjon.
- C'est la seule icône qui raconte les deux piliers de la spec : une contrainte aléatoire (le dé) et un chrono (le sablier), donc une séance courte.
- Elle est la plus lisible à 48 px et reste sobre en monochrome (un silhouette d'hexagone avec un sablier découpé).
- Elle s'anime naturellement avec le minuteur (écoulement du sable) et se décline en logo sans texte.
- Son risque : une impression « générique » de dé ; atténué par le sablier et le laiton.

DÉCISION: concept retenu — 03 d20 sablier / 01 tour / autre — recommandation : 03, puis affiner couleur de fond (brique ou cuir sombre) sur le design final.
DÉCISION: fond de l'icône — brique `#8A2E1C` / cuir sombre `#1B1612` — recommandation : brique pour se distinguer des applications sombres du lanceur, et assortie au thème clair par défaut.
