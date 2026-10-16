# DM Gym — Design system (Material 3)

Date : 2026-10-16. Auteur : ux-designer. Base : Material 3 1.4.0 (Compose). Parcours : `parcours.md`. Écrans à maquetter : `brief-maquettes.md`.
Les tokens sont donnés en valeurs hexadécimales pour que le maquettiste en fasse des variables CSS et le développeur un `ColorScheme`. Noms = rôles M3.

## 1. Identité : « la salle de sport du MJ »

Image : une salle d'entraînement à la torche, en pierre et en cuir. On y pose sa craie, son carnet et ses disques de fonte.

| Élément | Traduction visuelle | Pourquoi |
|---|---|---|
| **Parchemin** (clair) / **cuir sombre** (sombre) | Fonds chauds crème et brun-noir, jamais blanc pur ni noir pur | Chaleur JdR sans pâtir de la fatigue oculaire des fonds blancs |
| **Brique** (primaire) | Rouge brique profond, couleur de l'action principale | Évoque tapis de sol, sceau de cire ; reste distinct de l'erreur |
| **Ardoise** (secondaire) | Bleu-gris froid pour l'information et les contrôles | La « craie sur ardoise » du tableau d'entraînement |
| **Laiton** (tertiaire) | Or chaud, utilisé pour mots imposés, temps additionnel, bijou | Couleur « récompense » et « attention douce », jamais « alerte » |
| **Carte de tirage** | Forme à coins coupés (haut droit, bas gauche), liseré fin | Rappelle carte à jouer et jeton ; seule forme non standard de l'app |
| **Disque de fonte** | Numéros des champs en pastilles rondes à contour | Compter ses « reps » : 1 à 8 |
| **Titres en serif** | Alegreya (empattement calligraphique) | Voix de grimoire ; réservé aux titres, jamais à la saisie |
| **Gemme** | Icône losange pour « bijou » | Trésor à garder, repérable par la forme |
| **Icônes** | Material Symbols Rounded (+ 3 icônes dédiées au besoin : dé, gemme, haltère) | Cohérence M3, embarquées (hors ligne) |

Règle d'or : **le décor reste dans les marges, les cartes et les titres.** La zone de saisie est un aplat neutre (surface) à fort contraste, sans texture ni image.

Pas d'illustrations décoratives derrière le texte ; les états vides peuvent avoir une illustration simple (ligne, 2 couleurs), masquée à 150 % de police et plus pour libérer la place.

## 2. Couleurs

### 2.1 Mode clair « Parchemin »

| Rôle M3 | Hex | Usage dans DM Gym |
|---|---|---|
| primary | `#8A2E1C` | Bouton principal, indicateur actif, barre de progression du minuteur |
| onPrimary | `#FFFFFF` | Texte sur bouton principal |
| primaryContainer | `#FFDAD0` | Carte « Séance du jour », bouton « Commencer » tonal |
| onPrimaryContainer | `#3B0C03` | Texte sur ce conteneur |
| secondary | `#3F5565` | Icônes, contrôles secondaires |
| onSecondary | `#FFFFFF` | |
| secondaryContainer | `#D9E6F0` | Puces sélectionnées, sélecteur de niveau choisi, minuteur « dernière minute » |
| onSecondaryContainer | `#112430` | |
| tertiary | `#6B5000` | Gemme du bijou, icône temps additionnel |
| onTertiary | `#FFFFFF` | |
| tertiaryContainer | `#FFE08A` | **Surlignage des mots imposés**, minuteur en temps additionnel, bandeau de bascule |
| onTertiaryContainer | `#231A00` | |
| error | `#B3261E` | Erreurs techniques seulement (table illisible, suppression) |
| onError | `#FFFFFF` | |
| errorContainer | `#FFDAD6` | Carte d'erreur de table |
| onErrorContainer | `#410002` | |
| background | `#FBF5EA` | Fond d'écran |
| onBackground | `#2A211C` | |
| surface | `#FBF5EA` | Fond des écrans et de la zone de saisie |
| onSurface | `#2A211C` | Texte principal, texte saisi |
| surfaceVariant | `#F0E6D6` | Fond de champs, cartes de session |
| onSurfaceVariant | `#4F433A` | Texte secondaire, libellés |
| outline | `#7A6C5F` | Contours de champs, cases, séparateurs porteurs d'information |
| outlineVariant | `#D3C5B3` | Séparateurs décoratifs uniquement |
| surfaceDim | `#E3DACB` | |
| surfaceBright | `#FBF5EA` | |
| surfaceContainerLowest | `#FFFDF8` | Cartes de tirage (face) |
| surfaceContainerLow | `#F6EFE2` | Cartes d'exercice |
| surfaceContainer | `#F0E6D6` | Zone épinglée, barre de navigation |
| surfaceContainerHigh | `#EAE0CF` | Minuteur (état normal) |
| surfaceContainerHighest | `#E4D9C7` | Champs de saisie |
| inverseSurface | `#3B322B` | Snackbar |
| inverseOnSurface | `#F7EDE0` | |
| inversePrimary | `#FFB4A2` | Action dans la snackbar |
| scrim | `#000000` à 32 % | Arrière-plan de feuille modale |

