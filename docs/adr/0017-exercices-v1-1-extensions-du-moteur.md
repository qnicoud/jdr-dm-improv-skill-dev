# ADR 0017 — Exercices v1.1 : extensions génériques du moteur et nouvelles tables

- Statut : proposée (2026-10-18)
- Amende : ADR 0012 (points d'extension), ADR 0009 (4 nouvelles tables, minimum d'entrées propre à une table de vocabulaire), ADR 0011 (séance du jour figée par jour)
- Décision de pilotage : D23 (Échec qui avance, Règle des 3 indices, Oui et…)
- Entrées : `docs/research/02-pedagogie-exercices.md` §3 (A, B, C), `docs/ux/parcours-v1.1.md` §1 (V11-3, V11-4, V11-5, V11-13)

## Contexte

L'ADR 0012 promet qu'un exercice « tirage + N champs + minuteur » se branche avec une définition, une table, des chaînes et un test, sans nouvel écran. Les trois exercices de la v1.1 entrent presque dans ce cadre, mais l'UX demande quelques comportements que le moteur n'a pas encore :

| Besoin UX | Exercice | Couvert par le moteur actuel ? |
|---|---|---|
| Situation tirée + 3 champs | Échec qui avance, 3 indices | oui (`TableSlot`, `FieldLayout.Fixed`) |
| **Chaque champ lié au i-ème tirage d'un slot** (« Complication 1 · Coût », icône et indication propres au type) | Échec qui avance | **non** : les libellés de `FieldSpec` sont fixes |
| Choisir sa préférée dans le bilan | Échec qui avance | oui (`endChoice`, `FirstPick`), mais le libellé « Jouée en premier » est **en dur** dans l'export (dette LOW de R1a) et propre à Decision Point |
| Une offre à la fois, Précédente / Suivante | Oui, et… | oui (`FieldLayout.Sequential`) |
| **Deux champs par offre** (réflexe de blocage, puis « Oui, et… ») selon une option | Oui, et… | **non** : `Sequential` n'a qu'un champ |
| **Offres non révélées sur l'écran Prêt**, pas de relance avant de commencer, offre en cours épinglée | Oui, et… | **non** : `ReadyQuestions` affiche tout et la zone épinglée exclut les questions |
| **Préfixe visuel non modifiable** « Oui, et » devant le champ | Oui, et… | **non** |
| **3e question de grille seulement si une option est active** | Oui, et… | **non** : la grille est fixe (2 ou 3 questions) |
| Vibration de rythme toutes les 60 s, avance manuelle | Oui, et… | oui (`ScheduledEvent(Periodic(60 s, haptic))`, réglage existant « vibration chaque minute ») |
| Étiquettes fixes Objet / PNJ / Lieu avec icônes | 3 indices | oui (`FieldSpec.label`) ; l'icône par champ est à ajouter |

## Options

1. Écrire un écran propre à « Oui, et… » : écartée, contraire à l'ADR 0012.
2. Ajouter **quelques champs facultatifs** aux types existants, avec des valeurs par défaut neutres. Les 7 exercices actuels restent inchangés sans retoucher leurs définitions.
3. Créer de nouveaux types scellés (`FieldLayout.Paired`, `FieldLayout.Offers`) : plus de code d'interface à maintenir, pour un gain nul par rapport à l'option 2.

Pour les types de complication :

- (a) **Table `complication_types.json`** de 6 à 8 entrées (le texte est le nom du type). Elle se traduit avec le contenu (ADR 0016), et `id` est un code stable qui sert aussi à choisir l'icône et l'indication.
- (b) Une liste de codes dans le code avec des libellés `strings.xml` et un nouveau `CodeSlot` : il faudrait un nouveau type de slot, et le `textSnapshot` devrait être résolu hors du tireur pur.
- (c) Une table de 30 complications concrètes : elle ferait le travail de l'utilisateur à sa place. Écartée.

## Décision

Option 2 et table (a). Les ajouts au modèle (`core/exercise`) sont tous **facultatifs et rétrocompatibles**.

### 1. Ajouts génériques

```kotlin
// FieldSpec
val icon: IconKey? = null            // icône devant le libellé (3 indices : objet / pnj / lieu)
val prefix: StringResId? = null      // préfixe visuel non saisi (« Oui, et »)

// FieldLayout.Fixed
val boundSlot: String? = null        // le champ i est lié au tirage i de ce slot : libellé « {label} {i} · {tirage} »,
                                     // icône et indication choisies d'après le code (id) de l'entrée tirée
val hintByCode: Map<String, StringResId> = emptyMap()   // indications par code (« Ça coûte quoi ? »)

// FieldLayout.Sequential
val fields: List<FieldSpec>          // remplace `field` (un ou plusieurs champs par question ; visibleWhen par option)
val itemLabel: StringResId? = null   // « Offre %1$d sur %2$d » au lieu de « Question %1$d sur %2$d »
val revealProgressively: Boolean = false  // Prêt : description au lieu de la liste, pas de relance ;
                                          // pendant le jeu, l'élément courant est épinglé, et « tout voir » n'est pas proposé

// RubricQuestion
val visibleWhen: OptionCondition? = null  // question posée seulement si l'option est remplie

// ExerciseDefinition
val endChoiceLabel: StringResId? = null   // libellé du choix dans le détail et l'export (« Jouée en premier », « Ma préférée »)
```

