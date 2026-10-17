# ADR 0012 — Moteur d'exercice générique piloté par des définitions

- Statut : acceptée (2026-10-16)
- Lien : SPEC « Les 7 exercices », feuille de route v1.1 (D23 : Échec qui avance, 3 indices, Oui et…)

## Contexte

Les 7 exercices de la v1 et les 3 de la v1.1 partagent un même gabarit : consigne, tirage (relançable), minuteur, champs de saisie, cases, auto-évaluation, sauvegarde automatique. Écrire un écran par exercice dupliquerait le minuteur, la sauvegarde et l'accessibilité.

## Options

1. Un écran et un ViewModel par exercice.
2. **Un écran et un ViewModel génériques, alimentés par une `ExerciseDefinition` déclarative**, avec quelques points d'extension typés.
3. Des définitions en JSON dans les assets : trop dynamique, sans typage ni ressources `strings.xml`.

## Décision

Option 2. Une `ExerciseDefinition` est une valeur Kotlin immuable, déclarée dans `feature/exercise/<nom>/<Nom>Definition.kt`, et enregistrée dans `ExerciseRegistry`. Elle décrit :

- **identité** : `id` stable (`crazy_eights`…), libellés `@StringRes` (nom, discipline, consigne, rappel) ;
- **minuteur** : `TimerPolicy.Required(default)` | `Optional(suggested)` | `None` ;
- **tirage** : liste de `DrawSlot` :
  - `TableSlot(key, table, category?, filter)` (par ex. 3 mots sensoriels de sens distincts : `distinctBy = Category`) ;
  - `CardSlot(key, range = 1..10)` (Status Numbers) ;
  - `ChoiceSlot` (choix préalable, comme le domaine de Constraint Box ; en v1 un seul domaine « lieu », donc choix masqué) ;
- **saisie** : `FieldLayout` :
  - `Fixed(fields)` (Crazy Eights : 8 champs numérotés ; Decision Point : 3 champs avec drapeau « jamais d'habitude ») ;
  - `Single(field)` (Constraint Box, Connect the Story) ;
  - `Sequential(questionsSlot)` (Core Image : une question à la fois) ;
  - `RepeatableGroup(fields, max)` (One-Page : PNJ, motivation, liens ; 5 groupes au plus) ;
- **détection** : `DetectionSpec(slotKeys, field)` facultative (ADR 0008) ;
- **progression** : `ProgressRule` (par ex. « x/8 champs non vides ») ;
- **événements programmés** : `TimedEvent` (rappel périodique, bascule à un instant aléatoire entre 40 et 60 %, qui tire dans `status_shifts`) ;
- **options** : bascules propres à l'exercice (contre-stéréotype, bascule, mode vidéo ou table), libellés `@StringRes`, valeurs stockées avec la session ;
- **auto-évaluation** : 2 à 3 `RubricQuestion` (oui/non) propres à l'exercice, en plus des 3 niveaux et du bijou communs.

Le **cœur du moteur est pur** (`core/exercise`) : `ExerciseSession` (état immuable) et `ExerciseReducer` (`reduce(state, action): state`). Le ViewModel générique `ExerciseViewModel` fait le lien avec le minuteur, la détection, le repository et le `SavedStateHandle`. L'écran générique `ExerciseScreen` rend les slots et les champs. Un exercice peut fournir une **en-tête Compose spécifique** (par exemple la carte de statut) avec `headerContent`. C'est le seul code d'interface propre à un exercice.

## Conséquences

- Un nouvel exercice « tirage + N champs + minuteur » demande une définition (environ 50 lignes), une table JSON, des chaînes et un test de définition. Pas de nouvel écran ni de migration Room, puisque les réponses sont stockées par `fieldKey` (voir `docs/architecture.md` §4).
- Les points d'extension sont fermés (types scellés). Un besoin nouveau, comme un schéma de relations en v2, demandera un nouveau type de `FieldLayout` et un ADR.
- Le réducteur concentre la logique : il est testé exhaustivement (tirage, relance, saisie, cases, fin, reprise).

## Complément (2026-10-18, v1.1)

Ajouts facultatifs et rétrocompatibles pour les exercices de la v1.1 (ADR 0017) :
- `FieldSpec.icon` et `FieldSpec.prefix` ;
- `Fixed.boundSlot` et `Fixed.hintByCode` ;
- `Sequential.fields`, `Sequential.itemLabel` et `Sequential.revealProgressively` ;
- `RubricQuestion.visibleWhen` ;
- `ExerciseDefinition.endChoiceLabel`.

Le catalogue (`ExerciseId.ALL`, durées par défaut, échauffements) est dérivé du registre.