### 2.2 Mode sombre « Cuir à la torche »

| Rôle M3 | Hex |
|---|---|
| primary | `#FFB4A2` |
| onPrimary | `#5B1608` |
| primaryContainer | `#7A2A18` |
| onPrimaryContainer | `#FFDAD0` |
| secondary | `#B7CAD9` |
| onSecondary | `#1C3342` |
| secondaryContainer | `#2E4252` |
| onSecondaryContainer | `#D9E6F0` |
| tertiary | `#F2C94C` |
| onTertiary | `#3B2D00` |
| tertiaryContainer | `#5A4300` |
| onTertiaryContainer | `#FFE08A` |
| error | `#FFB4AB` |
| onError | `#690005` |
| errorContainer | `#93000A` |
| onErrorContainer | `#FFDAD6` |
| background / surface | `#1B1612` |
| onBackground / onSurface | `#EDE0D4` |
| surfaceVariant | `#26201B` |
| onSurfaceVariant | `#CFC2B5` |
| outline | `#988B7F` |
| outlineVariant | `#4F443A` |
| surfaceDim | `#14100D` |
| surfaceBright | `#3F3832` |
| surfaceContainerLowest | `#120E0B` |
| surfaceContainerLow | `#211B16` |
| surfaceContainer | `#26201B` |
| surfaceContainerHigh | `#302A24` |
| surfaceContainerHighest | `#3B342E` |
| inverseSurface | `#EDE0D4` |
| inverseOnSurface | `#35291F` |
| inversePrimary | `#8A2E1C` |
| scrim | `#000000` à 50 % |

Mode par défaut : suit le système (Réglages permet de forcer). Pas de couleurs dynamiques (Material You) en v1 : la palette « thème » est l'identité ; « thèmes personnalisés » est hors périmètre.

### 2.3 Rôles sémantiques propres à l'app (alias de rôles M3, pas de nouvelles couleurs)

| Rôle app | Clair | Sombre | Notes |
|---|---|---|---|
| `timerNormal` | surfaceContainerHigh / onSurface | idem | Texte + barre primary |
| `timerLastMinute` | secondaryContainer / onSecondaryContainer | idem | + icône sablier plein + texte « Dernière minute » |
| `timerOvertime` | tertiaryContainer / onTertiaryContainer | idem | + forme (barre à rayures) + texte « Temps additionnel » |
| `wordHighlight` | tertiaryContainer + soulignement 2 dp onTertiaryContainer | idem | Jamais la couleur seule |
| `wordChipChecked` | secondaryContainer + coche | idem | |
| `gem` | tertiary | tertiary | Bijou actif ; vide = contour outline |
| `drawCard` | surfaceContainerLowest + contour outline 1 dp | surfaceContainerLow | Coins coupés |

### 2.4 Contrastes WCAG (vérification)

Méthode : luminance relative WCAG 2.x calculée à la main pour chaque paire à partir des hex ci-dessus. **À reconfirmer par un outil** (script de test ou Accessibility Scanner) avant la release ; les valeurs sont arrondies à une décimale. Seuils : texte ≥ 4,5:1 (AA), texte large (≥ 18 sp ou 14 sp gras) ≥ 3:1, composants d'interface et contours porteurs d'information ≥ 3:1.

| Paire (texte / fond) | Clair | Sombre | Seuil |
|---|---|---|---|
| onSurface / surface | 14,5 | 13,8 | 4,5 |
| onSurface / surfaceContainerHighest (champ) | 10,8 | 9,9 | 4,5 |
| onSurfaceVariant / surfaceContainer | 7,7 | 9,2 | 4,5 |
| onPrimary / primary | 8,4 | 7,9 | 4,5 |
| primary (texte lien) / surface | 7,8 | 10,5 | 4,5 |
| onPrimaryContainer / primaryContainer | 13,1 | 7,5 | 4,5 |
| onSecondaryContainer / secondaryContainer | 12,5 | 8,2 | 4,5 |
| onTertiaryContainer / tertiaryContainer (surlignage, minuteur temps additionnel) | 13,4 | 7,3 | 4,5 |
| onSurface / tertiaryContainer (texte surligné dans la zone de saisie) | 12,2 | 7,2 | 4,5 |
| tertiary (icône gemme) / surface | 7,0 | 11,3 | 3 |
| secondary / surface | 7,2 | 10,7 | 3 |
| onErrorContainer / errorContainer | 13,0 | 7,3 | 4,5 |
| error (texte) / surface | 6,0 | 10,5 | 4,5 |
| outline / surface (contour de champ, case) | 4,7 | 5,4 | 3 |
| outline / surfaceContainer | 4,1 | 4,9 | 3 |