Règles :

- `IconKey` est un code texte (`"object"`, `"npc"`, `"place"`, `"cost"`…). L'interface le traduit en icône avec la même table de correspondance que les catégories de Connect the Story (`CategoryIcon`). `core` reste sans dépendance à Compose.
- Avec `Fixed.boundSlot`, le nombre de champs doit être égal au `count` du slot, ce que vérifie `init`. Les réponses restent adressées par `fieldKey` (`complication_1`…), ce qui ne demande aucun changement de stockage. Le lien avec le type se relit par la position du tirage.
- `Sequential.fields` : les réponses sont adressées par `AnswerKey(fieldKey, groupIndex = indice de la question)`, déjà le cas pour Core Image. Un `field` unique devient `fields = listOf(field)`, ce qui ne change pas les données de Core Image. Les champs dont `visibleWhen` n'est pas rempli ne sont ni affichés ni comptés.
- `revealProgressively` : les tirages ont toujours lieu sur l'écran Prêt et sont enregistrés au démarrage, comme d'habitude (aucune différence pour la reprise ou l'historique). Seul l'affichage change. Le bouton de relance est masqué en phase Prêt pour ce slot.
- Grille : le nombre de questions **visibles** pour les options de la session reste de 2 à 3. `init` vérifie chaque combinaison d'options. Les réponses d'une question masquée ne sont ni affichées ni exportées.
- `endChoiceLabel` règle la dette « libellé d'export en dur ». La clé stockée `first_pick:<fieldKey>` **ne change pas**, puisque des données réelles existent. Le nom Kotlin `FirstPick` peut devenir `PickedAnswer` dans le lot de dette, sans toucher à la clé.
- Le rythme de 60 s de « Oui, et… » est **global**, compté depuis le démarrage du minuteur, et non relancé à chaque changement d'offre. Ce choix réutilise `TimedEvent.Periodic` sans nouvel état. Le repère visuel de l'UX (segment de 60 s) montre l'avancée de la minute en cours. Un rythme par offre demanderait de mémoriser l'instant d'affichage de chaque offre dans `ExerciseSession`. Ce sera un ajout, à faire seulement si l'UX y tient après essai.
- Le réglage « vibration chaque minute » (`crazy_eights_minute_haptic`, déjà lu pour tout exercice qui a un `Periodic` haptique) est **renommé dans l'interface seulement** (« Vibration de rythme : Crazy Eights, Oui et… »). La clé DataStore ne change pas.

### 2. Les trois définitions

| | Échec qui avance (`failing_forward`) | Règle des 3 indices (`three_clues`) | Oui, et… (`yes_and`) |
|---|---|---|---|
| Minuteur | `Required(4 min)` | `Required(5 min)` | `Required(5 min)` |
| Slots | `situation` : `failure_situations` ×1 ; `types` : `complication_types` ×3, `distinctBy = Id` | `conclusion` : `clue_conclusions` ×1 | `offers` : `player_offers` ×5, `categoryCycle` sur les 4 catégories |
| Saisie | `Fixed` 3 champs `complication_1..3`, `boundSlot = "types"`, `hintByCode` (6 à 8 codes) | `Fixed` 3 champs `clue_object`, `clue_npc`, `clue_place`, libellés et icônes fixes | `Sequential(offers)`, `fields = [block (visibleWhen block_first, 1 ligne, softLimit 80), yes_and (prefix « Oui, et »)]`, `itemLabel`, `revealProgressively = true` |
| Progression | `FilledFields` (x/3) | `FilledFields` (x/3) | « Offre i sur 5 » (séquentiel) |
| Options | — | — | `block_first` (défaut `false`) |
| Événements | — | — | `Periodic(60 s, haptic)` |
| Bilan | `endChoice = true`, `endPrompt` « Laquelle ferait le plus avancer l'histoire ? », `endChoiceLabel` « Ma préférée » | — | — |
| Grille | 2 questions (UX §1.2) | 2 questions (UX §1.3) | 2 questions + 1 si `block_first` (UX §1.4) |
| Échauffement (séance du jour) | oui | non | oui |
| Sujet libre si la table est illisible | oui | oui | non (5 offres nécessaires) |

Ajouts au catalogue (`ExerciseId`) :

- `ALL` passe à 10 entrées (ordre UX : les 7 existants, puis les 3 nouveaux) ;
- `DEFAULT_DURATIONS` reçoit 4, 5 et 5 min ;
- `DAILY_WARM_UPS` reçoit `failing_forward` et `yes_and`.

Ces trois listes doublent aujourd'hui des informations de la définition (`warmUp`, `timer`). Le lot V1 les **dérive du registre** (`ExerciseRegistry.Default`) pour qu'un futur exercice ne s'ajoute qu'à un seul endroit. Un test vérifie que `ALL`, `DEFAULT_DURATIONS` et `DAILY_WARM_UPS` sont cohérents avec le registre.

### 3. Nouvelles tables (complément à l'ADR 0009)

Même enveloppe (`schemaVersion` 1), déposées dans `assets/tables/fr/` (ADR 0016).

| Fichier | Entrées | `category` | `keywords` | `tags` | Remarques |
|---|---|---|---|---|---|
| `failure_situations.json` | ≥ 30 | facultative : `combat`, `social`, `exploration`, `stealth`, `knowledge` | — | — | une phrase : qui rate quoi (« Le voleur rate son jet pour crocheter la porte du temple. ») |
| `complication_types.json` | **6 à 8** (exception au minimum de 30) | — | — | — | `id` = code du type, dans un vocabulaire fermé : `cost`, `time`, `information`, `relation`, `position`, `danger` (+ `resource`, `ally` si le rédacteur les juge utiles) ; `text` = nom du type (« Coût »), ≤ 20 caractères ; `difficulty` libre, sans exigence de couvrir les 3 niveaux |
| `clue_conclusions.json` | ≥ 30 | facultative : `identity`, `location`, `motive`, `method`, `secret` | — | — | une conclusion d'enquête énoncée comme un fait (« Le bourgmestre est un imposteur. ») |
| `player_offers.json` | ≥ 40, **≥ 10 par catégorie** | **obligatoire** : `social`, `action`, `exploration`, `unexpected` | — | — | une réplique de joueur à la 1re personne (« Je veux séduire le dragon. »), ≤ 120 caractères ; ni piège ni offre irrecevable |

- `TableId` reçoit `FailureSituations`, `ComplicationTypes`, `ClueConclusions` et `PlayerOffers`.
- `TablesContentTest` passe de 10 à 14 fichiers. Il permet un **minimum propre à une table de vocabulaire** (`complication_types` : de 6 à 8 entrées, ids dans le vocabulaire fermé, pas d'exigence de couverture des difficultés). Chaque code de `complication_types` doit avoir son indication (`hintByCode`) et son icône : ce test est écrit côté définition.
- Aucune de ces tables ne porte de `keywords` : aucun de ces exercices n'utilise la détection.

### 4. Séance du jour figée par jour (UX V11-13, amende l'ADR 0011)

Le plan du jour est aujourd'hui recalculé à chaque affichage à partir de la date. Une mise à jour qui agrandit le catalogue changerait donc la séance déjà proposée ce jour-là. La correction : le premier plan calculé dans la journée est **enregistré** dans DataStore (`daily_plan` = `"<epochDay>:<id>,<id>,<id>"`) et réutilisé jusqu'au lendemain. Les remplacements (`daily_replacements`) s'appliquent par-dessus, sans changement. Un identifiant inconnu (exercice retiré) invalide le plan enregistré, qui est alors recalculé.

## Conséquences

- Un exercice de la v1.1 tient en une définition (environ 60 lignes), une table, des chaînes, une icône et un test de définition. Il n'y a **aucune migration Room**, puisque les réponses restent adressées par `fieldKey` et `groupIndex`.
- `FieldList`, `QuestionList`, `EndPanel`, `ReadyBody` et la zone épinglée reçoivent les rendus correspondants. Le découpage d'`ExerciseViewModel` (lot V0) doit être fait **avant**, pour éviter des conflits.
- Tests :
  - réducteur et définitions : `boundSlot` (libellés liés aux tirages, relance des types), `Sequential.fields` (deux champs par question, champ masqué sans l'option), grille conditionnelle (2 ou 3 questions), cohérence registre ↔ catalogue ;
  - Compose : préfixe visible et absent du texte saisi, offres non révélées sur l'écran Prêt, offre courante épinglée, « Ma préférée » ;
  - `AllExercisesSmokeTest` étendu aux 10 exercices ;
  - `DailySessionPlanner` avec 10 exercices et plan figé ;
  - export lisible : libellé du choix tiré de la définition.

## Complément : étiquette « Nouveau » (V2b)

L'étiquette « Nouveau » de la carte d'accueil est portée par `ExerciseId.NEW_IN_LATEST_RELEASE`. Elle disparaît après la **première session comptée** de l'exercice, c'est-à-dire terminée, ou inachevée avec du contenu : la règle du compteur de sessions (ADR 0011, `IS_COUNTED`). Une session inachevée sans contenu, ou encore en cours, la laisse en place. Rien n'est stocké : `HistoryRepository.observeCountedExerciseIds(ids)` (requête `SELECT DISTINCT`, émission sur changement seulement) la déduit de l'historique.

## Décisions de l'utilisateur (2026-10-18)

- D56 : types de complication tirés et imposés depuis une table de 6 à 8 types. D57 : offres de « Oui, et… » révélées une à une. D58 : repère par minute, passage manuel à l'offre suivante. D69 : la séance du jour déjà proposée le jour de la mise à jour reste figée jusqu'au lendemain.
