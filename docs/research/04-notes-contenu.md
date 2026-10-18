# Notes de contenu — tables aléatoires v1

Fichiers : `app/src/main/assets/tables/*.json` (10 fichiers, format de l'ADR 0009, `schemaVersion` 1).

## Volumes et difficulté

| Table | Entrées | D1 / D2 / D3 |
|---|---|---|
| problems | 38 | 12 / 14 / 12 |
| places | 35 | 12 / 14 / 9 |
| sensory_words | 43 (sight 10, hearing 8, smell 8, taste 8, touch 9) | 16 / 16 / 11 |
| core_images | 37 | 12 / 14 / 11 |
| threading_questions | 33 (what 8, origin 8, who 8, link 9) | 10 / 13 / 10 |
| npc_roles | 34 | 10 / 12 / 12 |
| status_shifts | 34 | 10 / 13 / 11 |
| surprise_situations | 34 | 10 / 13 / 11 |
| story_elements | 158 (≥ 31 par catégorie) | 51 / 58 / 49 |
| one_page_situations | 31 | 8 / 11 / 12 |

Critères de difficulté : D1 = situation simple, un seul enjeu, un cliché assumé ; D2 = un dilemme ou un détail inattendu ; D3 = enjeu moral, paradoxe, élément abstrait ou qui oblige à improviser une règle du monde. Pour `sensory_words` : D1 = mots courants, D3 = mots rares ou peu associés au lieu. Pour `threading_questions`, la difficulté monte avec l'abstraction (la catégorie `link` est plus difficile).

## Choix éditoriaux

- Fantasy générique, aucun nom propre, aucun emprunt à un univers publié. Pas de genre multiple : le schéma n'a pas de champ `genre`.
- Contenus évités : violence sexuelle, discriminations réelles, cruauté gratuite envers les enfants ou les animaux, mentions de religions réelles. Les tons tragique, comique et mystérieux sont mélangés.
- `npc_roles` : environ moitié `rank:high` et moitié `rank:low`. Plusieurs rôles sont déjà à contre-emploi (un tyran humble, un enfant qui commande une bande). Un MJ peut donc tirer le contre-stéréotype en inversant le tag.
- `status_shifts` : événements de scène (une arrivée, une révélation, un incident), jamais un état permanent. 17 `shift:up` et 17 `shift:down`.
- `threading_questions` : génériques, sans référence à une image précise. Les questions `link` s'appuient sur « un autre élément de ton monde ».
- `surprise_situations` : toutes formulées comme une action de joueur qui sort du scénario prévu.
- `one_page_situations` : un contexte et au moins deux camps ou personnes en tension ; assez de matière pour 5 PNJ au plus.
- `places` : adapté à Constraint Box. Aucun `keywords` fourni (facultatif).

## Cas limites des `keywords`

- Un mot-clé est une forme que le MJ écrirait naturellement : singulier, pluriel, féminin, et un synonyme proche. Il n'y a pas de racinisation (`IdentityStemmer`).
- Les doublons après normalisation sont retirés (par exemple « rouille » et « rouillé » donnent la même forme normalisée, donc un seul est gardé). Le test JVM les refuserait.
- Les mots très courants sont évités (« pas », « terre », « jamais » seul). Pour les répliques (`line`), on utilise des suites de jetons (« rien vu », « vous attendait ») pour la précision, au prix d'un rappel plus bas ; les répliques sont l'élément le plus risqué pour le corpus doré.
- Faux positifs connus et acceptés : « retard » dans une phrase sans rapport, « froid » et « feu » (mots courants), « clé » (au sens figuré), les négations (« pas de lanterne »). Le cochage manuel corrige.
- Le champ `fuzzy` n'est jamais activé : la tolérance aux fautes est inutile tant que le corpus doré n'a pas mesuré de problème.
- Une même idée sensorielle existe dans `sensory_words` (un mot) et `story_elements` (une phrase) ; ce sont deux tables indépendantes.
- Plusieurs mots-clés de `sensory_words` partagent une racine d'une entrée à l'autre (« fumé » dans taste, « feu de bois » dans smell) : ils sont volontairement distincts.

## Vérification

Script de contrôle (hors dépôt) : JSON valide, `table`/`schemaVersion`/`locale`, identifiants kebab-case uniques, `difficulty` ∈ 1..3 avec les trois niveaux présents, texte ≤ 200 caractères en NFC, pas de doublon normalisé, `category` et `tags` dans le vocabulaire, 1 à 4 `keywords` sans doublon là où ils sont obligatoires.

## Tables v1.1 (lot C1)

- `failure_situations` (36) : « qui rate quoi », une phrase, sans conséquence imposée. Catégories `combat`, `social`, `exploration`, `stealth`, `knowledge` (7 à 8 chacune). D1 = échec banal, D3 = échec devant témoins ou à enjeu public.
- `complication_types` (6) : `cost`, `time`, `information`, `relation`, `position`, `danger`. Définitions de travail (le fichier ne porte que le nom) :
  - Coût : il faut payer, perdre ou sacrifier quelque chose (« la serrure cède, mais le crochet casse »).
  - Temps : le délai se réduit ou une échéance apparaît (« la porte s'ouvre, mais les gardes reviennent dans une minute »).
  - Information : on apprend, ou l'on perd de vue, un fait important (« on entend la conversation, mais on est repéré »).
  - Relation : un PNJ, un allié ou une faction en est affecté (« le portier cède, mais il vous en voudra »).
  - Position : on se retrouve ailleurs ou dans une situation plus exposée (« on franchit la porte, côté ennemi »).
  - Danger : une menace nouvelle surgit (« la porte s'ouvre sur une pièce où quelque chose s'éveille »).
  - `resource` et `ally` n'ont pas été ajoutés : ils recoupent Coût et Relation.
- `clue_conclusions` (34) : un fait énoncé à l'indicatif, découvrable par un objet, un PNJ ou un lieu. Catégories `identity`, `location`, `motive`, `method`, `secret`.
- `player_offers` (46) : répliques à la 1re personne, ≤ 120 caractères, recevables (aucun piège). 11 à 12 par catégorie `social`, `action`, `exploration`, `unexpected`.
- Aucun `keywords`. Tables à déposer dans `tables/` (ou `tables/fr/` si V5 est fusionné).