Notes :
- Toutes les paires de texte dépassent AA ; le plus faible est outline/surfaceContainer, qui ne porte pas de texte.
- `outlineVariant` n'est **pas** utilisé pour un contour porteur d'information (contraste inférieur à 3:1, donc décoratif seulement).
- Le surlignage de mot imposé garde ≥ 7:1 pour le texte ; il est doublé d'un soulignement et d'un état coché dans les puces, donc lisible aussi en daltonisme et en niveaux de gris.
- Aucune information n'est portée par la couleur seule : voir §7.

## 3. Typographie

Polices : titres **Alegreya** (serif, OFL) ; corps et interface **Atkinson Hyperlegible Next** (OFL, conçue pour la lisibilité). Repli : `serif` / `sans-serif`. Chiffres du minuteur en **chiffres tabulaires** pour éviter que le texte « saute » à chaque seconde (si la police n'offre pas `tnum`, utiliser la police monospace du système pour le minuteur seulement). Voir DÉCISION 11 (`parcours.md`).

Toutes les tailles en **sp** ; interligne en sp ; aucun `maxLines` sur les contenus essentiels.

| Style M3 | Police | Taille/interligne (sp) | Graisse | Usage |
|---|---|---|---|---|
| displayLarge/Medium/Small | — | non utilisés | | |
| `timerLarge` (custom) | Atkinson, tabulaire | 48 / 56 | 700 | Minuteur de l'écran Prêt/En cours |
| `timerCompact` (custom) | Atkinson, tabulaire | 32 / 40 | 700 | Minuteur en zone épinglée compacte |
| headlineLarge | Alegreya | 32 / 40 | 700 | Titre de bienvenue, compteur de sessions |
| headlineMedium | Alegreya | 28 / 36 | 700 | Titre d'écran en grand |
| headlineSmall | Alegreya | 24 / 32 | 600 | Titre de section d'écran |
| titleLarge | Alegreya | 22 / 28 | 600 | Barre du haut, nom d'exercice, question de Core Image |
| titleMedium | Atkinson | 16 / 24 | 700 | Titre de carte, ligne de liste |
| titleSmall | Atkinson | 14 / 20 | 700 | Sous-titres |
| **bodyLarge (saisie et consigne)** | Atkinson | **18 / 28** | 400 | Texte saisi, consigne, tirage |
| bodyMedium | Atkinson | 16 / 24 | 400 | Texte courant |
| bodySmall | Atkinson | 14 / 20 | 400 | Aide, métadonnées |
| labelLarge | Atkinson | 14 / 20 | 700 | Boutons |
| labelMedium | Atkinson | 12 / 16 | 700 | Puces, badges |
| labelSmall | Atkinson | 12 / 16 | 500 | Plus petit texte de l'app (12 sp minimum, jamais d'information critique) |

Choix non évidents :
- Saisie et consigne à **18 sp** (M3 : 16) : lecture à bout de bras, en mobilité.
- Pas de texte sous 12 sp : à 200 % c'est 24 sp, tout reste lisible.
- Interligne de la saisie ×1,55 : relire ses idées sans se perdre.
- Majuscules fantaisie interdites sur le texte saisi ; petites capitales possibles sur les surtitres de cartes (« PROBLÈME », en style labelMedium).

## 4. Formes, espacements, élévation, icônes

### 4.1 Formes (dp)

| Token | Valeur | Usage |
|---|---|---|
| shape.extraSmall | 4 | Soulignement/indicateur, séparateurs |
| shape.small | 8 | Puces (mots imposés, filtres), badges |
| shape.medium | 12 | Cartes, champs, bandeau de bascule |
| shape.large | 16 | Zone épinglée, feuilles modales (coins hauts 28) |
| shape.extraLarge | 28 | Feuilles basses, gros boutons |
| shape.full | 50 % | Boutons, pastilles de numéro |
| **shape.drawCard** (custom) | coins coupés 16 en haut à droite et en bas à gauche, 12 sinon | Carte de tirage uniquement |

### 4.2 Espacements (grille de 4 dp)

| Token | dp | Usage |
|---|---|---|
| space.1 | 4 | Écart icône-texte serré |
| space.2 | 8 | Entre éléments d'un groupe ; **espace mini entre deux cibles tactiles** |
| space.3 | 12 | Entre champs numérotés ; entre cartes |
| space.4 | 16 | **Marge d'écran (compact)**, padding interne des cartes |
| space.5 | 24 | Marge d'écran ≥ 600 dp ; entre sections |
| space.6 | 32 | Respiration d'en-tête |
| space.7 | 48 | Hauteur tactile minimale |
| space.8 | 64 | Séparation de blocs en tablette |

- Largeur de contenu max : 640 dp centrée en tablette (lecture).
- Zone gestuelle/barres système : toujours traitées en insets, bouton « Commencer » au-dessus.
- Padding clavier : `imePadding` sur la zone défilante ; la zone épinglée est **hors** de la zone qui défile.

### 4.3 Élévation

M3 tonal (couleurs `surfaceContainer*`), ombres quasi absentes. Niveaux : 0 écran, 1 cartes (couleur `surfaceContainerLow`), 2 zone épinglée (couleur `surfaceContainer` + séparateur 1 dp en outlineVariant), 3 feuilles et menus. Justification : les ombres coûtent en lisibilité sur fonds chauds et en batterie en sombre.

### 4.4 Icônes (Material Symbols Rounded, noms à vérifier à l'implémentation)

| Usage | Icône |
|---|---|
| Crazy Eights | `counter_8` |
| Constraint Box | `deployed_code` |
| Core Image Threading | `hub` |
| Status Numbers | `playing_cards` |
| Decision Point Drill | `alt_route` |
| Connect the Story | `link` |
| One-Page Design | `description` |
| Séance du jour | `calendar_today` |
| Relancer | `casino` |
| Minuteur / dernière minute / temps additionnel | `timer` / `hourglass_bottom` / `add_circle` |
| Pause / reprise | `pause` / `play_arrow` |
| Bijou | `diamond` (contour = non, plein = oui) |
| Série | `local_fire_department` (jamais de flamme rouge) |
| Niveaux | `edit` (Brouillon), `construction` (Solide), `shield` (Prêt pour la table) |
| Enregistré | `check` |
| Navigation | `fitness_center` (S'entraîner), `menu_book` (Historique), `bar_chart` (Progrès) |

Sens (Constraint Box) : `hearing`, `air` (odorat), `back_hand` (toucher), `restaurant` (goût), `visibility` (vue).

## 5. Composants

Les noms de composants servent au maquettiste (classes) et au développeur (composables). Tous respectent §7.

### 5.1 Carte d'exercice (`ExerciseCard`)
- **Contenu** : icône dans une pastille (secondaryContainer, 48 dp), nom (titleMedium), discipline d'origine (bodySmall, onSurfaceVariant), durée à droite (labelMedium dans une puce small : « 8 min » ou icône `timer_off` + « Sans minuteur »), chevron.
- **Taille** : hauteur min 72 dp, cible entière cliquable. Fond surfaceContainerLow, shape.medium.
- **États** : défaut, appuyé (ondulation d'état), focus (contour 2 dp primary), « Fait aujourd'hui » (coche + texte « Fait » en haut à droite, pas seulement une teinte), « Préparation » (étiquette sur One-Page).
- **200 %** : disposition en colonne (la durée passe sous le nom), pas de troncature du nom.

### 5.2 Carte de tirage (`DrawCard`) avec relance comptée
- **Anatomie** : surtitre en petites capitales (« PROBLÈME », « LIEU », « IMAGE CENTRALE », « SITUATION »), contenu en grand (titleLarge pour courts, bodyLarge pour longs), liseré intérieur fin, pied avec **bouton « Relancer le tirage »** (tonal, icône `casino`) et **compteur de relances** (« 0 relance » / « 1 relance » / « 2 relances ») en bodySmall.
- **Variantes** :
  - *Texte* (Crazy Eights, Core Image, Decision Point, One-Page).
  - *Lieu + mots* (Constraint Box) : lieu en titre, trois puces de mot sous le lieu (voir 5.6).
  - *Carte à jouer* (Status Numbers) : grande valeur (headlineLarge 48 sp+ dans un coin et au centre), libellé textuel « Statut 8 sur 10 · haut » (jamais la valeur seule), rôle sous la carte. Les libellés d'extrémité : 1 = « tout en bas », 10 = « tout en haut ».
  - *Cinq éléments* (Connect the Story) : voir 5.8.
- **États** : squelette (chargement), face visible, retournement (≤ 300 ms, remplacé par fondu 100 ms ou rien si animations réduites), après relance (nouveau contenu, ancien annoncé remplacé), erreur (voir 5.13).
- **Compact (épinglée)** : surtitre masqué, contenu sur 2 lignes max puis « Voir tout » (étend en place) ; relance masquée pendant que le clavier est ouvert (retrouvée via menu « ⋮ »). Justification : gagner 100 dp sur l'espace du clavier.
- **A11y** : la carte est un seul élément de lecture ; la relance annonce « Nouveau tirage : … ». Le compteur est lu « Relances : 1 ».
- **Relance** : au pouce, 48 dp de haut, jamais à moins de 8 dp du bouton « Commencer ».

### 5.3 Minuteur (`SessionTimer`)
États (tous visuellement distincts **sans** la couleur) :

| État | Texte | Forme / icône | Conteneur | Remarque |
|---|---|---|---|---|
| Prêt | durée réglée (« 8:00 »), libellé « Prêt » | icône `timer`, barre pleine contour | surfaceContainerHigh | Touche = ouvre la durée |
| En cours | « 4:30 » (timerLarge) | barre de progression pleine qui se vide (6 dp) | surfaceContainerHigh | |
| Dernière minute (durées ≥ 3 min) | « 0:58 » + libellé « Dernière minute » | icône `hourglass_bottom`, barre épaisse 8 dp | secondaryContainer | une seule annonce TalkBack |
| Temps additionnel | « +0:42 » + libellé « Temps additionnel » | icône `add_circle`, barre **pleine à rayures** (motif diagonal 4 dp) qui ne diminue plus | tertiaryContainer | **fin douce** : pas de rouge, pas de clignotement |
| Pause | valeur figée + « En pause » | icône `pause`, barre à demi opacité avec bords hachurés | surfaceContainerHigh | |
| Sans minuteur | « Sans minuteur » | icône `timer_off`, pas de barre | surfaceContainer | Touche = « Lancer un minuteur » |

- **Transition vers le temps additionnel** : fondu 200 ms du conteneur, changement d'icône et de motif au même instant ; une seule vibration courte, un seul bip, une seule annonce. Pas de pulsation.
- **Taille** : minimum 56 dp de haut en compact (timerCompact), 96 dp en version complète. Boutons « Pause/Reprendre » 48 dp.
- **Sémantique** : un seul nœud d'accessibilité avec description textuelle (« Minuteur : 4 minutes 30 restantes »), **pas de live region à la seconde**. Annonces séparées (région polite) uniquement aux jalons : démarrage, dernière minute, temps additionnel, reprise.
- **Source du temps** : horodatage ; l'affichage se rafraîchit à la seconde, indépendamment des annonces.
- **Réduction de mouvement** : la barre se met à jour par pas d'une seconde sans animation fluide ; le motif rayé est statique.

### 5.4 Zone épinglée (`PinnedBrief`)
- Conteneur surfaceContainer, shape.large en bas, séparateur 1 dp. Contient minuteur (+ pause) et consigne/tirage compact. **Hauteur max 35 % de la zone visible (clavier compris).**
- Modes : *complet* (avant clavier), *compact* (clavier ouvert), *ligne unique* (paysage avec clavier ou police ≥ 150 % si > 35 %) : « 4:30 · Consigne ▾ ».
- Au débordement : la consigne se plie, jamais le minuteur ni le bouton Terminer.
- Dépliage : en place, pousse le contenu vers le bas et peut défiler ; ne couvre jamais le minuteur. Pas de feuille modale pendant l'exercice.

### 5.5 Champ numéroté (`NumberedField`)
- **Anatomie** : pastille à gauche (36 dp visuel, cible 48 dp) contenant le numéro ; champ plein (surfaceContainerHighest, shape.medium, contour outline 1 dp, 2 dp primary au focus) ; texte 18/28 ; hauteur min 56 dp, croissance jusqu'à 3 lignes puis défilement interne.
- **États** : *vide* (pastille à contour, placeholder « Idée 3 »), *focus* (contour 2 dp primary, pastille remplie primary), *rempli* (pastille remplie secondaryContainer, numéro conservé), *désactivé* (n'existe pas : tous les champs restent modifiables). Pas d'état erreur : un champ vide n'est pas une faute.
- **Compteur** : puce « 5/8 » + 8 segments (plein/contour). Segments de 12 dp, écart 4 dp.
- **Clavier** : IME Suivant → champ suivant ; dernier champ → Terminé (ferme le clavier). Majuscule auto en début de phrase ; correction auto laissée au clavier ; dictée vocale disponible (micro du clavier).
- **A11y** : description « Idée 3 sur 8, vide » / « Idée 3 sur 8 » + texte.
- **Pas de limite de caractères** (sauf compteur souple du One-Page).

### 5.6 Puce de mot imposé (`WordChip`)
- **Anatomie** : icône du sens (hearing, air, back_hand…) à gauche, mot, et à droite une case cochable. Fond surfaceContainerLowest + contour outline (non cochée) ; secondaryContainer + contour primary 2 dp + coche pleine (cochée) ; le mot n'est jamais barré, pour rester lisible.
- **Taille** : hauteur visuelle 40 dp, zone tactile 48 dp, largeur selon le mot, écart 8 dp.
- **États** : *non utilisée*, *utilisée (auto)* (coche + petit libellé « auto » dans l'étiquette d'accessibilité et un point discret `·`), *utilisée (manuel)*, *focus*. Le toucher bascule ; le manuel fait foi.
- **A11y** : rôle case à cocher ; « grésillement, ouïe, utilisé » / « non utilisé » ; bascule annoncée par TalkBack.
- **Flux** : en zone épinglée, passage en ligne scrollable si les 3 puces ne tiennent pas (font ≥ 150 % : une puce par ligne).

### 5.7 Surlignage des mots imposés (`WordHighlight`)
- **Rendu dans la saisie** : fond tertiaryContainer sur le mot, soulignement 2 dp (onTertiaryContainer) ; texte reste onSurface. Contraste texte/fond ≥ 7:1.
- **Comportement** : s'applique aux formes détectées (accents, casse, flexions via `keywords`), recalculé à chaque frappe sans bloquer ; ne surligne pas un mot si la case est décochée à la main.
- **Pas d'animation** à l'apparition. Un léger scintillement serait une distraction sous pression.
- **A11y** : le surlignage n'est pas annoncé par TalkBack ; l'information passe par les cases (5.6, 5.8) lues dans l'ordre.
- **Détail en lecture (S09)** : mêmes surlignages, figés.

### 5.8 Case « élément utilisé » (`ElementCheckRow`, Connect the Story)
- **Anatomie** : case 24 dp dans une cible 48 dp, icône de catégorie (lieu `place`, objet `inventory_2`, trait `face`, sens `hearing`, réplique `chat_bubble`), catégorie (labelMedium), texte de l'élément (bodyLarge), état à droite (« auto » ou « main » en labelMedium si coché).
- **Hauteur** : min 56 dp ; 5 lignes = 280 dp en mode déployé.
- **Mode compact** : 5 puces horizontales en flux (icône + case + début du texte, 2 lignes max), même cible 48 dp. Un toucher bascule ; un appui long ou le chevron déploie.
- **États** : *non utilisé*, *auto-coché*, *coché à la main*, *décoché à la main (verrou manuel)*. Le verrou manuel est affiché par une icône cadenas `lock` discrète pour comprendre pourquoi la détection ne recoche pas ; toucher à nouveau le retire.
- **Compteur** : « 3 sur 5 reliés » (jamais « 3/5 manqués »).
- **A11y** : « Réplique : Tu n'aurais pas dû revenir avant la marée. Non utilisé » ; le texte complet de l'élément est lu même si l'affichage est tronqué.

### 5.9 Sélecteur d'auto-évaluation 3 niveaux (`LevelPicker`)
- **Anatomie** : 3 boutons égaux (groupe de choix unique), chacun icône + texte : *Brouillon* (`edit`), *Solide* (`construction`), *Prêt pour la table* (`shield`). Hauteur 72 dp, écart 8 dp. À l'état sélectionné : secondaryContainer + coche + contour primary 2 dp.
- **Comportement** : toucher = choisir ; retoucher = désélectionner ; aucune sélection par défaut ni « pré-coche ». Facultatif.
- **À partir de 150 %** : empilé verticalement, pleine largeur.
- **A11y** : groupe « Auto-évaluation, facultative » ; chaque bouton « Solide, 2 sur 3, non sélectionné ».

### 5.10 Bouton Bijou (`GemToggle`)
- Bouton à bascule : icône `diamond` + texte « Garder comme bijou ». Actif : icône pleine tertiary, fond tertiaryContainer, texte « Bijou ». 48 dp. Pas d'animation, sauf un halo 200 ms si animations autorisées.
- Dans les listes : petite gemme pleine + texte masqué visuellement mais lu (« bijou »).

### 5.11 Compteur de sessions et série avec jour de grâce (`PracticeStats`)
- **Bandeau compact (Accueil)** : « 47 sessions · série 5 jours » en bodyMedium, deux éléments séparés par un point.
- **Carte Progrès** : nombre de sessions en headlineLarge (le plus visible de l'écran), sous-texte « dont 6 cette semaine » ; série en headlineSmall + `local_fire_department` (couleur secondary, pas orange), sous-texte « Jour de grâce disponible » ou « Jour de grâce utilisé cette semaine ».
- **Rangée de 7 jours** (du plus ancien au plus récent, aujourd'hui entouré) : trois symboles distincts par **forme** :
  - ● disque plein = jour pratiqué ;
  - ◐ cercle à contour pointillé avec « g » ou icône `shield_moon` = jour de grâce utilisé ;
  - ○ cercle vide = pas de pratique (aucune teinte d'échec, couleur neutre).
  Chaque jour a une étiquette de lettre (L, M, M, J, V, S, D) et une description « mardi : pratiqué ».
- **Série rompue** : le texte devient « Nouvelle série : 1 jour » + « Meilleure série : 12 jours ». Aucune animation de perte, aucune couleur d'alerte.
- **Rien à zéro** : à 0 session, on affiche seulement « Pas encore de session », la série est masquée.

### 5.12 Bandeau de bascule (`ShiftBanner`, Status Numbers)
- Conteneur tertiaryContainer, icône `swap_vert`, titre « Bascule » + phrase de l'événement. Apparaît en glissant 200 ms (instantané si animations réduites). Une vibration courte, une annonce polite. Reste affiché ; bouton « Compris » facultatif (réduit à une ligne).

### 5.13 État vide et erreur (`EmptyState`, `ErrorCard`)
- **Vide** : icône ou illustration simple, titre court, une phrase d'encouragement, **un** bouton d'action. Jamais de point d'exclamation ni de reproche.
- **Erreur de table** : conteneur errorContainer, icône `error` + titre « Les tirages de cet exercice sont indisponibles », phrase « Rien n'est perdu : tes sessions sont intactes. », actions « Réessayer » (tonal) et « Retour ». Seul endroit où la couleur d'erreur est employée avec la suppression.

### 5.14 Indicateur d'enregistrement (`SaveStatus`)
- Icône `check` + « Enregistré » (labelMedium) en barre du haut. Reste discret : 12 sp, onSurfaceVariant. Texte remplacé par « Enregistrement… » pendant l'écriture (≤ 300 ms ; sans clignoter). Pas d'annonce TalkBack (silencieux).

### 5.15 Carte de session (`SessionCard`, Historique)
- Icône d'exercice, nom, heure/date, aperçu 2 lignes (bodyMedium), badges alignés à droite : gemme (si bijou), niveau (icône + mot), « Inachevée ». Fond surfaceContainerLow, shape.medium, hauteur min 88 dp.
- Aperçu jamais tronqué en lecture TalkBack (description complète en `contentDescription` ou texte fusionné).

### 5.16 Boutons
- Principal : plein primary, stade complet, 56 dp (« Commencer », « Terminé »). Secondaire : tonal. Tertiaire : texte. Icône seule : 48 dp avec libellé accessible. Destructif : texte error, avec annulation par snackbar plutôt que dialogue (sauf « tout supprimer »).

## 6. Mouvement

| Mouvement | Durée | Si animations réduites |
|---|---|---|
| Retournement de carte (relance) | 300 ms | Fondu 100 ms ou changement instantané |
| Transition de page | 250 ms (axe partagé M3) | Fondu 100 ms |
| Bascule chiffrée du minuteur | aucune (pas d'animation sur les chiffres) | — |
| Barre de progression | continue | Pas d'une seconde |
| Entrée en temps additionnel | fondu 200 ms | Instantané |
| Bandeau de bascule | glissement 200 ms | Instantané |
| Halo du bijou | 200 ms | Aucun |

Détection : `Settings.Global.ANIMATOR_DURATION_SCALE == 0` ou réglage in-app « Réduire les animations ». Aucun mouvement ne dure plus de 300 ms ; aucun ne clignote plus de 3 fois par seconde (WCAG 2.3.1).

## 7. Accessibilité

### 7.1 Cibles et tailles
- **Toute cible tactile ≥ 48 × 48 dp**, écart ≥ 8 dp entre cibles voisines. Les éléments visuellement plus petits (pastille 36 dp, case 24 dp, puce 40 dp) reçoivent une zone tactile étendue.
- Texte en **sp** uniquement ; minimum 12 sp ; corps 16 sp, saisie 18 sp.
- **200 % de taille de police** testé sur tous les écrans : aucune hauteur fixe sur le texte, aucune troncature de contenu essentiel, retour à la ligne, passage en colonne des rangées horizontales (cartes d'exercice, sélecteur de niveau, puces).
- **Zone épinglée plafonnée à 35 %** de la fenêtre visible ; au-delà, la consigne se plie (voir 5.4). Le minuteur et « Terminer » restent visibles à 200 % avec clavier en portrait sur écran 360 × 640 dp (cas le plus défavorable).
- Zoom d'affichage (densité) pris en compte de la même façon.

### 7.2 Contraste
- Texte ≥ 4,5:1 ; gros texte et composants ≥ 3:1 (tableau 2.4). Le contour de champ est `outline` (≥ 4,1:1), jamais `outlineVariant`.
- Focus clavier/commutateur : contour 2 dp primary visible sur tous les éléments interactifs.
- Mode sombre vérifié séparément, pas déduit du clair.

### 7.3 Pas de couleur seule
| Information | Autre canal |
|---|---|
| Mot imposé utilisé | Soulignement + case cochée + libellé « utilisé » |
| Élément relié | Coche + libellé ; auto/manuel en texte |
| Champ rempli | Pastille pleine vs contour |
| Minuteur : dernière minute / temps additionnel | Icône, libellé, motif de barre (rayures) |
| Niveau choisi | Coche + contour épais + texte |
| Jour de série | Forme (disque / pointillé / vide) + lettre |
| Bijou | Forme pleine vs contour + texte |
| Erreur | Icône + texte |

### 7.4 TalkBack et technologies d'assistance
- Chaque composant a un libellé et un rôle (case à cocher, bouton radio, bouton, en-tête). Les titres d'écran sont des en-têtes (navigation par titres).
- **Minuteur** : élément unique, description à la demande. **Pas de `liveRegion` seconde par seconde.** Jalons uniquement : début, mi-parcours (si durée ≥ 6 min), dernière minute (si durée ≥ 3 min), fin (« Temps écoulé. Tu peux continuer à écrire. »). Annonces polite, jamais assertive.
- **Champs** : étiquette « Idée 3 sur 8 » ; le focus reste dans le champ, TalkBack ne lit pas le surlignage.
- **Ordre de focus** : barre du haut, minuteur, consigne, champs, bouton. Le bouton « Terminer » est dans la barre du haut : accessible en 2-3 balayages.
- Après relance, fermeture de feuille ou changement de question : focus sur le nouvel élément et annonce unique.
- Textes alternatifs pour graphiques de Progrès : liste équivalente (« Crazy Eights : 12 sessions »).
- Commutateur et clavier physique : tous les éléments atteignables, Tab/Maj-Tab, Entrée = activer, Échap = retour.
- **Dictée** : le micro du clavier est laissé accessible ; aucun blocage des services d'assistance sur les champs.

### 7.5 Temps (WCAG 2.2.1) et mouvement
- Durée réglable et option « Sans minuteur » par exercice (D17). Après la fin, la saisie reste ouverte (temps additionnel) : l'utilisateur dispose d'un temps illimité pour terminer.
- Pas de contenu clignotant. Animations réduites suivies (§6).
- Vibration et bip réglables ; le bip ne sonne pas en mode silencieux/vibreur.

### 7.6 Clavier et fenêtre
- `windowSoftInputMode`/insets : la zone défilante se redimensionne avec le clavier ; la zone épinglée et la barre du haut ne bougent pas.
- Désactiver le mode plein écran du clavier en paysage (`flagNoExtractUi`).
- Ne jamais réinitialiser le texte à la rotation ou à la mise en arrière-plan.

### 7.7 Checklist de revue (à utiliser par le testeur)
1. 200 % de police, portrait 360 × 640 dp, clavier ouvert : minuteur, consigne (pliable) et « Terminer » visibles.
2. TalkBack : parcourir S04 → S05 → S06 sans piège ; annonces uniquement aux jalons.
3. Niveaux de gris : chaque état se distingue.
4. Mode sombre : mêmes tests.
5. Animations désactivées : aucun mouvement résiduel.
6. Rotation à chaque étape : état conservé.

## 8. Notes pour le maquettiste

- Variables CSS = rôles M3 ci-dessus (`--md-sys-color-primary`, …). Maquettes clair **et** sombre via une classe `.dark` sur le conteneur.
- Dimensions : cadre 360 × 800 dp (Pixel moyen) ; pour les écrans « En cours », dessiner un clavier de 280 dp (bloc gris `surfaceContainerHigh` étiqueté « Clavier ») et laisser 520 dp de fenêtre visible.
- Polices : Alegreya et Atkinson Hyperlegible via CSS (Google Fonts en maquette, embarquement dans l'app plus tard). Repli `Georgia` / `system-ui`.
- Un seul vrai lien interactif : la bascule clair/sombre et la bascule d'état du minuteur. Le reste peut être statique.
